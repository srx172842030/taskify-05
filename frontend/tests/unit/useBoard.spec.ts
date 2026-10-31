import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({ move: vi.fn(), upsert: vi.fn(), reload: vi.fn(), error: vi.fn() }))

vi.mock('@/services/taskApi', () => ({
  taskApi: { move: mocks.move },
}))
vi.mock('@/stores/tasks', () => ({ useTasks: () => ({ upsert: mocks.upsert, reload: mocks.reload }) }))
vi.mock('vue-sonner', () => ({
  toast: Object.assign(vi.fn(), { error: mocks.error, success: vi.fn() }),
}))

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
  priority: null,
  startAt: null,
  startAllDay: false,
  dueAt: null,
  dueAllDay: false,
  blocked: false,
  blockerIds: [],
  createdAt: '',
  updatedAt: '',
}

describe('useBoard.moveTask', () => {
  beforeEach(() => {
    mocks.move.mockReset()
    mocks.upsert.mockReset()
    mocks.reload.mockReset()
    mocks.error.mockReset()
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
    expect(mocks.error).toHaveBeenCalled()
  })

  it('被阻塞任务不允许向前推进，并触发回弹重算', async () => {
    const blockedTask: Task = { ...task, blocked: true }
    const { moveTask } = useBoard()
    const ok = await moveTask(blockedTask, 'IN_PROGRESS')
    expect(ok).toBe(false)
    expect(mocks.move).not.toHaveBeenCalled()
    expect(mocks.error).toHaveBeenCalled()
    expect(mocks.upsert).toHaveBeenCalledWith({ ...blockedTask })
  })

  it('被阻塞任务允许回退', async () => {
    const blockedInProgress: Task = { ...task, status: 'IN_PROGRESS', blocked: true }
    mocks.move.mockResolvedValue({ ...blockedInProgress, status: 'TODO' })
    const { moveTask } = useBoard()
    const ok = await moveTask(blockedInProgress, 'TODO')
    expect(ok).toBe(true)
    expect(mocks.move).toHaveBeenCalledWith(blockedInProgress.id, 'TODO')
  })

  it('进入/离开已完成时刷新列表', async () => {
    mocks.move.mockResolvedValue({ ...task, status: 'DONE' })
    const { moveTask } = useBoard()
    const ok = await moveTask(task, 'DONE')
    expect(ok).toBe(true)
    expect(mocks.reload).toHaveBeenCalled()
  })
})
