package com.taskify.project.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.taskify.project.entity.Reminder;
import com.taskify.project.entity.Task;
import com.taskify.project.mapper.ReminderMapper;
import com.taskify.project.mapper.TaskMapper;
import com.taskify.project.mq.TaskEvent;
import com.taskify.project.mq.TaskEventProducer;
import com.taskify.project.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 提醒调度器：周期扫描（≤1s）到期未触发提醒，置 fired 后发布 REMINDER 事件。
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderMapper reminderMapper;
    private final TaskMapper taskMapper;
    private final ReminderService reminderService;

    @Autowired(required = false)
    private TaskEventProducer eventProducer;

    @Scheduled(fixedRate = 1000)
    public void fireDueReminders() {
        List<Reminder> reminders = reminderMapper.selectList(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getFired, false));
        OffsetDateTime now = OffsetDateTime.now();
        for (Reminder r : reminders) {
            OffsetDateTime triggerAt = reminderService.resolveTriggerAt(r);
            if (triggerAt != null && !triggerAt.isAfter(now)) {
                r.setFired(true);
                reminderMapper.updateById(r);
                Task t = taskMapper.selectById(r.getTaskId());
                if (t != null) {
                    emit(TaskEvent.of("REMINDER", t, null));
                }
            }
        }
    }

    private void emit(TaskEvent event) {
        if (eventProducer != null) {
            eventProducer.send(event);
        }
    }
}
