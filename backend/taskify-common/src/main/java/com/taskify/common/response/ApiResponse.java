package com.taskify.common.response;

import com.taskify.common.error.ErrorCode;
import com.taskify.common.web.RequestContext;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 统一成功响应封装。业务接口直接返回 {@link #ok}，无需自行处理响应结构。
 */
@Getter
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;
    private final String requestId;
    private final OffsetDateTime timestamp;

    private ApiResponse(String code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.timestamp = OffsetDateTime.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.name(), "ok", data, RequestContext.getRequestId());
    }
}
