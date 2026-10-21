package com.taskify.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskify.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
