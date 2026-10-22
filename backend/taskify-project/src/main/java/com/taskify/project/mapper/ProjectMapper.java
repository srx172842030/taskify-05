package com.taskify.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskify.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
