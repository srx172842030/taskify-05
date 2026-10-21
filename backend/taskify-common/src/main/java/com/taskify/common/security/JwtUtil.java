package com.taskify.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具（无状态会话）：签发与校验。认证服务负责签发，网关负责校验。
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    public static String generate(String secret, long ttlSeconds, String subject, String username) {
        SecretKey key = key(secret);
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlSeconds * 1000))
                .signWith(key)
                .compact();
    }

    public static Claims parse(String secret, String token) {
        return Jwts.parser()
                .verifyWith(key(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean isValid(String secret, String token) {
        try {
            parse(secret, token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
