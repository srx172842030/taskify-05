package com.taskify.project.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.common.response.PageResult;
import com.taskify.project.dto.CommentResponse;
import com.taskify.project.dto.CreateCommentRequest;
import com.taskify.project.dto.UpdateCommentRequest;
import com.taskify.project.entity.Comment;
import com.taskify.project.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tasks/{taskId}/comments")
    public ApiResponse<CommentResponse> create(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                               @PathVariable String taskId,
                                               @Valid @RequestBody CreateCommentRequest req) {
        return ApiResponse.ok(CommentResponse.from(
                commentService.create(requireUser(userId), taskId, req.content())));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public ApiResponse<PageResult<CommentResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        requireUser(userId);
        PageResult<Comment> result = commentService.list(taskId, page, size);
        return ApiResponse.ok(new PageResult<>(
                result.getItems().stream().map(CommentResponse::from).toList(),
                result.getPage(), result.getSize(), result.getTotal()));
    }

    @PatchMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> update(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                               @PathVariable String commentId,
                                               @Valid @RequestBody UpdateCommentRequest req) {
        return ApiResponse.ok(CommentResponse.from(
                commentService.update(requireUser(userId), commentId, req.content())));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<CommentService.DeleteResult> delete(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String commentId) {
        commentService.delete(requireUser(userId), commentId);
        return ApiResponse.ok(new CommentService.DeleteResult(true));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }
}
