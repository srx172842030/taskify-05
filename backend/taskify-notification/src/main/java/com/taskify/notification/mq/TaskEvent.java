package com.taskify.notification.mq;

/**
 * 任务领域事件（与 taskify-project 发送的事件结构一致）。
 */
public record TaskEvent(
        String type,
        String taskId,
        String projectId,
        String title,
        String assigneeId,
        String createdBy,
        String toStatus
) {
}
