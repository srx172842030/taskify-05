package com.taskify.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("taskify_task")
public class Task extends BaseEntity {

    private String projectId;

    private String title;

    private String description;

    private String assigneeId;

    private String status;

    private String createdBy;

    private OffsetDateTime startAt;

    private Boolean startAllDay;

    private OffsetDateTime dueAt;

    private Boolean dueAllDay;
}
