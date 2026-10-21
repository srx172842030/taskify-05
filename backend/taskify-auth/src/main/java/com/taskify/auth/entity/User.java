package com.taskify.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.taskify.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("taskify_user")
public class User extends BaseEntity {

    private String username;

    private String passwordHash;
}
