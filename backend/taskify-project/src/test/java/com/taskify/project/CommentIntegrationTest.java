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
class CommentIntegrationTest {

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

    private String taskId;

    @BeforeEach
    void createTask() {
        HttpHeaders headers = userHeaders("pm_user");
        ResponseEntity<JsonNode> p = rest.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "项目"), headers), JsonNode.class);
        String projectId = p.getBody().get("data").get("id").asText();
        ResponseEntity<JsonNode> t = rest.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "任务"), headers), JsonNode.class);
        taskId = t.getBody().get("data").get("id").asText();
    }

    @Test
    void onlyAuthorCanEditOrDeleteComment() {
        HttpHeaders author = userHeaders("engineer1");
        HttpHeaders other = userHeaders("engineer2");

        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                new HttpEntity<>(Map.of("content", "这个方案可行。"), author), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String commentId = created.getBody().get("data").get("id").asText();

        // 非作者编辑 → FORBIDDEN
        ResponseEntity<JsonNode> forbidden = rest.exchange("/api/v1/comments/" + commentId, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("content", "篡改"), other), JsonNode.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 作者编辑 → OK
        ResponseEntity<JsonNode> edited = rest.exchange("/api/v1/comments/" + commentId, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("content", "已修改"), author), JsonNode.class);
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 非作者删除 → FORBIDDEN
        ResponseEntity<JsonNode> delForbidden = rest.exchange("/api/v1/comments/" + commentId, HttpMethod.DELETE,
                new HttpEntity<>(other), JsonNode.class);
        assertThat(delForbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectBlankComment() {
        ResponseEntity<JsonNode> blank = rest.exchange("/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                new HttpEntity<>(Map.of("content", "  "), userHeaders("engineer1")), JsonNode.class);
        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blank.getBody().get("code").asText()).isEqualTo("PARAM_ERROR");
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
