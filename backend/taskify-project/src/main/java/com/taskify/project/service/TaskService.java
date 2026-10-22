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
import com.taskify.project.mapper.TaskMapper;
import com.taskify.project.mq.TaskEvent;
import com.taskify.project.mq.TaskEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 任务：创建（默认 TODO）、按状态过滤列表、详情、修改、相邻列移动。
 * 负责人为逻辑引用（阶段一不校验用户存在性）。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;

    @Autowired(required = false)
    private TaskEventProducer eventProducer;

    public Task create(String userId, String projectId, String title, String description, String assigneeId) {
        if (projectMapper.selectById(projectId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "项目不存在");
        }
        Task t = new Task();
        t.setProjectId(projectId);
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
        t.setStatus(TaskStatus.TODO.name());
        t.setCreatedBy(userId);
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

    public Task update(String id, String title, String description, String assigneeId) {
        Task t = get(id);
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
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
        t.setStatus(target.name());
        taskMapper.updateById(t);
        emit(TaskEvent.of("KANBAN_MOVED", t, target.name()));
        return t;
    }

    private void emit(TaskEvent event) {
        if (eventProducer != null) {
            eventProducer.send(event);
        }
    }
}
