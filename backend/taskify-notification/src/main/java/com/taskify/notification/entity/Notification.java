package com.taskify.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("taskify_notification")
public class Notification extends BaseEntity {

    private String recipientId;

    private String type;

    private String taskId;

    private String message;

    private Boolean read;
}
