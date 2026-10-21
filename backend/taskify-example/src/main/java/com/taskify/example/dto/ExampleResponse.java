package com.taskify.example.dto;

import com.taskify.example.entity.Example;

import java.time.OffsetDateTime;

public record ExampleResponse(
        String id,
        String name,
        String description,
        String ownerId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ExampleResponse from(Example e) {
        return new ExampleResponse(e.getId(), e.getName(), e.getDescription(),
                e.getOwnerId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
