package com.taskify.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taskify.auth.captcha.CaptchaService;
import com.taskify.auth.entity.User;
import com.taskify.auth.mapper.UserMapper;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;

    @Value("${taskify.jwt.secret}")
    private String jwtSecret;

    @Value("${taskify.jwt.ttl-hours:24}")
    private long ttlHours;

    public AuthResponse register(String username, String password, String captchaId, String captchaCode) {
        captchaService.validate(captchaId, captchaCode);

        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userMapper.insert(user);

        return new AuthResponse(issueToken(user), new UserResponse(user.getId(), user.getUsername()));
    }

    public AuthResponse login(String username, String password, String captchaId, String captchaCode) {
        captchaService.validate(captchaId, captchaCode);

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.UNAUTHENTICATED, "用户名或密码错误");
        }

        return new AuthResponse(issueToken(user), new UserResponse(user.getId(), user.getUsername()));
    }

    public LogoutResponse logout() {
        // 无状态登出：客户端丢弃 token；第一阶段不引入服务端吊销列表
        return new LogoutResponse(true);
    }

    private String issueToken(User user) {
        return JwtUtil.generate(jwtSecret, ttlHours * 3600, user.getId(), user.getUsername());
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UserResponse(String id, String username) {
    }

    public record LogoutResponse(boolean loggedOut) {
    }
}
