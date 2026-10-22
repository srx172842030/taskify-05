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
class TaskIntegrationTest {

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
    void createTaskDefaultsToTodoAndRejectBlankTitle() {
        HttpHeaders headers = userHeaders("pm_user");

        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "实现登录页"), headers), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().get("data").get("status").asText()).isEqualTo("TODO");

        ResponseEntity<JsonNode> blank = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "  "), headers), JsonNode.class);
        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blank.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");
    }

    @Test
    void moveOnlyAllowsAdjacentColumns() {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "任务"), headers), JsonNode.class);
        String taskId = created.getBody().get("data").get("id").asText();

        // 跨列（TODO → DONE）被拒
        ResponseEntity<JsonNode> cross = rest.exchange("/api/v1/tasks/" + taskId + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", "DONE"), headers), JsonNode.class);
        assertThat(cross.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cross.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");

        // 相邻列（TODO → IN_PROGRESS）成功
        ResponseEntity<JsonNode> ok = rest.exchange("/api/v1/tasks/" + taskId + "/move", HttpMethod.POST,
                new HttpEntity<>(Map.of("toStatus", "IN_PROGRESS"), headers), JsonNode.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().get("data").get("status").asText()).isEqualTo("IN_PROGRESS");
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
