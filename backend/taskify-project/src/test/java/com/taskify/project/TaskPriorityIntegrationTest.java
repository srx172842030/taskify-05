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
class TaskPriorityIntegrationTest {

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
    void createWithPriorityRoundTripsAndRejectsInvalidValue() {
        HttpHeaders headers = userHeaders("pm_user");

        // 带优先级创建，往返一致
        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "高优先级任务", "priority", "HIGH"), headers), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().get("data").get("priority").asText()).isEqualTo("HIGH");

        // 缺省（null）保持未设置
        ResponseEntity<JsonNode> unset = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "无优先级任务"), headers), JsonNode.class);
        assertThat(unset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unset.getBody().get("data").get("priority").isNull()).isTrue();

        // 非法值被拒
        ResponseEntity<JsonNode> bad = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "非法", "priority", "URGENT"), headers), JsonNode.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");
    }

    @Test
    void updateCanSetAndClearPriority() {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "任务", "priority", "LOW"), headers), JsonNode.class);
        String taskId = created.getBody().get("data").get("id").asText();

        // 修改优先级
        ResponseEntity<JsonNode> updated = rest.exchange("/api/v1/tasks/" + taskId, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("title", "任务", "priority", "MEDIUM"), headers), JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("data").get("priority").asText()).isEqualTo("MEDIUM");

        // 置 null 清空（Map.of 不允许 null 值，改用 HashMap）
        java.util.Map<String, Object> clearBody = new java.util.HashMap<>();
        clearBody.put("title", "任务");
        clearBody.put("priority", null);
        ResponseEntity<JsonNode> cleared = rest.exchange("/api/v1/tasks/" + taskId, HttpMethod.PATCH,
                new HttpEntity<>(clearBody, headers), JsonNode.class);
        assertThat(cleared.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cleared.getBody().get("data").get("priority").isNull()).isTrue();
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
