package com.taskify.project.mq;

import com.taskify.project.entity.Task;

/**
 * 任务领域事件，经 RocketMQ 发送给通知服务。
 * type 取值：TASK_CREATED / TASK_UPDATED / KANBAN_MOVED。
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
    public static TaskEvent of(String type, Task t, String toStatus) {
        return new TaskEvent(type, t.getId(), t.getProjectId(), t.getTitle(),
                t.getAssigneeId(), t.getCreatedBy(), toStatus);
    }
}
