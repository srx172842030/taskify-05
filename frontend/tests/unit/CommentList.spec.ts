import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { mount } from '@vue/test-utils'

const mocks = vi.hoisted(() => ({
  list: vi.fn(),
}))

vi.mock('@/services/commentApi', () => ({
  commentApi: { list: mocks.list, update: vi.fn(), remove: vi.fn() },
}))
vi.mock('@/stores/auth', () => ({ useAuth: () => ({ user: ref({ id: 'me', username: 'me' }) }) }))
vi.mock('@/stores/users', () => ({
  useUsers: () => ({ byId: { me: '我', other: '他人' } }),
}))
vi.mock('vue-sonner', () => ({
  toast: Object.assign(vi.fn(), { error: vi.fn(), success: vi.fn() }),
}))

import CommentList from '@/components/task/CommentList.vue'

const mine = { id: 'c1', taskId: 't', authorId: 'me', content: '我的评论', createdAt: '', updatedAt: '' }
const others = { id: 'c2', taskId: 't', authorId: 'other', content: '他人评论', createdAt: '', updatedAt: '' }

describe('CommentList 编辑/删除仅作者可见', () => {
  beforeEach(() => {
    mocks.list.mockReset()
  })

  it('仅作者评论显示编辑/删除按钮', async () => {
    mocks.list.mockResolvedValue({ items: [mine, others], page: 1, size: 50, total: 2, totalPages: 1 })
    const wrapper = mount(CommentList, { props: { taskId: 't', refreshKey: 0 } })
    await wrapper.vm.$nextTick()
    await new Promise((r) => setTimeout(r, 0))

    const buttons = wrapper.findAll('button')
    const editButtons = buttons.filter((b) => b.text() === '编辑')
    const deleteButtons = buttons.filter((b) => b.text() === '删除')
    expect(editButtons.length).toBe(1)
    expect(deleteButtons.length).toBe(1)
  })
})
