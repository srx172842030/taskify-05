package com.taskify.project;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "taskify.rocketmq.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
        })
@Testcontainers
class TaskDependencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taskify")
            .withUsername("taskify")
            .withPassword("taskify");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    private String projectId;

    @BeforeEach
    void createProject() {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> p = rest.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "项目"), headers), JsonNode.class);
        projectId = p.getBody().get("data").get("id").asText();
    }

    @Test
    void hardBlockPreventsStartUntilBlockerDone() {
        HttpHeaders headers = userHeaders("pm_user");
        String a = createTask("A", headers);
        String b = createTask("B", headers);

        // A 阻塞 B
        ResponseEntity<JsonNode> dep = rest.exchange("/api/v1/tasks/" + b + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(Map.of("blockerTaskId", a), headers), JsonNode.class);
        assertThat(dep.getStatusCode()).isEqualTo(HttpStatus.OK);

        // B 被标记阻塞，移动到进行中被拒（TASK_BLOCKED）
        ResponseEntity<JsonNode> moveB = rest.exchange("/api/v1/tasks/" + b + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", "IN_PROGRESS"), headers), JsonNode.class);
        assertThat(moveB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(moveB.getBody().get("code").asText()).isEqualTo("TASK_BLOCKED");

        // A 完成
        moveTo(headers, a, "IN_PROGRESS");
        moveTo(headers, a, "IN_REVIEW");
        moveTo(headers, a, "DONE");

        // B 解除阻塞后可开始
        ResponseEntity<JsonNode> moveBOk = rest.exchange("/api/v1/tasks/" + b + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", "IN_PROGRESS"), headers), JsonNode.class);
        assertThat(moveBOk.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveBOk.getBody().get("data").get("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void blockedTaskCannotAdvanceButCanRevert() {
        HttpHeaders headers = userHeaders("pm_user");
        String a = createTask("A", headers);
        String b = createTask("B", headers);

        // B 先进入「进行中」
        moveTo(headers, b, "IN_PROGRESS");
        // 建立 A 阻塞 B（A 未完成）
        rest.exchange("/api/v1/tasks/" + b + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(Map.of("blockerTaskId", a), headers), JsonNode.class);

        // 被阻塞的 B 向「审查中」推进应被拒（TASK_BLOCKED）
        ResponseEntity<JsonNode> adv = rest.exchange("/api/v1/tasks/" + b + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", "IN_REVIEW"), headers), JsonNode.class);
        assertThat(adv.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(adv.getBody().get("code").asText()).isEqualTo("TASK_BLOCKED");

        // 回退到「待办事项」应被允许
        ResponseEntity<JsonNode> revert = rest.exchange("/api/v1/tasks/" + b + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", "TODO"), headers), JsonNode.class);
        assertThat(revert.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(revert.getBody().get("data").get("status").asText()).isEqualTo("TODO");
    }

    @Test
    void rejectSelfBlockCycleAndCrossProject() {
        HttpHeaders headers = userHeaders("pm_user");
        String a = createTask("A", headers);
        String b = createTask("B", headers);

        // 自阻塞
        ResponseEntity<JsonNode> self = rest.exchange("/api/v1/tasks/" + a + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(Map.of("blockerTaskId", a), headers), JsonNode.class);
        assertThat(self.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(self.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");

        // 建立 A 阻塞 B，再尝试 B 阻塞 A（环）
        rest.exchange("/api/v1/tasks/" + b + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(Map.of("blockerTaskId", a), headers), JsonNode.class);
        ResponseEntity<JsonNode> cycle = rest.exchange("/api/v1/tasks/" + a + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(Map.of("blockerTaskId", b), headers), JsonNode.class);
        assertThat(cycle.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cycle.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");

        // 跨项目
        ResponseEntity<JsonNode> p2 = rest.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "项目2"), headers), JsonNode.class);
        String project2Id = p2.getBody().get("data").get("id").asText();
        ResponseEntity<JsonNode> c = rest.exchange("/api/v1/projects/" + project2Id + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "C"), headers), JsonNode.class);
        String cTask = c.getBody().get("data").get("id").asText();
        ResponseEntity<JsonNode> cross = rest.exchange("/api/v1/tasks/" + cTask + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(Map.of("blockerTaskId", a), headers), JsonNode.class);
        assertThat(cross.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cross.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");
    }

    private String createTask(String title, HttpHeaders headers) {
        ResponseEntity<JsonNode> res = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title), headers), JsonNode.class);
        return res.getBody().get("data").get("id").asText();
    }

    private void moveTo(HttpHeaders headers, String taskId, String status) {
        ResponseEntity<JsonNode> res = rest.exchange("/api/v1/tasks/" + taskId + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", status), headers), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
