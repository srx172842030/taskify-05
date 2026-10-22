package com.taskify.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.project.dto.DependencyResponse;
import com.taskify.project.entity.Task;
import com.taskify.project.entity.TaskDependency;
import com.taskify.project.mapper.TaskDependencyMapper;
import com.taskify.project.mapper.TaskMapper;
import com.taskify.project.mq.TaskEvent;
import com.taskify.project.mq.TaskEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 阻塞关系：建立/删除、查询；校验同项目、非自阻塞、无环、不重复，并在阻塞状态变化时发布事件。
 */
@Service
@RequiredArgsConstructor
public class DependencyService {

    private final TaskDependencyMapper taskDependencyMapper;
    private final TaskMapper taskMapper;

    @Autowired(required = false)
    private TaskEventProducer eventProducer;

    public DependencyResponse list(String taskId) {
        List<DependencyResponse.TaskRef> blockers = taskDependencyMapper.selectBlockerIds(taskId).stream()
                .map(this::toRef).toList();
        List<DependencyResponse.TaskRef> dependents = taskDependencyMapper.selectDependentIds(taskId).stream()
                .map(this::toRef).toList();
        return new DependencyResponse(blockers, dependents);
    }

    public void create(String dependentId, String blockerId) {
        if (dependentId.equals(blockerId)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "不能阻塞自身");
        }
        Task dependent = taskMapper.selectById(dependentId);
        Task blocker = taskMapper.selectById(blockerId);
        if (dependent == null || blocker == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (!dependent.getProjectId().equals(blocker.getProjectId())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "只能在同一项目内建立阻塞关系");
        }
        if (wouldCreateCycle(blockerId, dependentId)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "不能形成循环依赖");
        }
        Long exists = taskDependencyMapper.selectCount(new LambdaQueryWrapper<TaskDependency>()
                .eq(TaskDependency::getBlockerId, blockerId)
                .eq(TaskDependency::getDependentId, dependentId));
        if (exists != null && exists > 0) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "阻塞关系已存在");
        }
        boolean before = taskDependencyMapper.hasIncompleteBlocker(dependentId);
        TaskDependency d = new TaskDependency();
        d.setBlockerId(blockerId);
        d.setDependentId(dependentId);
        taskDependencyMapper.insert(d);
        boolean after = taskDependencyMapper.hasIncompleteBlocker(dependentId);
        if (!before && after) {
            emit(TaskEvent.of("TASK_BLOCKED", dependent, null));
        }
    }

    public void remove(String dependentId, String blockerId) {
        boolean before = taskDependencyMapper.hasIncompleteBlocker(dependentId);
        taskDependencyMapper.delete(new LambdaQueryWrapper<TaskDependency>()
                .eq(TaskDependency::getBlockerId, blockerId)
                .eq(TaskDependency::getDependentId, dependentId));
        boolean after = taskDependencyMapper.hasIncompleteBlocker(dependentId);
        if (before && !after) {
            Task dependent = taskMapper.selectById(dependentId);
            if (dependent != null) {
                emit(TaskEvent.of("TASK_UNBLOCKED", dependent, null));
            }
        }
    }

    private DependencyResponse.TaskRef toRef(String id) {
        Task t = taskMapper.selectById(id);
        if (t == null) {
            return new DependencyResponse.TaskRef(id, null, null);
        }
        return new DependencyResponse.TaskRef(t.getId(), t.getTitle(), t.getStatus());
    }

    private boolean wouldCreateCycle(String blockerId, String dependentId) {
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(dependentId);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (cur.equals(blockerId)) {
                return true;
            }
            if (!visited.add(cur)) {
                continue;
            }
            for (String child : taskDependencyMapper.selectDependentIds(cur)) {
                stack.push(child);
            }
        }
        return false;
    }

    private void emit(TaskEvent event) {
        if (eventProducer != null) {
            eventProducer.send(event);
        }
    }
}
