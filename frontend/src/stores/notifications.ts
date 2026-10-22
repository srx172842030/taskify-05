import { ref } from 'vue'
import { notificationApi, type Notification } from '@/services/notificationApi'

const notifications = ref<Notification[]>([])
const unread = ref(0)

const TOKEN_KEY = 'taskify-token'
let source: EventSource | null = null

async function load() {
  notifications.value = (await notificationApi.list()).items
}

async function refreshUnread() {
  unread.value = (await notificationApi.unreadCount()).count
}

async function markRead(id: string) {
  await notificationApi.markRead(id)
  const n = notifications.value.find((x) => x.id === id)
  if (n && !n.read) {
    n.read = true
    unread.value = Math.max(0, unread.value - 1)
  }
}

async function markAllRead() {
  await notificationApi.markAllRead()
  notifications.value.forEach((n) => (n.read = true))
  unread.value = 0
}

function prepend(n: Notification) {
  notifications.value.unshift(n)
  unread.value += 1
}

function connectStream() {
  if (source) return
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return
  source = new EventSource(`/api/v1/notifications/stream?token=${encodeURIComponent(token)}`)
  source.addEventListener('notification', (e: MessageEvent) => {
    try {
      prepend(JSON.parse(e.data) as Notification)
    } catch {
      // 忽略无法解析的事件
    }
  })
}

function disconnectStream() {
  source?.close()
  source = null
}

export function useNotifications() {
  return {
    notifications,
    unread,
    load,
    refreshUnread,
    markRead,
    markAllRead,
    prepend,
    connectStream,
    disconnectStream,
  }
}
