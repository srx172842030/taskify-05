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

import java.util.List;
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
class TaskBatchUpdateIntegrationTest {

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

    private String createTask(String title) {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> r = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title), headers), JsonNode.class);
        return r.getBody().get("data").get("id").asText();
    }

    private void move(String taskId, String toStatus) {
        HttpHeaders headers = userHeaders("pm_user");
        rest.exchange("/api/v1/tasks/" + taskId + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", toStatus), headers), JsonNode.class);
    }

    @Test
    void batchUpdateAssigneesAndPriority() {
        HttpHeaders headers = userHeaders("pm_user");
        String a = createTask("任务A");
        String b = createTask("任务B");

        ResponseEntity<JsonNode> r = rest.exchange(
                "/api/v1/projects/" + projectId + "/tasks/batch-update", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "taskIds", List.of(a, b),
                        "patch", Map.of("assigneeId", "eng_user", "priority", "HIGH")), headers), JsonNode.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode items = r.getBody().get("data");
        assertThat(items.size()).isEqualTo(2);
        for (JsonNode item : items) {
            assertThat(item.get("assigneeId").asText()).isEqualTo("eng_user");
            assertThat(item.get("priority").asText()).isEqualTo("HIGH");
        }
    }

    @Test
    void batchStatusCrossColumnRejectedAtomically() {
        HttpHeaders headers = userHeaders("pm_user");
        String inProgress = createTask("进行中任务");
        move(inProgress, "IN_PROGRESS");
        String todo = createTask("待办任务");

        // 批量移动到 DONE：todo（TODO→DONE 跨列）非法，整批拒绝
        ResponseEntity<JsonNode> r = rest.exchange(
                "/api/v1/projects/" + projectId + "/tasks/batch-update", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "taskIds", List.of(inProgress, todo),
                        "patch", Map.of("status", "DONE")), headers), JsonNode.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");

        // 无部分生效：进行中任务仍为 IN_PROGRESS
        ResponseEntity<JsonNode> check = rest.exchange("/api/v1/tasks/" + inProgress, HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(check.getBody().get("data").get("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void batchUpdateEmptyTaskIdsRejected() {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> r = rest.exchange(
                "/api/v1/projects/" + projectId + "/tasks/batch-update", HttpMethod.POST,
                new HttpEntity<>(Map.of("taskIds", List.of(), "patch", Map.of("priority", "HIGH")), headers),
                JsonNode.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
