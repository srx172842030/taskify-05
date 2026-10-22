package com.taskify.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taskify.auth.entity.User;
import com.taskify.auth.mapper.UserMapper;
import com.taskify.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户列表：供项目服务前端选择任务负责人。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        List<UserResponse> users = userMapper.selectList(
                        Wrappers.<User>lambdaQuery().orderByAsc(User::getUsername))
                .stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername()))
                .toList();
        return ApiResponse.ok(users);
    }

    public record UserResponse(String id, String username) {
    }
}
