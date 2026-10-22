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

import java.util.HashMap;
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
class ReminderIntegrationTest {

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
    private String taskId;

    @BeforeEach
    void createTask() {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> p = rest.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "项目"), headers), JsonNode.class);
        projectId = p.getBody().get("data").get("id").asText();
        Map<String, Object> taskBody = new HashMap<>();
        taskBody.put("title", "任务");
        taskBody.put("dueAt", "2026-10-28T18:00:00Z");
        ResponseEntity<JsonNode> t = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(taskBody, headers), JsonNode.class);
        taskId = t.getBody().get("data").get("id").asText();
    }

    @Test
    void createAbsoluteAndRelativeReminders() {
        HttpHeaders headers = userHeaders("pm_user");

        ResponseEntity<JsonNode> abs = rest.exchange("/api/v1/tasks/" + taskId + "/reminders", HttpMethod.POST,
                new HttpEntity<>(Map.of("mode", "ABSOLUTE", "triggerAt", "2026-10-27T09:00:00Z"), headers), JsonNode.class);
        assertThat(abs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(abs.getBody().get("data").get("mode").asText()).isEqualTo("ABSOLUTE");

        ResponseEntity<JsonNode> rel = rest.exchange("/api/v1/tasks/" + taskId + "/reminders", HttpMethod.POST,
                new HttpEntity<>(Map.of("mode", "RELATIVE", "offsetMinutes", 1440, "anchor", "DUE"), headers), JsonNode.class);
        assertThat(rel.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rel.getBody().get("data").get("mode").asText()).isEqualTo("RELATIVE");
        assertThat(rel.getBody().get("data").get("anchor").asText()).isEqualTo("DUE");
    }

    @Test
    void rejectInvalidReminder() {
        HttpHeaders headers = userHeaders("pm_user");

        // 绝对提醒缺触发时间
        ResponseEntity<JsonNode> abs = rest.exchange("/api/v1/tasks/" + taskId + "/reminders", HttpMethod.POST,
                new HttpEntity<>(Map.of("mode", "ABSOLUTE"), headers), JsonNode.class);
        assertThat(abs.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(abs.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");

        // 相对提醒缺锚定（任务无 startAt，锚定 START 应被拒）
        ResponseEntity<JsonNode> relStart = rest.exchange("/api/v1/tasks/" + taskId + "/reminders", HttpMethod.POST,
                new HttpEntity<>(Map.of("mode", "RELATIVE", "offsetMinutes", 60, "anchor", "START"), headers), JsonNode.class);
        assertThat(relStart.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(relStart.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
