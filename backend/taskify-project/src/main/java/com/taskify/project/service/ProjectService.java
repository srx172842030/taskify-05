package com.taskify.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.PageResult;
import com.taskify.project.entity.Project;
import com.taskify.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 项目：创建、分页列表、详情。阶段一无成员关系表——所有预置用户可访问全部项目。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public Project create(String userId, String name, String description) {
        Project p = new Project();
        p.setName(name.trim());
        p.setDescription(description);
        p.setCreatedBy(userId);
        projectMapper.insert(p);
        return p;
    }

    public Project get(String id) {
        Project p = projectMapper.selectById(id);
        if (p == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return p;
    }

    public PageResult<Project> list(long page, long size) {
        Page<Project> p = projectMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Project>lambdaQuery().orderByDesc(Project::getCreatedAt));
        return PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal());
    }
}
