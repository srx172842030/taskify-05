package com.taskify.common.error;

import com.taskify.common.web.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * 全局异常处理：业务异常、参数校验异常、未预期异常统一转为 ProblemDetail（RFC 9457）。
 * 未预期异常不向客户端泄露堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        return build(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(ErrorCode.PARAM_ERROR, detail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        return build(ErrorCode.SERVER_ERROR, "服务器内部错误");
    }

    private ProblemDetail build(ErrorCode code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code.getHttpStatus()), detail);
        pd.setTitle(code.name());
        pd.setProperty("code", code.name());
        pd.setProperty("requestId", RequestContext.getRequestId());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }
}
