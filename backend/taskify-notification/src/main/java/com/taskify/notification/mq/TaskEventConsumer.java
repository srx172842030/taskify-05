package com.taskify.notification.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费者：消费任务事件，生成通知并通过 SSE 推送。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "taskify.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(topic = "taskify-task-events", consumerGroup = "taskify-notification")
public class TaskEventConsumer implements RocketMQListener<String> {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public TaskEventConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        try {
            TaskEvent event = objectMapper.readValue(message, TaskEvent.class);
            notificationService.handle(event);
        } catch (Exception e) {
            log.error("处理任务事件失败: {}", message, e);
        }
    }
}
