package com.taskify.notification.entity;

/**
 * 通知类型：任务发布 / 任务修改 / 看板变更。
 */
public enum NotificationType {
    TASK_CREATED,
    TASK_UPDATED,
    KANBAN_MOVED,
    REMINDER,
    TASK_BLOCKED,
    TASK_UNBLOCKED
}
