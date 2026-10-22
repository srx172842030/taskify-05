package com.taskify.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

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
}
