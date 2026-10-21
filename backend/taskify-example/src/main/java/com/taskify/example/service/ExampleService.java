package com.taskify.example.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.PageResult;
import com.taskify.example.entity.Example;
import com.taskify.example.mapper.ExampleMapper;
import com.taskify.example.mq.ExampleEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 示例 CRUD：演示资源级授权——默认拒绝，用户仅能访问 owner_id 为自身的记录。
 */
@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleMapper exampleMapper;

    /** 可选注入：RocketMQ 关闭（如测试环境）时为 null。 */
    @Autowired(required = false)
    private ExampleEventProducer eventProducer;

    public Example create(String userId, String name, String description) {
        Example e = new Example();
        e.setName(name);
        e.setDescription(description);
        e.setOwnerId(userId);
        exampleMapper.insert(e);
        if (eventProducer != null) {
            eventProducer.sendCreated(e.getId());
        }
        return e;
    }

    public Example get(String userId, String id) {
        Example e = exampleMapper.selectById(id);
        if (e == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "记录不存在");
        }
        requireOwner(e, userId);
        return e;
    }

    public PageResult<Example> list(String userId, long page, long size) {
        Page<Example> p = exampleMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Example>lambdaQuery().eq(Example::getOwnerId, userId).orderByDesc(Example::getCreatedAt));
        return PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal());
    }

    public Example update(String userId, String id, String name, String description) {
        Example e = get(userId, id);
        e.setName(name);
        e.setDescription(description);
        exampleMapper.updateById(e);
        return e;
    }

    public void delete(String userId, String id) {
        Example e = get(userId, id);
        exampleMapper.deleteById(e.getId());
    }

    private void requireOwner(Example e, String userId) {
        if (!e.getOwnerId().equals(userId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "无权限访问该记录");
        }
    }

    public record DeleteResult(boolean deleted) {
    }
}
