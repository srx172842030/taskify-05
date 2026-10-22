package com.taskify.project.dto;

import java.util.List;

public record DependencyResponse(
        List<TaskRef> blockers,
        List<TaskRef> dependents
) {
    public record TaskRef(String taskId, String title, String status) {
    }
}
