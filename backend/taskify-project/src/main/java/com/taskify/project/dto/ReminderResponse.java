package com.taskify.project.dto;

import com.taskify.project.entity.Reminder;

import java.time.OffsetDateTime;

public record ReminderResponse(
        String id,
        String taskId,
        String mode,
        OffsetDateTime triggerAt,
        Integer offsetMinutes,
        String anchor,
        Boolean fired,
        OffsetDateTime createdAt
) {
    public static ReminderResponse from(Reminder r) {
        return new ReminderResponse(r.getId(), r.getTaskId(), r.getMode(), r.getTriggerAt(),
                r.getOffsetMinutes(), r.getAnchor(), r.getFired(), r.getCreatedAt());
    }
}
