package com.taskify.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.project.dto.ReminderRequest;
import com.taskify.project.entity.Reminder;
import com.taskify.project.entity.ReminderMode;
import com.taskify.project.entity.Task;
import com.taskify.project.mapper.ReminderMapper;
import com.taskify.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 提醒节点：绝对时间点（ABSOLUTE）或相对提前量（RELATIVE）两种形态，增删改查与解析。
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderMapper reminderMapper;
    private final TaskMapper taskMapper;

    public List<Reminder> list(String taskId) {
        return reminderMapper.selectList(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getTaskId, taskId)
                .orderByAsc(Reminder::getCreatedAt));
    }

    public Reminder create(String taskId, ReminderRequest req) {
        if (taskMapper.selectById(taskId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        }
        Reminder r = new Reminder();
        r.setTaskId(taskId);
        r.setFired(false);
        apply(r, req);
        reminderMapper.insert(r);
        return r;
    }

    public Reminder update(String reminderId, ReminderRequest req) {
        Reminder r = reminderMapper.selectById(reminderId);
        if (r == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "提醒不存在");
        }
        apply(r, req);
        r.setFired(false);
        reminderMapper.updateById(r);
        return r;
    }

    public void delete(String reminderId) {
        reminderMapper.deleteById(reminderId);
    }

    private void apply(Reminder r, ReminderRequest req) {
        ReminderMode mode;
        try {
            mode = ReminderMode.valueOf(req.mode());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "非法提醒形态");
        }
        if (mode == ReminderMode.ABSOLUTE) {
            if (req.triggerAt() == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "绝对提醒需要触发时间");
            }
            r.setMode(ReminderMode.ABSOLUTE.name());
            r.setTriggerAt(req.triggerAt());
            r.setOffsetMinutes(null);
            r.setAnchor(null);
        } else {
            if (req.offsetMinutes() == null || req.offsetMinutes() <= 0
                    || req.anchor() == null || req.anchor().isBlank()) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "相对提醒需要有效的提前分钟数与锚定时间");
            }
            if (!"START".equals(req.anchor()) && !"DUE".equals(req.anchor())) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "锚定时间须为 START 或 DUE");
            }
            Task t = taskMapper.selectById(r.getTaskId());
            if (t == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
            }
            if ("START".equals(req.anchor()) && t.getStartAt() == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "任务未设置开始时间");
            }
            if ("DUE".equals(req.anchor()) && t.getDueAt() == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "任务未设置截止时间");
            }
            r.setMode(ReminderMode.RELATIVE.name());
            r.setTriggerAt(null);
            r.setOffsetMinutes(req.offsetMinutes());
            r.setAnchor(req.anchor());
        }
    }

    /** 解析相对提醒的实际触发时间；绝对提醒直接返回 triggerAt；无法解析返回 null。 */
    public OffsetDateTime resolveTriggerAt(Reminder r) {
        if (ReminderMode.ABSOLUTE.name().equals(r.getMode())) {
            return r.getTriggerAt();
        }
        Task t = taskMapper.selectById(r.getTaskId());
        if (t == null) {
            return null;
        }
        OffsetDateTime anchorTime = "START".equals(r.getAnchor()) ? t.getStartAt() : t.getDueAt();
        if (anchorTime == null || r.getOffsetMinutes() == null) {
            return null;
        }
        return anchorTime.minusMinutes(r.getOffsetMinutes());
    }
}
