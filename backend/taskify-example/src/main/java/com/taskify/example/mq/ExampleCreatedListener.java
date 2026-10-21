package com.taskify.example.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费者示例：监听示例记录创建事件。
 * 可通过 taskify.rocketmq.enabled=false 关闭（如集成测试环境）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "taskify.rocketmq.enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(topic = ExampleEventProducer.TOPIC, consumerGroup = "taskify-example")
public class ExampleCreatedListener implements RocketMQListener<String> {

    @Override
    public void onMessage(String id) {
        log.info("收到示例创建事件，id={}", id);
    }
}
