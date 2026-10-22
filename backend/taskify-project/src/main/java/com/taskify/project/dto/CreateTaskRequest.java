package com.taskify.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateTaskRequest(
        @NotBlank(message = "标题不能为空") @Size(max = 200, message = "标题长度不能超过 200") String title,
        @Size(max = 5000, message = "描述长度不能超过 5000") String description,
        String assigneeId,
        OffsetDateTime startAt,
        Boolean startAllDay,
        OffsetDateTime dueAt,
        Boolean dueAllDay
) {
}
