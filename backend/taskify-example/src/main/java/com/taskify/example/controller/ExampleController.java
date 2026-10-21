package com.taskify.example.controller;

import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.common.response.PageResult;
import com.taskify.example.dto.ExampleCreateRequest;
import com.taskify.example.dto.ExampleResponse;
import com.taskify.example.dto.ExampleUpdateRequest;
import com.taskify.example.entity.Example;
import com.taskify.example.service.ExampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @PostMapping
    public ApiResponse<ExampleResponse> create(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                               @Valid @RequestBody ExampleCreateRequest req) {
        Example e = exampleService.create(requireUser(userId), req.name(), req.description());
        return ApiResponse.ok(ExampleResponse.from(e));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExampleResponse> get(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                            @PathVariable String id) {
        return ApiResponse.ok(ExampleResponse.from(exampleService.get(requireUser(userId), id)));
    }

    @GetMapping
    public ApiResponse<PageResult<ExampleResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        PageResult<Example> result = exampleService.list(requireUser(userId), page, size);
        return ApiResponse.ok(new PageResult<>(
                result.getItems().stream().map(ExampleResponse::from).toList(),
                result.getPage(), result.getSize(), result.getTotal()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExampleResponse> update(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                               @PathVariable String id,
                                               @Valid @RequestBody ExampleUpdateRequest req) {
        return ApiResponse.ok(ExampleResponse.from(
                exampleService.update(requireUser(userId), id, req.name(), req.description())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ExampleService.DeleteResult> delete(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        exampleService.delete(requireUser(userId), id);
        return ApiResponse.ok(new ExampleService.DeleteResult(true));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "未认证");
        }
        return userId;
    }
}
