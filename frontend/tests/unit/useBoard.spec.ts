import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({ move: vi.fn(), upsert: vi.fn(), push: vi.fn() }))

vi.mock('@/services/taskApi', () => ({
  taskApi: { move: mocks.move },
}))
vi.mock('@/stores/tasks', () => ({ useTasks: () => ({ upsert: mocks.upsert }) }))
vi.mock('@/composables/useToast', () => ({ useToast: () => ({ push: mocks.push }) }))

import { useBoard } from '@/composables/useBoard'
import type { Task } from '@/services/taskApi'

const task: Task = {
  id: '1',
  projectId: 'p',
  title: '任务',
  description: null,
  assigneeId: null,
  status: 'TODO',
  createdBy: 'u',
  createdAt: '',
  updatedAt: '',
}

describe('useBoard.moveTask', () => {
  beforeEach(() => {
    mocks.move.mockReset()
    mocks.upsert.mockReset()
    mocks.push.mockReset()
  })

  it('移动成功时乐观更新并落定', async () => {
    mocks.move.mockResolvedValue({ ...task, status: 'IN_PROGRESS' })
    const { moveTask } = useBoard()
    const ok = await moveTask(task, 'IN_PROGRESS')
    expect(ok).toBe(true)
    expect(mocks.upsert).toHaveBeenCalledWith({ ...task, status: 'IN_PROGRESS' })
    expect(mocks.upsert).toHaveBeenCalledTimes(2)
  })

  it('移动失败时回滚到原列', async () => {
    mocks.move.mockRejectedValue(new Error('只能移动到相邻列'))
    const { moveTask } = useBoard()
    const ok = await moveTask(task, 'DONE')
    expect(ok).toBe(false)
    expect(mocks.upsert).toHaveBeenCalledWith({ ...task, status: 'DONE' })
    expect(mocks.upsert).toHaveBeenCalledWith({ ...task, status: 'TODO' })
    expect(mocks.push).toHaveBeenCalled()
  })
})
