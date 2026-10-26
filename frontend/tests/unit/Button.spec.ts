import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { Button } from '@/components/ui/button'

describe('Button', () => {
  it('渲染默认插槽内容', () => {
    const wrapper = mount(Button, { slots: { default: '点击' } })
    expect(wrapper.text()).toContain('点击')
  })

  it('根据 variant 应用样式类', () => {
    const wrapper = mount(Button, { props: { variant: 'destructive' } })
    expect(wrapper.classes()).toContain('bg-destructive')
  })
})
