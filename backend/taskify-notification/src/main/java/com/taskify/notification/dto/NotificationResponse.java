package com.taskify.notification.dto;

import com.taskify.notification.entity.Notification;

import java.time.OffsetDateTime;

public record NotificationResponse(
        String id,
        String type,
        String taskId,
        String message,
        Boolean read,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTaskId(),
                n.getMessage(), n.getRead(), n.getCreatedAt());
    }
}
