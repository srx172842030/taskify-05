package com.taskify.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "内容不能为空") @Size(max = 5000, message = "内容长度不能超过 5000") String content
) {
}
