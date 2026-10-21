package com.taskify.common.error;

import lombok.Getter;

/**
 * 业务异常：携带稳定错误码与可读信息，由全局异常处理器转为 ProblemDetail。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public static BusinessException of(ErrorCode code, String detail) {
        return new BusinessException(code, detail);
    }
}
