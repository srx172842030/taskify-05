package com.taskify.example.mq;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 生产者示例：示例记录创建后发送事件。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "taskify.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class ExampleEventProducer {

    public static final String TOPIC = "example-created";

    private final RocketMQTemplate rocketMQTemplate;

    public void sendCreated(String id) {
        rocketMQTemplate.convertAndSend(TOPIC, id);
    }
}
