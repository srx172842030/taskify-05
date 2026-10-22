package com.taskify.project.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.common.response.PageResult;
import com.taskify.project.dto.CreateTaskRequest;
import com.taskify.project.dto.MoveTaskRequest;
import com.taskify.project.dto.TaskResponse;
import com.taskify.project.dto.UpdateTaskRequest;
import com.taskify.project.entity.Task;
import com.taskify.project.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    public ApiResponse<TaskResponse> create(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                            @PathVariable String projectId,
                                            @Valid @RequestBody CreateTaskRequest req) {
        return ApiResponse.ok(toResponse(taskService.create(
                requireUser(userId), projectId, req.title(), req.description(), req.assigneeId(),
                req.startAt(), req.startAllDay(), req.dueAt(), req.dueAllDay())));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ApiResponse<PageResult<TaskResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long size) {
        requireUser(userId);
        PageResult<Task> result = taskService.list(projectId, status, page, size);
        return ApiResponse.ok(new PageResult<>(
                result.getItems().stream().map(this::toResponse).toList(),
                result.getPage(), result.getSize(), result.getTotal()));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskResponse> get(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                         @PathVariable String taskId) {
        requireUser(userId);
        return ApiResponse.ok(toResponse(taskService.get(taskId)));
    }

    @PatchMapping("/tasks/{taskId}")
    public ApiResponse<TaskResponse> update(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                            @PathVariable String taskId,
                                            @Valid @RequestBody UpdateTaskRequest req) {
        requireUser(userId);
        return ApiResponse.ok(toResponse(
                taskService.update(taskId, req.title(), req.description(), req.assigneeId(),
                        req.startAt(), req.startAllDay(), req.dueAt(), req.dueAllDay())));
    }

    @PostMapping("/tasks/{taskId}/move")
    public ApiResponse<TaskResponse> move(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                          @PathVariable String taskId,
                                          @Valid @RequestBody MoveTaskRequest req) {
        requireUser(userId);
        return ApiResponse.ok(toResponse(taskService.move(taskId, req.toStatus())));
    }

    private TaskResponse toResponse(Task t) {
        return TaskResponse.from(t, taskService.isBlocked(t.getId()), taskService.blockerIds(t.getId()));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }
}
