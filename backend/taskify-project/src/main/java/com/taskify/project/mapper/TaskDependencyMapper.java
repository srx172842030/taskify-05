package com.taskify.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskify.project.entity.TaskDependency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskDependencyMapper extends BaseMapper<TaskDependency> {

    /** 给定阻塞者的全部依赖任务 ID。 */
    @Select("SELECT dependent_id FROM taskify_task_dependency WHERE blocker_id = #{blockerId}")
    List<String> selectDependentIds(@Param("blockerId") String blockerId);

    /** 给定依赖任务的全部阻塞者 ID（不区分状态）。 */
    @Select("SELECT blocker_id FROM taskify_task_dependency WHERE dependent_id = #{dependentId}")
    List<String> selectBlockerIds(@Param("dependentId") String dependentId);

    /** 依赖任务是否存在未完成（status != DONE）的阻塞者。 */
    @Select("SELECT COUNT(*) > 0 FROM taskify_task_dependency d JOIN taskify_task t ON d.blocker_id = t.id "
            + "WHERE d.dependent_id = #{dependentId} AND t.status <> 'DONE'")
    boolean hasIncompleteBlocker(@Param("dependentId") String dependentId);

    /** 依赖任务的全部未完成阻塞者 ID（用于警示列表）。 */
    @Select("SELECT d.blocker_id FROM taskify_task_dependency d JOIN taskify_task t ON d.blocker_id = t.id "
            + "WHERE d.dependent_id = #{dependentId} AND t.status <> 'DONE'")
    List<String> selectIncompleteBlockerIds(@Param("dependentId") String dependentId);
}
