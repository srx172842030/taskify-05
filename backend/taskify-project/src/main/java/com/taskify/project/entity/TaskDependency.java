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
 * 阻塞关系：blocker（被依赖任务/阻塞者）阻塞 dependent（依赖任务/被阻塞者）。
 */
@Getter
@Setter
@TableName("taskify_task_dependency")
public class TaskDependency {

    @TableId(type = IdType.INPUT)
    private String id;

    private String blockerId;

    private String dependentId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
