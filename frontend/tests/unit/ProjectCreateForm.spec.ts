import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const mocks = vi.hoisted(() => ({ create: vi.fn(), error: vi.fn() }))

vi.mock('@/stores/projects', () => ({ useProjects: () => ({ create: mocks.create }) }))
vi.mock('vue-sonner', () => ({
  toast: Object.assign(vi.fn(), { error: mocks.error, success: vi.fn() }),
}))

import ProjectCreateForm from '@/components/project/ProjectCreateForm.vue'

function mountOpen() {
  return mount(ProjectCreateForm, {
    props: { modelValue: true },
    global: { stubs: { Teleport: { template: '<div><slot /></div>' } } },
  })
}

describe('ProjectCreateForm', () => {
  beforeEach(() => {
    mocks.create.mockReset()
    mocks.error.mockReset()
  })

  it('空名称被拒且不创建', async () => {
    const wrapper = mountOpen()
    await flushPromises()
    await wrapper.find('input').setValue('   ')
    await wrapper.find('form').trigger('submit')
    expect(mocks.create).not.toHaveBeenCalled()
    expect(mocks.error).toHaveBeenCalledWith('名称不能为空')
  })

  it('合法名称触发创建', async () => {
    mocks.create.mockResolvedValue({ id: '1' })
    const wrapper = mountOpen()
    await flushPromises()
    await wrapper.find('input').setValue('官网改版')
    await wrapper.find('form').trigger('submit')
    expect(mocks.create).toHaveBeenCalledWith('官网改版', undefined)
  })
})
