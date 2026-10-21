package com.taskify.common.error;

import lombok.Getter;

/**
 * 稳定业务错误码，与 HTTP 状态码一一映射。契约见 contracts/api-conventions.md。
 */
@Getter
public enum ErrorCode {
    SUCCESS(200),
    PARAM_ERROR(400),
    UNAUTHENTICATED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    RATE_LIMITED(429),
    SERVER_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }
}
