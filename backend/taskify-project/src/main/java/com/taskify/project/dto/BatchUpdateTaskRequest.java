package com.taskify.project.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量编辑请求：对同一项目内若干任务一次性应用同一变更（全或无）。
 * patch 仅包含要修改的字段；可空字段传 null 表示清空；不含某字段表示保持不变。
 */
public record BatchUpdateTaskRequest(
        @NotEmpty(message = "taskIds 不能为空") List<String> taskIds,
        @NotNull(message = "patch 不能为空") JsonNode patch
) {
}
