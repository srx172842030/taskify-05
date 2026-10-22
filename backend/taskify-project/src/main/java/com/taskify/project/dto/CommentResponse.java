package com.taskify.project.dto;

import com.taskify.project.entity.Comment;

import java.time.OffsetDateTime;

public record CommentResponse(
        String id,
        String taskId,
        String authorId,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getTaskId(), c.getAuthorId(),
                c.getContent(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
