package com.taskify.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.PageResult;
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

import java.time.OffsetDateTime;
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
                       OffsetDateTime startAt, Boolean startAllDay, OffsetDateTime dueAt, Boolean dueAllDay) {
        if (projectMapper.selectById(projectId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "项目不存在");
        }
        validateTime(startAt, dueAt);
        Task t = new Task();
        t.setProjectId(projectId);
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
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
                       OffsetDateTime startAt, Boolean startAllDay, OffsetDateTime dueAt, Boolean dueAllDay) {
        Task t = get(id);
        validateTime(startAt, dueAt);
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
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

    public boolean isBlocked(String taskId) {
        return taskDependencyMapper.hasIncompleteBlocker(taskId);
    }

    public List<String> blockerIds(String taskId) {
        return taskDependencyMapper.selectIncompleteBlockerIds(taskId);
    }

    private void validateTime(OffsetDateTime startAt, OffsetDateTime dueAt) {
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "截止时间不能早于开始时间");
        }
    }

    private void emit(TaskEvent event) {
        if (eventProducer != null) {
            eventProducer.send(event);
        }
    }
}
