package com.taskify.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExampleUpdateRequest(
        @NotBlank(message = "名称不能为空") @Size(max = 128, message = "名称长度不能超过 128") String name,
        @Size(max = 2000, message = "描述长度不能超过 2000") String description
) {
}
