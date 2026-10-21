import { reactive } from 'vue'

export interface Toast {
  id: number
  message: string
  type: 'default' | 'success' | 'error'
}

let nextId = 0
const toasts = reactive<Toast[]>([])

export function useToast() {
  function push(message: string, type: Toast['type'] = 'default') {
    const id = ++nextId
    toasts.push({ id, message, type })
    setTimeout(() => dismiss(id), 3000)
  }

  function dismiss(id: number) {
    const idx = toasts.findIndex((t) => t.id === id)
    if (idx >= 0) toasts.splice(idx, 1)
  }

  return { toasts, push, dismiss }
}
