import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

const mocks = vi.hoisted(() => ({
  batchUpdate: vi.fn(),
  upsert: vi.fn(),
}))

vi.mock('@/services/taskApi', () => ({
  taskApi: { batchUpdate: mocks.batchUpdate },
}))
vi.mock('@/stores/tasks', () => ({
  useTasks: () => ({ upsert: mocks.upsert }),
}))
vi.mock('@/stores/users', () => ({
  useUsers: () => ({ users: [{ id: 'u1', username: '小王' }] }),
}))
vi.mock('vue-sonner', () => ({
  toast: Object.assign(vi.fn(), { error: vi.fn(), success: vi.fn() }),
}))

import BatchEditBar from '@/components/table/BatchEditBar.vue'

describe('BatchEditBar', () => {
  beforeEach(() => {
    mocks.batchUpdate.mockReset()
    mocks.upsert.mockReset()
  })

  it('显示已选数量', () => {
    const wrapper = mount(BatchEditBar, { props: { taskIds: ['t1', 't2'], projectId: 'p1' } })
    expect(wrapper.text()).toContain('已选 2 项')
  })

  it('设置截止日后调用批量接口并 upsert 结果', async () => {
    mocks.batchUpdate.mockResolvedValue([{ id: 't1' }])
    const wrapper = mount(BatchEditBar, { props: { taskIds: ['t1'], projectId: 'p1' } })

    await wrapper.find('input[type=date]').setValue('2026-10-30')
    const btn = wrapper.findAll('button').find((b) => b.text() === '设截止日')
    await btn!.trigger('click')
    await new Promise((r) => setTimeout(r, 0))

    expect(mocks.batchUpdate).toHaveBeenCalledWith('p1', {
      taskIds: ['t1'],
      patch: { dueAt: expect.any(String), dueAllDay: true },
    })
    expect(mocks.upsert).toHaveBeenCalledWith({ id: 't1' })
  })
})
