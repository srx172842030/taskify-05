package com.taskify.notification.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.common.response.PageResult;
import com.taskify.notification.dto.NotificationResponse;
import com.taskify.notification.entity.Notification;
import com.taskify.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return notificationService.stream(requireUser(userId));
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        requireUser(userId);
        PageResult<Notification> result = notificationService.list(userId, page, size);
        return ApiResponse.ok(new PageResult<>(
                result.getItems().stream().map(NotificationResponse::from).toList(),
                result.getPage(), result.getSize(), result.getTotal()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCount> unreadCount(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ApiResponse.ok(new UnreadCount(notificationService.unreadCount(requireUser(userId))));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                      @PathVariable String notificationId) {
        return ApiResponse.ok(NotificationResponse.from(
                notificationService.markRead(requireUser(userId), notificationId)));
    }

    @PostMapping("/read-all")
    public ApiResponse<ReadAllResult> markAllRead(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ApiResponse.ok(new ReadAllResult(notificationService.markAllRead(requireUser(userId))));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }

    public record UnreadCount(long count) {
    }

    public record ReadAllResult(long updated) {
    }
}
