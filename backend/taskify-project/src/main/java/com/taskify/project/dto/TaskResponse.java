package com.taskify.project.dto;

import com.taskify.project.entity.Task;

import java.time.OffsetDateTime;

public record TaskResponse(
        String id,
        String projectId,
        String title,
        String description,
        String assigneeId,
        String status,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(t.getId(), t.getProjectId(), t.getTitle(), t.getDescription(),
                t.getAssigneeId(), t.getStatus(), t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
