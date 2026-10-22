package com.taskify.project.dto;

import com.taskify.project.entity.Project;

import java.time.OffsetDateTime;

public record ProjectResponse(
        String id,
        String name,
        String description,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
