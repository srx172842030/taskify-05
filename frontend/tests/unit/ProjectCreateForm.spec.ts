import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

const mocks = vi.hoisted(() => ({ create: vi.fn(), push: vi.fn() }))

vi.mock('@/stores/projects', () => ({ useProjects: () => ({ create: mocks.create }) }))
vi.mock('@/composables/useToast', () => ({ useToast: () => ({ push: mocks.push }) }))

import ProjectCreateForm from '@/components/project/ProjectCreateForm.vue'

describe('ProjectCreateForm', () => {
  beforeEach(() => {
    mocks.create.mockReset()
    mocks.push.mockReset()
  })

  it('空名称被拒且不创建', async () => {
    const wrapper = mount(ProjectCreateForm, { props: { modelValue: true } })
    await wrapper.find('input').setValue('   ')
    await wrapper.find('form').trigger('submit')
    expect(mocks.create).not.toHaveBeenCalled()
    expect(mocks.push).toHaveBeenCalledWith('名称不能为空', 'error')
  })

  it('合法名称触发创建', async () => {
    mocks.create.mockResolvedValue({ id: '1' })
    const wrapper = mount(ProjectCreateForm, { props: { modelValue: true } })
    await wrapper.find('input').setValue('官网改版')
    await wrapper.find('form').trigger('submit')
    expect(mocks.create).toHaveBeenCalledWith('官网改版', undefined)
  })
})
