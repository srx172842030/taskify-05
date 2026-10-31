import { computed, ref, type Ref } from 'vue'
import type { Task, TaskStatus, Priority } from '@/services/taskApi'

/** 视图筛选条件。 */
export interface ViewFilters {
  mineOnly: boolean
  assigneeId: string | null
  status: TaskStatus | null
  priority: Priority | null
}

export type GroupBy = null | 'assignee' | 'priority'
export type SortBy = 'dueAt' | 'createdAt' | 'title' | 'priority'
export type SortDir = 'asc' | 'desc'

export interface TaskGroup {
  key: string
  label: string
  tasks: Task[]
}

export const PRIORITY_LABELS: Record<Priority, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
}

export function priorityLabel(p: Priority | null): string {
  return p ? (PRIORITY_LABELS[p] ?? p) : '无优先级'
}

/** 筛选：先筛掉不满足条件的任务。 */
export function filterTasks(tasks: Task[], filters: ViewFilters, currentUserId: string): Task[] {
  return tasks.filter((t) => {
    if (filters.mineOnly && t.assigneeId !== currentUserId) return false
    if (filters.assigneeId && t.assigneeId !== filters.assigneeId) return false
    if (filters.status && t.status !== filters.status) return false
    if (filters.priority && t.priority !== filters.priority) return false
    return true
  })
}

/** 分组：按负责人或优先级聚合；未分配负责人→「未分配」，未设优先级→「无优先级」。 */
export function groupTasks(
  tasks: Task[],
  groupBy: GroupBy,
  resolveAssignee: (id: string) => string,
): TaskGroup[] {
  if (!groupBy) {
    return [{ key: '__all__', label: '', tasks: [...tasks] }]
  }
  const map = new Map<string, Task[]>()
  const order: string[] = []
  for (const t of tasks) {
    const key = groupBy === 'assignee' ? (t.assigneeId ?? '__none__') : (t.priority ?? '__none__')
    if (!map.has(key)) {
      map.set(key, [])
      order.push(key)
    }
    map.get(key)!.push(t)
  }
  return order.map((key) => ({
    key,
    label: groupLabel(key, groupBy, resolveAssignee),
    tasks: map.get(key)!,
  }))
}

function groupLabel(key: string, groupBy: GroupBy, resolveAssignee: (id: string) => string): string {
  if (key === '__none__') return groupBy === 'assignee' ? '未分配' : '无优先级'
  if (groupBy === 'assignee') return resolveAssignee(key)
  return priorityLabel(key as Priority)
}

/** 组内排序：空截止日始终排末尾。 */
export function sortTasks(tasks: Task[], sortBy: SortBy, sortDir: SortDir): Task[] {
  const dir = sortDir === 'asc' ? 1 : -1
  return [...tasks].sort((a, b) => {
    switch (sortBy) {
      case 'dueAt': {
        const ad = a.dueAt ? Date.parse(a.dueAt) : null
        const bd = b.dueAt ? Date.parse(b.dueAt) : null
        if (ad == null && bd == null) return 0
        if (ad == null) return 1
        if (bd == null) return -1
        return (ad - bd) * dir
      }
      case 'createdAt':
        return (Date.parse(a.createdAt) - Date.parse(b.createdAt)) * dir
      case 'title':
        return a.title.localeCompare(b.title) * dir
      case 'priority': {
        return (priorityOrder(a.priority) - priorityOrder(b.priority)) * dir
      }
      default:
        return 0
    }
  })
}

function priorityOrder(p: Priority | null): number {
  if (p === 'HIGH') return 0
  if (p === 'MEDIUM') return 1
  if (p === 'LOW') return 2
  return 3
}

/**
 * 组合式函数：对共享 tasks 做筛选 → 分组 → 组内排序。
 * 配置状态对外暴露，便于视图级持久化（useViewConfig）与界面控件绑定。
 */
export function useTaskQuery(tasks: Ref<Task[]>, resolveAssignee: (id: string) => string, currentUserId: Ref<string>) {
  const filters = ref<ViewFilters>({ mineOnly: false, assigneeId: null, status: null, priority: null })
  const groupBy = ref<GroupBy>(null)
  const sortBy = ref<SortBy>('dueAt')
  const sortDir = ref<SortDir>('asc')

  const groups = computed<TaskGroup[]>(() => {
    const filtered = filterTasks(tasks.value, filters.value, currentUserId.value)
    const grouped = groupTasks(filtered, groupBy.value, resolveAssignee)
    return grouped.map((g) => ({ ...g, tasks: sortTasks(g.tasks, sortBy.value, sortDir.value) }))
  })

  return { filters, groupBy, sortBy, sortDir, groups }
}
