package com.taskify.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.PageResult;
import com.taskify.project.entity.Priority;
import com.taskify.project.entity.Task;
import com.taskify.project.entity.TaskStatus;
import com.taskify.project.mapper.ProjectMapper;
import com.taskify.project.mapper.TaskDependencyMapper;
import com.taskify.project.mapper.TaskMapper;
import com.taskify.project.mq.TaskEvent;
import com.taskify.project.mq.TaskEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务：创建（默认 TODO）、按状态过滤列表、详情、修改、相邻列移动。
 * 时间字段：开始/截止可空，各自带「全天」标记；截止不早于开始。
 * 硬阻塞：目标为 IN_PROGRESS 且存在未完成阻塞者时拒绝移动。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final TaskDependencyMapper taskDependencyMapper;

    @Autowired(required = false)
    private TaskEventProducer eventProducer;

    public Task create(String userId, String projectId, String title, String description, String assigneeId,
                       String priority, OffsetDateTime startAt, Boolean startAllDay,
                       OffsetDateTime dueAt, Boolean dueAllDay) {
        if (projectMapper.selectById(projectId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "项目不存在");
        }
        validateTime(startAt, dueAt);
        Task t = new Task();
        t.setProjectId(projectId);
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
        t.setPriority(normalizePriority(priority));
        t.setStatus(TaskStatus.TODO.name());
        t.setCreatedBy(userId);
        t.setStartAt(startAt);
        t.setStartAllDay(Boolean.TRUE.equals(startAllDay));
        t.setDueAt(dueAt);
        t.setDueAllDay(Boolean.TRUE.equals(dueAllDay));
        taskMapper.insert(t);
        emit(TaskEvent.of("TASK_CREATED", t, null));
        return t;
    }

    public Task get(String id) {
        Task t = taskMapper.selectById(id);
        if (t == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return t;
    }

    public PageResult<Task> list(String projectId, String status, long page, long size) {
        LambdaQueryWrapper<Task> wrapper = Wrappers.<Task>lambdaQuery()
                .eq(Task::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Task::getStatus, status);
        }
        wrapper.orderByDesc(Task::getCreatedAt);
        Page<Task> p = taskMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal());
    }

    public Task update(String id, String title, String description, String assigneeId,
                       String priority, OffsetDateTime startAt, Boolean startAllDay,
                       OffsetDateTime dueAt, Boolean dueAllDay) {
        Task t = get(id);
        validateTime(startAt, dueAt);
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
        t.setPriority(normalizePriority(priority));
        t.setStartAt(startAt);
        t.setStartAllDay(Boolean.TRUE.equals(startAllDay));
        t.setDueAt(dueAt);
        t.setDueAllDay(Boolean.TRUE.equals(dueAllDay));
        taskMapper.updateById(t);
        emit(TaskEvent.of("TASK_UPDATED", t, null));
        return t;
    }

    public Task move(String id, String toStatus) {
        Task t = get(id);
        TaskStatus current = TaskStatus.from(t.getStatus());
        TaskStatus target = TaskStatus.from(toStatus);
        if (current == null || target == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "非法看板列");
        }
        if (!TaskStatus.isAdjacent(current, target)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "只能移动到相邻列");
        }
        if (isBlocked(id) && target.order() > current.order()) {
            throw BusinessException.of(ErrorCode.TASK_BLOCKED, "任务被未完成的依赖任务阻塞，无法推进");
        }
        boolean intoDone = target == TaskStatus.DONE;
        boolean outOfDone = current == TaskStatus.DONE;
        List<String> dependents = taskDependencyMapper.selectDependentIds(id);
        Map<String, Boolean> before = new HashMap<>();
        if (intoDone || outOfDone) {
            for (String dep : dependents) {
                before.put(dep, taskDependencyMapper.hasIncompleteBlocker(dep));
            }
        }
        t.setStatus(target.name());
        taskMapper.updateById(t);
        emit(TaskEvent.of("KANBAN_MOVED", t, target.name()));
        if (intoDone || outOfDone) {
            for (String dep : dependents) {
                boolean after = taskDependencyMapper.hasIncompleteBlocker(dep);
                boolean was = before.getOrDefault(dep, false);
                if (was != after) {
                    Task dependent = taskMapper.selectById(dep);
                    if (dependent != null) {
                        emit(TaskEvent.of(after ? "TASK_BLOCKED" : "TASK_UNBLOCKED", dependent, null));
                    }
                }
            }
        }
        return t;
    }

    /**
     * 批量编辑：单事务内对若干任务应用同一 patch（全或无）。
     * patch 含 status 时视为「移动每个任务到目标列」，逐任务校验相邻列与硬阻塞。
     * 任一任务校验失败即整体回滚。
     */
    @Transactional
    public List<Task> batchUpdate(String projectId, List<String> taskIds, JsonNode patch) {
        if (patch == null || patch.size() == 0) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "批量编辑字段不能为空");
        }
        boolean hasStatus = patch.has("status") && !patch.get("status").isNull()
                && !patch.get("status").asText().isBlank();
        List<Task> result = new ArrayList<>();
        for (String id : taskIds) {
            Task t = get(id);
            if (!projectId.equals(t.getProjectId())) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "任务不属于该项目");
            }
            boolean intoDone = false;
            boolean outOfDone = false;
            Map<String, Boolean> beforeBlocked = new HashMap<>();
            if (hasStatus) {
                String toStatus = patch.get("status").asText();
                TaskStatus current = TaskStatus.from(t.getStatus());
                TaskStatus target = TaskStatus.from(toStatus);
                if (current == null || target == null) {
                    throw BusinessException.of(ErrorCode.PARAM_ERROR, "非法看板列");
                }
                if (!TaskStatus.isAdjacent(current, target)) {
                    throw BusinessException.of(ErrorCode.PARAM_ERROR, "只能移动到相邻列");
                }
                if (isBlocked(id) && target.order() > current.order()) {
                    throw BusinessException.of(ErrorCode.TASK_BLOCKED, "任务被未完成的依赖任务阻塞，无法推进");
                }
                intoDone = target == TaskStatus.DONE;
                outOfDone = current == TaskStatus.DONE;
                if (intoDone || outOfDone) {
                    for (String dep : taskDependencyMapper.selectDependentIds(id)) {
                        beforeBlocked.put(dep, taskDependencyMapper.hasIncompleteBlocker(dep));
                    }
                }
                t.setStatus(target.name());
            }
            if (patch.has("assigneeId")) {
                t.setAssigneeId(patch.get("assigneeId").isNull() ? null : patch.get("assigneeId").asText());
            }
            if (patch.has("priority")) {
                t.setPriority(normalizePriority(patch.get("priority").isNull() ? null : patch.get("priority").asText()));
            }
            if (patch.has("startAt")) {
                t.setStartAt(dateValue(patch.get("startAt")));
            }
            if (patch.has("startAllDay")) {
                t.setStartAllDay(!patch.get("startAllDay").isNull() && patch.get("startAllDay").asBoolean());
            }
            if (patch.has("dueAt")) {
                t.setDueAt(dateValue(patch.get("dueAt")));
            }
            if (patch.has("dueAllDay")) {
                t.setDueAllDay(!patch.get("dueAllDay").isNull() && patch.get("dueAllDay").asBoolean());
            }
            validateTime(t.getStartAt(), t.getDueAt());
            taskMapper.updateById(t);
            // 阻塞状态重算（与单任务 move 一致）：进/出 DONE 时更新依赖任务的阻塞状态并发出事件
            if (intoDone || outOfDone) {
                for (Map.Entry<String, Boolean> e : beforeBlocked.entrySet()) {
                    boolean after = taskDependencyMapper.hasIncompleteBlocker(e.getKey());
                    if (e.getValue() != after) {
                        Task dependent = taskMapper.selectById(e.getKey());
                        if (dependent != null) {
                            emit(TaskEvent.of(after ? "TASK_BLOCKED" : "TASK_UNBLOCKED", dependent, null));
                        }
                    }
                }
            }
            result.add(t);
        }
        for (Task t : result) {
            emit(TaskEvent.of(hasStatus ? "KANBAN_MOVED" : "TASK_UPDATED", t,
                    hasStatus ? patch.get("status").asText() : null));
        }
        return result;
    }

    public boolean isBlocked(String taskId) {
        return taskDependencyMapper.hasIncompleteBlocker(taskId);
    }

    public List<String> blockerIds(String taskId) {
        return taskDependencyMapper.selectIncompleteBlockerIds(taskId);
    }

    private OffsetDateTime dateValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return OffsetDateTime.parse(node.asText());
    }

    private void validateTime(OffsetDateTime startAt, OffsetDateTime dueAt) {
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "截止时间不能早于开始时间");
        }
    }

    /** 归一化优先级：空/空白返回 null（未设置），非法取值抛 PARAM_ERROR。 */
    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        Priority p = Priority.from(priority);
        if (p == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "非法优先级");
        }
        return p.name();
    }

    private void emit(TaskEvent event) {
        if (eventProducer != null) {
            eventProducer.send(event);
        }
    }
}
