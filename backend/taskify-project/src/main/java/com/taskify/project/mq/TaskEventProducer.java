package com.taskify.project.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 生产者：任务创建/修改/看板移动后发布事件。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "taskify.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class TaskEventProducer {

    public static final String TOPIC = "taskify-task-events";

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public void send(TaskEvent event) {
        try {
            rocketMQTemplate.convertAndSend(TOPIC, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化任务事件失败", e);
        }
    }
}
