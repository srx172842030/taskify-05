import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { mount } from '@vue/test-utils'

const mocks = vi.hoisted(() => ({
  update: vi.fn(),
  upsert: vi.fn(),
  push: vi.fn(),
}))

vi.mock('@/services/taskApi', () => ({
  taskApi: { update: mocks.update },
}))
vi.mock('@/services/commentApi', () => ({
  commentApi: { list: vi.fn().mockResolvedValue({ items: [], page: 1, size: 50, total: 0, totalPages: 0 }), create: vi.fn(), update: vi.fn(), remove: vi.fn() },
}))
vi.mock('@/stores/tasks', () => ({ useTasks: () => ({ upsert: mocks.upsert }) }))
vi.mock('@/stores/users', () => ({ useUsers: () => ({ users: [], byId: {} }) }))
vi.mock('@/stores/auth', () => ({ useAuth: () => ({ user: ref(null) }) }))
vi.mock('@/composables/useBoard', () => ({ useBoard: () => ({ moveTask: vi.fn() }) }))
vi.mock('@/composables/useToast', () => ({ useToast: () => ({ push: mocks.push }) }))

import TaskDetailDrawer from '@/components/task/TaskDetailDrawer.vue'
import type { Task } from '@/services/taskApi'

const task: Task = {
  id: '1',
  projectId: 'p',
  title: '标题',
  description: null,
  assigneeId: null,
  status: 'TODO',
  createdBy: 'u',
  createdAt: '',
  updatedAt: '',
}

describe('TaskDetailDrawer', () => {
  beforeEach(() => {
    mocks.update.mockReset()
    mocks.upsert.mockReset()
    mocks.push.mockReset()
  })

  it('渲染抽屉并拒绝空标题保存', async () => {
    const wrapper = mount(TaskDetailDrawer, { props: { task, modelValue: true } })
    expect(wrapper.text()).toContain('任务详情')

    const titleInput = wrapper.find('input')
    await titleInput.setValue('   ')
    const saveButton = wrapper.findAll('button').find((b) => b.text() === '保存')
    await saveButton!.trigger('click')

    expect(mocks.update).not.toHaveBeenCalled()
    expect(mocks.push).toHaveBeenCalledWith('标题不能为空', 'error')
  })
})
