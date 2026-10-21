package com.taskify.common;

import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.ApiResponse;
import com.taskify.common.response.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonContractTest {

    @Test
    void apiResponseOkUsesSuccessCode() {
        ApiResponse<String> resp = ApiResponse.ok("data");
        assertThat(resp.getCode()).isEqualTo("SUCCESS");
        assertThat(resp.getData()).isEqualTo("data");
        assertThat(resp.getTimestamp()).isNotNull();
    }

    @Test
    void errorCodeMapsHttpStatus() {
        assertThat(ErrorCode.PARAM_ERROR.getHttpStatus()).isEqualTo(400);
        assertThat(ErrorCode.UNAUTHENTICATED.getHttpStatus()).isEqualTo(401);
        assertThat(ErrorCode.FORBIDDEN.getHttpStatus()).isEqualTo(403);
        assertThat(ErrorCode.NOT_FOUND.getHttpStatus()).isEqualTo(404);
        assertThat(ErrorCode.CONFLICT.getHttpStatus()).isEqualTo(409);
        assertThat(ErrorCode.RATE_LIMITED.getHttpStatus()).isEqualTo(429);
        assertThat(ErrorCode.SERVER_ERROR.getHttpStatus()).isEqualTo(500);
    }

    @Test
    void pageResultComputesTotalPages() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 1, 2, 5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getItems()).hasSize(2);
    }
}
