package com.taskify.project.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.project.dto.CreateDependencyRequest;
import com.taskify.project.dto.DependencyResponse;
import com.taskify.project.service.DependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    @GetMapping("/tasks/{taskId}/dependencies")
    public ApiResponse<DependencyResponse> list(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                @PathVariable String taskId) {
        requireUser(userId);
        return ApiResponse.ok(dependencyService.list(taskId));
    }

    @PostMapping("/tasks/{dependentId}/dependencies")
    public ApiResponse<Map<String, Boolean>> create(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                    @PathVariable String dependentId,
                                                    @Valid @RequestBody CreateDependencyRequest req) {
        requireUser(userId);
        dependencyService.create(dependentId, req.blockerTaskId());
        return ApiResponse.ok(Map.of("created", true));
    }

    @DeleteMapping("/tasks/{dependentId}/dependencies/{blockerTaskId}")
    public ApiResponse<Map<String, Boolean>> remove(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                    @PathVariable String dependentId,
                                                    @PathVariable String blockerTaskId) {
        requireUser(userId);
        dependencyService.remove(dependentId, blockerTaskId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }
}
