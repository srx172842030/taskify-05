package com.taskify.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.PageResult;
import com.taskify.notification.dto.NotificationResponse;
import com.taskify.notification.entity.Notification;
import com.taskify.notification.mapper.NotificationMapper;
import com.taskify.notification.mq.TaskEvent;
import com.taskify.notification.sse.SseConnectionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 通知：SSE 订阅、列表、未读计数、已读；并消费任务事件生成通知。
 * 接收者规则：创建/修改 → 负责人；看板变更 → 负责人 + 创建者（去重）；负责人为空时不产生负责人通知。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SseConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    public SseEmitter stream(String userId) {
        return registry.register(userId);
    }

    public PageResult<Notification> list(String userId, long page, long size) {
        Page<Notification> p = notificationMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getRecipientId, userId)
                        .orderByDesc(Notification::getCreatedAt));
        return PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal());
    }

    public long unreadCount(String userId) {
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getRead, false));
        return count == null ? 0 : count;
    }

    public Notification markRead(String userId, String id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (!n.getRecipientId().equals(userId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "无权操作该通知");
        }
        n.setRead(true);
        notificationMapper.updateById(n);
        return n;
    }

    public long markAllRead(String userId) {
        return notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getRead, false)
                .set(Notification::getRead, true));
    }

    public void handle(TaskEvent event) {
        switch (event.type()) {
            case "TASK_CREATED" -> notifyAssignee(event, "新任务「" + event.title() + "」已发布并指派给你");
            case "TASK_UPDATED" -> notifyAssignee(event, "任务「" + event.title() + "」已被修改");
            case "KANBAN_MOVED" -> {
                String message = "任务「" + event.title() + "」已移动到「" + statusLabel(event.toStatus()) + "」";
                Set<String> recipients = new LinkedHashSet<>();
                if (notBlank(event.assigneeId())) {
                    recipients.add(event.assigneeId());
                }
                if (notBlank(event.createdBy())) {
                    recipients.add(event.createdBy());
                }
                for (String recipient : recipients) {
                    createAndPush(recipient, event, message);
                }
            }
            default -> {
                // 未知类型忽略
            }
        }
    }

    private void notifyAssignee(TaskEvent event, String message) {
        if (notBlank(event.assigneeId())) {
            createAndPush(event.assigneeId(), event, message);
        }
    }

    private void createAndPush(String recipientId, TaskEvent event, String message) {
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setType(event.type());
        n.setTaskId(event.taskId());
        n.setMessage(message);
        n.setRead(false);
        notificationMapper.insert(n);
        try {
            registry.push(recipientId, objectMapper.writeValueAsString(NotificationResponse.from(n)));
        } catch (JsonProcessingException ignored) {
            // 推送失败不影响落库
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "TODO" -> "待办事项";
            case "IN_PROGRESS" -> "进行中";
            case "IN_REVIEW" -> "审查中";
            case "DONE" -> "已完成";
            default -> status;
        };
    }
}
