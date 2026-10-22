package com.taskify.project.dto;

import com.taskify.project.entity.Task;

import java.time.OffsetDateTime;
import java.util.List;

public record TaskResponse(
        String id,
        String projectId,
        String title,
        String description,
        String assigneeId,
        String status,
        String createdBy,
        OffsetDateTime startAt,
        Boolean startAllDay,
        OffsetDateTime dueAt,
        Boolean dueAllDay,
        boolean blocked,
        List<String> blockerIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TaskResponse from(Task t) {
        return from(t, false, List.of());
    }

    public static TaskResponse from(Task t, boolean blocked, List<String> blockerIds) {
        return new TaskResponse(t.getId(), t.getProjectId(), t.getTitle(), t.getDescription(),
                t.getAssigneeId(), t.getStatus(), t.getCreatedBy(),
                t.getStartAt(), t.getStartAllDay(), t.getDueAt(), t.getDueAllDay(),
                blocked, blockerIds, t.getCreatedAt(), t.getUpdatedAt());
    }
}
