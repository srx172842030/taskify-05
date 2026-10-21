package com.taskify.auth.controller;

import com.taskify.auth.captcha.CaptchaService;
import com.taskify.auth.dto.LoginRequest;
import com.taskify.auth.dto.RegisterRequest;
import com.taskify.auth.service.AuthService;
import com.taskify.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @PostMapping("/captcha")
    public ApiResponse<CaptchaService.CaptchaResponse> captcha() {
        return ApiResponse.ok(captchaService.generate());
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req.username(), req.password(), req.captchaId(), req.captchaCode()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req.username(), req.password(), req.captchaId(), req.captchaCode()));
    }

    @PostMapping("/logout")
    public ApiResponse<AuthService.LogoutResponse> logout() {
        return ApiResponse.ok(authService.logout());
    }
}
