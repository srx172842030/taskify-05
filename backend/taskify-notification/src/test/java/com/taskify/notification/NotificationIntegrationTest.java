package com.taskify.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskify.notification.mq.TaskEvent;
import com.taskify.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "taskify.rocketmq.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
        })
@Testcontainers
class NotificationIntegrationTest {

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
    NotificationService notificationService;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanNotifications() {
        jdbcTemplate.update("DELETE FROM taskify_notification");
    }

    @Test
    void recipientRulesAndMarkRead() {
        // 任务创建 → 仅负责人收到
        notificationService.handle(new TaskEvent("TASK_CREATED", "t1", "p1", "任务标题", "engineer1", "pm_user", null));
        assertThat(unreadCount("engineer1")).isEqualTo(1);
        assertThat(unreadCount("pm_user")).isEqualTo(0);

        // 看板变更 → 负责人 + 创建者收到
        notificationService.handle(new TaskEvent("KANBAN_MOVED", "t1", "p1", "任务标题", "engineer1", "pm_user", "IN_PROGRESS"));
        assertThat(unreadCount("engineer1")).isEqualTo(2);
        assertThat(unreadCount("pm_user")).isEqualTo(1);

        // 标记单条已读
        String id = firstNotificationId("engineer1");
        ResponseEntity<JsonNode> read = rest.exchange("/api/v1/notifications/" + id + "/read", HttpMethod.POST,
                new HttpEntity<>(userHeaders("engineer1")), JsonNode.class);
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unreadCount("engineer1")).isEqualTo(1);
    }

    @Test
    void noNotificationWhenAssigneeBlank() {
        notificationService.handle(new TaskEvent("TASK_CREATED", "t1", "p1", "任务标题", null, "pm_user", null));
        assertThat(unreadCount("pm_user")).isEqualTo(0);
    }

    @Test
    void kanbanMoveNotifiesCreatorEvenWithoutAssignee() {
        // 看板变更：负责人为空时仍通知创建者
        notificationService.handle(new TaskEvent("KANBAN_MOVED", "t1", "p1", "任务标题", null, "pm_user", "IN_PROGRESS"));
        assertThat(unreadCount("pm_user")).isEqualTo(1);
    }

    @Test
    void blockUnblockAndReminderNotifyAssignee() {
        // 被阻塞 → 负责人收到 TASK_BLOCKED
        notificationService.handle(new TaskEvent("TASK_BLOCKED", "t2", "p1", "任务标题", "engineer1", "pm_user", null));
        assertThat(unreadCount("engineer1")).isEqualTo(1);
        assertThat(unreadCount("pm_user")).isEqualTo(0);

        // 解除阻塞 → 负责人收到 TASK_UNBLOCKED
        notificationService.handle(new TaskEvent("TASK_UNBLOCKED", "t2", "p1", "任务标题", "engineer1", "pm_user", null));
        assertThat(unreadCount("engineer1")).isEqualTo(2);

        // 提醒 → 负责人收到 REMINDER
        notificationService.handle(new TaskEvent("REMINDER", "t2", "p1", "任务标题", "engineer1", "pm_user", null));
        assertThat(unreadCount("engineer1")).isEqualTo(3);
    }

    private long unreadCount(String userId) {
        ResponseEntity<JsonNode> res = rest.exchange("/api/v1/notifications/unread-count", HttpMethod.GET,
                new HttpEntity<>(userHeaders(userId)), JsonNode.class);
        return res.getBody().get("data").get("count").asLong();
    }

    private String firstNotificationId(String userId) {
        ResponseEntity<JsonNode> res = rest.exchange("/api/v1/notifications", HttpMethod.GET,
                new HttpEntity<>(userHeaders(userId)), JsonNode.class);
        return res.getBody().get("data").get("items").get(0).get("id").asText();
    }

    private HttpHeaders userHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }
}
