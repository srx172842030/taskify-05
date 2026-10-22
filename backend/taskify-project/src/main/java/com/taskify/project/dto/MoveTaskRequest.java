package com.taskify.project.dto;

import jakarta.validation.constraints.NotBlank;

public record MoveTaskRequest(
        @NotBlank(message = "目标列不能为空") String toStatus
) {
}
