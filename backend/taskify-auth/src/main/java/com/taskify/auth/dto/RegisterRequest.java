package com.taskify.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度须在 3-64 之间")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "用户名仅允许字母/数字/下划线/连字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 128, message = "密码长度须在 8-128 之间")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码须同时包含字母与数字")
        String password,

        @NotBlank(message = "验证码 ID 不能为空") String captchaId,
        @NotBlank(message = "验证码不能为空") String captchaCode
) {
}
