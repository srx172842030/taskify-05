package com.taskify.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskify.project.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
