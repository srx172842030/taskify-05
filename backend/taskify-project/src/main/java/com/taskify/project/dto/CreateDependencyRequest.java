package com.taskify.project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDependencyRequest(
        @NotBlank(message = "阻塞任务不能为空") String blockerTaskId
) {
}
