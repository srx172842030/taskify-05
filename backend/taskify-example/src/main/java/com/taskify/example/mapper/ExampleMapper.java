package com.taskify.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskify.example.entity.Example;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExampleMapper extends BaseMapper<Example> {
}
