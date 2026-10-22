package com.taskify.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record ReminderRequest(
        @NotBlank(message = "提醒形态不能为空") String mode,
        OffsetDateTime triggerAt,
        Integer offsetMinutes,
        String anchor
) {
}
