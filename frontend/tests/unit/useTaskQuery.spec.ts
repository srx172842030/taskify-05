import { describe, it, expect } from 'vitest'
import {
  filterTasks,
  groupTasks,
  sortTasks,
  priorityLabel,
  type ViewFilters,
  type TaskGroup,
} from '@/composables/useTaskQuery'
import type { Task } from '@/services/taskApi'

const me = 'user-1'

function task(overrides: Partial<Task> = {}): Task {
  return {
    id: 't1',
    projectId: 'p1',
    title: '任务',
    description: null,
    assigneeId: null,
    status: 'TODO',
    createdBy: me,
    priority: null,
    startAt: null,
    startAllDay: false,
    dueAt: null,
    dueAllDay: false,
    blocked: false,
    blockerIds: [],
    createdAt: '2026-10-01T00:00:00Z',
    updatedAt: '2026-10-01T00:00:00Z',
    ...overrides,
  }
}

describe('filterTasks', () => {
  it('“只看我的任务”仅保留负责人为当前用户的任务', () => {
    const tasks = [
      task({ id: 'a', assigneeId: me }),
      task({ id: 'b', assigneeId: 'other' }),
      task({ id: 'c', assigneeId: null }),
    ]
    const f: ViewFilters = { mineOnly: true, assigneeId: null, status: null, priority: null }
    expect(filterTasks(tasks, f, me).map((t) => t.id)).toEqual(['a'])
  })

  it('可按负责人、状态、优先级组合筛选', () => {
    const tasks = [
      task({ id: 'a', assigneeId: 'x', status: 'TODO', priority: 'HIGH' }),
      task({ id: 'b', assigneeId: 'x', status: 'DONE', priority: 'HIGH' }),
      task({ id: 'c', assigneeId: 'y', status: 'TODO', priority: 'HIGH' }),
    ]
    const f: ViewFilters = { mineOnly: false, assigneeId: 'x', status: 'TODO', priority: 'HIGH' }
    expect(filterTasks(tasks, f, me).map((t) => t.id)).toEqual(['a'])
  })
})

describe('groupTasks', () => {
  it('按负责人分组，未分配归入「未分配」', () => {
    const tasks = [task({ id: 'a', assigneeId: 'x' }), task({ id: 'b', assigneeId: null })]
    const g = groupTasks(tasks, 'assignee', (id) => (id === 'x' ? '小王' : id))
    expect(g.map((x) => x.label)).toEqual(['小王', '未分配'])
    expect(g[1].tasks.map((t) => t.id)).toEqual(['b'])
  })

  it('按优先级分组，未设置归入「无优先级」', () => {
    const tasks = [task({ id: 'a', priority: 'HIGH' }), task({ id: 'b', priority: null })]
    const g = groupTasks(tasks, 'priority', (id) => id)
    expect(g.map((x) => x.label)).toEqual(['高', '无优先级'])
  })

  it('不分组时返回单个平铺分组', () => {
    const tasks = [task({ id: 'a' }), task({ id: 'b' })]
    const g: TaskGroup[] = groupTasks(tasks, null, (id) => id)
    expect(g).toHaveLength(1)
    expect(g[0].tasks).toHaveLength(2)
  })
})

describe('sortTasks', () => {
  it('按截止日升序，空截止日排末尾', () => {
    const tasks = [
      task({ id: 'a', dueAt: '2026-10-30T00:00:00Z' }),
      task({ id: 'b', dueAt: null }),
      task({ id: 'c', dueAt: '2026-10-20T00:00:00Z' }),
    ]
    expect(sortTasks(tasks, 'dueAt', 'asc').map((t) => t.id)).toEqual(['c', 'a', 'b'])
  })

  it('按优先级排序，无优先级排末尾', () => {
    const tasks = [
      task({ id: 'a', priority: null }),
      task({ id: 'b', priority: 'HIGH' }),
      task({ id: 'c', priority: 'LOW' }),
    ]
    expect(sortTasks(tasks, 'priority', 'asc').map((t) => t.id)).toEqual(['b', 'c', 'a'])
  })
})

describe('priorityLabel', () => {
  it('返回中文标签或「无优先级」', () => {
    expect(priorityLabel('HIGH')).toBe('高')
    expect(priorityLabel(null)).toBe('无优先级')
  })
})
