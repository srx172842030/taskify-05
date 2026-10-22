package com.taskify.project.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.project.dto.ReminderRequest;
import com.taskify.project.dto.ReminderResponse;
import com.taskify.project.service.ReminderService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping("/tasks/{taskId}/reminders")
    public ApiResponse<List<ReminderResponse>> list(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                    @PathVariable String taskId) {
        requireUser(userId);
        return ApiResponse.ok(reminderService.list(taskId).stream().map(ReminderResponse::from).toList());
    }

    @PostMapping("/tasks/{taskId}/reminders")
    public ApiResponse<ReminderResponse> create(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                @PathVariable String taskId,
                                                @Valid @RequestBody ReminderRequest req) {
        requireUser(userId);
        return ApiResponse.ok(ReminderResponse.from(reminderService.create(taskId, req)));
    }

    @PatchMapping("/reminders/{reminderId}")
    public ApiResponse<ReminderResponse> update(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                @PathVariable String reminderId,
                                                @Valid @RequestBody ReminderRequest req) {
        requireUser(userId);
        return ApiResponse.ok(ReminderResponse.from(reminderService.update(reminderId, req)));
    }

    @DeleteMapping("/reminders/{reminderId}")
    public ApiResponse<Map<String, Boolean>> delete(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                    @PathVariable String reminderId) {
        requireUser(userId);
        reminderService.delete(reminderId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }
}
