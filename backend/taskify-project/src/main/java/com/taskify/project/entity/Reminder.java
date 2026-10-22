package com.taskify.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 提醒节点：绝对时间点（triggerAt）或相对提前量（offsetMinutes + anchor，anchor 取 START/DUE）。
 */
@Getter
@Setter
@TableName("taskify_reminder")
public class Reminder {

    @TableId(type = IdType.INPUT)
    private String id;

    private String taskId;

    private String mode;

    private OffsetDateTime triggerAt;

    private Integer offsetMinutes;

    private String anchor;

    private Boolean fired;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
