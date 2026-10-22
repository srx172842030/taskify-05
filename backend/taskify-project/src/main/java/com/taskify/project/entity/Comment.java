package com.taskify.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("taskify_comment")
public class Comment extends BaseEntity {

    private String taskId;

    private String authorId;

    private String content;
}
