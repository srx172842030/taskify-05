package com.taskify.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskify.auth.captcha.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
        })
@Testcontainers
class AuthIntegrationTest {

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

    @MockitoBean
    CaptchaService captchaService;

    @Autowired
    TestRestTemplate rest;

    @Test
    void registerAndLoginFlowWithin3Seconds() {
        doNothing().when(captchaService).validate(anyString(), anyString());
        Map<String, String> body =
                Map.of("username", "alice", "password", "secret123", "captchaId", "c", "captchaCode", "x");

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            ResponseEntity<JsonNode> reg = rest.postForEntity("/api/v1/auth/register", body, JsonNode.class);
            assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reg.getBody().get("code").asText()).isEqualTo("SUCCESS");
            assertThat(reg.getBody().get("data").get("token").asText()).isNotBlank();

            ResponseEntity<JsonNode> login = rest.postForEntity("/api/v1/auth/login", body, JsonNode.class);
            assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        });
    }

    @Test
    void duplicateUsernameReturnsConflict() {
        doNothing().when(captchaService).validate(anyString(), anyString());
        Map<String, String> body =
                Map.of("username", "bob", "password", "secret123", "captchaId", "c", "captchaCode", "x");
        rest.postForEntity("/api/v1/auth/register", body, JsonNode.class);

        ResponseEntity<JsonNode> dup = rest.postForEntity("/api/v1/auth/register", body, JsonNode.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dup.getBody().get("code").asText()).isEqualTo("CONFLICT");
    }
}
