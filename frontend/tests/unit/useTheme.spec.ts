import { describe, it, expect, beforeEach } from 'vitest'
import { useTheme } from '@/composables/useTheme'

describe('useTheme', () => {
  beforeEach(() => {
    document.documentElement.classList.remove('dark')
  })

  it('切换暗色主题会添加 dark class', () => {
    const { setTheme, isDark } = useTheme()
    setTheme('dark')
    expect(isDark.value).toBe(true)
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('切换亮色主题会移除 dark class', () => {
    const { setTheme, isDark } = useTheme()
    setTheme('dark')
    setTheme('light')
    expect(isDark.value).toBe(false)
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
