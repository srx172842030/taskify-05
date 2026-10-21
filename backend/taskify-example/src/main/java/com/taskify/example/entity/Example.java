package com.taskify.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("taskify_example")
public class Example extends BaseEntity {

    private String name;

    private String description;

    private String ownerId;
}
