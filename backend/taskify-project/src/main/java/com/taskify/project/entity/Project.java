package com.taskify.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("taskify_project")
public class Project extends BaseEntity {

    private String name;

    private String description;

    private String createdBy;
}
