package com.taskify.example;

import com.fasterxml.jackson.databind.JsonNode;
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
class ExampleIntegrationTest {

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

    @Test
    void crudAndResourceLevelAuthorization() {
        HttpHeaders alice = userHeaders("alice");
        HttpHeaders bob = userHeaders("bob");

        ResponseEntity<JsonNode> created = rest.exchange("/api/v1/examples", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "条目", "description", "描述"), alice), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String id = created.getBody().get("data").get("id").asText();

        // 非属主读取 → FORBIDDEN
        ResponseEntity<JsonNode> forbidden = rest.exchange("/api/v1/examples/" + id, HttpMethod.GET,
                new HttpEntity<>(bob), JsonNode.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody().get("code").asText()).isEqualTo("FORBIDDEN");

        // 属主读取 → OK
        ResponseEntity<JsonNode> got = rest.exchange("/api/v1/examples/" + id, HttpMethod.GET,
                new HttpEntity<>(alice), JsonNode.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
