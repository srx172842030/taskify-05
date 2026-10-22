package com.taskify.project.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.common.response.PageResult;
import com.taskify.project.dto.CreateProjectRequest;
import com.taskify.project.dto.ProjectResponse;
import com.taskify.project.entity.Project;
import com.taskify.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<ProjectResponse> create(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                               @Valid @RequestBody CreateProjectRequest req) {
        return ApiResponse.ok(ProjectResponse.from(
                projectService.create(requireUser(userId), req.name(), req.description())));
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        requireUser(userId);
        PageResult<Project> result = projectService.list(page, size);
        return ApiResponse.ok(new PageResult<>(
                result.getItems().stream().map(ProjectResponse::from).toList(),
                result.getPage(), result.getSize(), result.getTotal()));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                            @PathVariable String projectId) {
        requireUser(userId);
        return ApiResponse.ok(ProjectResponse.from(projectService.get(projectId)));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }
}
