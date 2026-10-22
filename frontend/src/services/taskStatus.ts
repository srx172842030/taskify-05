import type { TaskStatus } from './taskApi'

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: '待办事项',
  IN_PROGRESS: '进行中',
  IN_REVIEW: '审查中',
  DONE: '已完成',
}

export const STATUS_ORDER: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']

/** 返回给定列的相邻列（前进或后退一步）。 */
export function adjacentStatuses(status: TaskStatus): TaskStatus[] {
  const i = STATUS_ORDER.indexOf(status)
  const result: TaskStatus[] = []
  if (i > 0) result.push(STATUS_ORDER[i - 1]!)
  if (i >= 0 && i < STATUS_ORDER.length - 1) result.push(STATUS_ORDER[i + 1]!)
  return result
}
