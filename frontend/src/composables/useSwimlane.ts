import { computed, type Ref } from 'vue'
import type { Task } from '@/services/taskApi'
import { groupTasks, type GroupBy, type TaskGroup } from './useTaskQuery'

/** 看板泳道分组维度：关闭 / 按负责人 / 按优先级。 */
export type SwimlaneBy = GroupBy

/**
 * 看板泳道分组：对给定任务按负责人或优先级分组（复用 useTaskQuery 的分组纯函数）。
 * 关闭（null）时返回单个平铺分组。
 */
export function useSwimlane(
  tasks: Ref<Task[]>,
  by: Ref<SwimlaneBy>,
  resolveAssignee: (id: string) => string,
) {
  const lanes = computed<TaskGroup[]>(() => groupTasks(tasks.value, by.value, resolveAssignee))
  return { lanes }
}
