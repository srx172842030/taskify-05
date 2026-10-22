package com.taskify.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import com.taskify.common.response.PageResult;
import com.taskify.project.entity.Comment;
import com.taskify.project.mapper.CommentMapper;
import com.taskify.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 评论：发表、按时间升序列表、编辑/删除（仅作者本人）。
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final TaskMapper taskMapper;

    public Comment create(String userId, String taskId, String content) {
        if (taskMapper.selectById(taskId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        }
        Comment c = new Comment();
        c.setTaskId(taskId);
        c.setAuthorId(userId);
        c.setContent(content.trim());
        commentMapper.insert(c);
        return c;
    }

    public PageResult<Comment> list(String taskId, long page, long size) {
        Page<Comment> p = commentMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Comment>lambdaQuery().eq(Comment::getTaskId, taskId).orderByAsc(Comment::getCreatedAt));
        return PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal());
    }

    public Comment update(String userId, String commentId, String content) {
        Comment c = requireAuthor(commentId, userId);
        c.setContent(content.trim());
        commentMapper.updateById(c);
        return c;
    }

    public void delete(String userId, String commentId) {
        Comment c = requireAuthor(commentId, userId);
        commentMapper.deleteById(c.getId());
    }

    private Comment requireAuthor(String commentId, String userId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "评论不存在");
        }
        if (!c.getAuthorId().equals(userId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "仅作者可编辑或删除该评论");
        }
        return c;
    }

    public record DeleteResult(boolean deleted) {
    }
}
