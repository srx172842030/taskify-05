import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useSwimlane } from '@/composables/useSwimlane'
import type { Task } from '@/services/taskApi'

function task(overrides: Partial<Task> = {}): Task {
  return {
    id: 't1',
    projectId: 'p1',
    title: '任务',
    description: null,
    assigneeId: null,
    status: 'TODO',
    createdBy: 'me',
    priority: null,
    startAt: null,
    startAllDay: false,
    dueAt: null,
    dueAllDay: false,
    blocked: false,
    blockerIds: [],
    createdAt: '',
    updatedAt: '',
    ...overrides,
  }
}

describe('useSwimlane', () => {
  it('按负责人分组，未分配归入「未分配」', () => {
    const tasks = ref([task({ id: 'a', assigneeId: 'x' }), task({ id: 'b', assigneeId: null })])
    const by = ref<'assignee' | 'priority' | null>('assignee')
    const { lanes } = useSwimlane(tasks, by, (id) => (id === 'x' ? '小王' : id))
    expect(lanes.value.map((l) => l.label)).toEqual(['小王', '未分配'])
  })

  it('按优先级分组，未设置归入「无优先级」', () => {
    const tasks = ref([task({ id: 'a', priority: 'HIGH' }), task({ id: 'b', priority: null })])
    const by = ref<'assignee' | 'priority' | null>('priority')
    const { lanes } = useSwimlane(tasks, by, (id) => id)
    expect(lanes.value.map((l) => l.label)).toEqual(['高', '无优先级'])
  })

  it('关闭分组返回单个平铺分组', () => {
    const tasks = ref([task({ id: 'a' }), task({ id: 'b' })])
    const by = ref<'assignee' | 'priority' | null>(null)
    const { lanes } = useSwimlane(tasks, by, (id) => id)
    expect(lanes.value).toHaveLength(1)
    expect(lanes.value[0].tasks).toHaveLength(2)
  })
})
