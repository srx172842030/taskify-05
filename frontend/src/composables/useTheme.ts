import { computed, watch } from 'vue'
import { useColorMode } from '@vueuse/core'

export type ThemeMode = 'light' | 'dark' | 'system'

const colorMode = useColorMode({
  selector: 'html',
  attribute: 'class',
  modes: { light: 'light', dark: 'dark', auto: 'auto' },
  storageKey: 'taskify-theme',
  initialValue: 'auto',
})

export function useTheme() {
  const mode = computed<ThemeMode>({
    get: () => (colorMode.value === 'auto' ? 'system' : (colorMode.value as ThemeMode)),
    set: (v) => {
      colorMode.value = v === 'system' ? 'auto' : v
    },
  })

  const isDark = computed(() => {
    const m = colorMode.value
    return (
      m === 'dark' ||
      (m === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    )
  })

  // 同步切换 .dark class（auto 模式随系统偏好）
  watch(
    isDark,
    (dark) => {
      document.documentElement.classList.toggle('dark', dark)
    },
    { immediate: true, flush: 'sync' },
  )

  function setTheme(m: ThemeMode) {
    mode.value = m
  }

  return { mode, isDark, setTheme }
}
