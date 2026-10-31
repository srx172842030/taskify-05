import { computed, type Ref } from 'vue'

const STORAGE_PREFIX = 'taskify:view:'

/**
 * 视图配置持久化（个人级、按项目、按视图），存于浏览器 localStorage。
 * 键形如 `taskify:view:{projectId}:{view}`。读写失败静默降级（不阻塞界面）。
 */
export function useViewConfig(projectId: Readonly<Ref<string>>, view: 'board' | 'table') {
  const key = computed(() => `${STORAGE_PREFIX}${projectId.value}:${view}`)

  function load<T>(): T | null {
    try {
      const raw = localStorage.getItem(key.value)
      return raw ? (JSON.parse(raw) as T) : null
    } catch {
      return null
    }
  }

  function save(value: unknown) {
    try {
      localStorage.setItem(key.value, JSON.stringify(value))
    } catch {
      // 存储不可用（隐私模式等）时忽略
    }
  }

  function clear() {
    try {
      localStorage.removeItem(key.value)
    } catch {
      // 忽略
    }
  }

  return { load, save, clear }
}
