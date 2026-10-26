import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  unreadCount: vi.fn(),
  markRead: vi.fn(),
  markAllRead: vi.fn(),
  reload: vi.fn(),
}))

vi.mock('@/services/notificationApi', () => ({
  notificationApi: {
    list: vi.fn().mockResolvedValue({ items: [], page: 1, size: 50, total: 0, totalPages: 0 }),
    unreadCount: mocks.unreadCount,
    markRead: mocks.markRead,
    markAllRead: mocks.markAllRead,
  },
}))
vi.mock('@/stores/tasks', () => ({
  useTasks: () => ({ reload: mocks.reload }),
}))

const sources: { listeners: Record<string, (e: { data: string }) => void> }[] = []
globalThis.EventSource = class {
  listeners: Record<string, (e: { data: string }) => void> = {}
  constructor() {
    sources.push(this)
  }
  addEventListener(type: string, fn: (e: { data: string }) => void) {
    this.listeners[type] = fn
  }
  close() {}
} as unknown as typeof EventSource

import { useNotifications } from '@/stores/notifications'
import type { Notification } from '@/services/notificationApi'

function notif(id: string, read = false, type: Notification['type'] = 'TASK_CREATED'): Notification {
  return { id, type, taskId: 't', message: 'm', read, createdAt: '' }
}

describe('notifications store 未读计数与已读', () => {
  beforeEach(() => {
    mocks.unreadCount.mockReset()
    mocks.markRead.mockReset()
    mocks.markAllRead.mockReset()
    mocks.reload.mockReset()
    sources.length = 0
  })

  it('prepend 增加未读，markRead/markAllRead 递减清零', async () => {
    const s = useNotifications()

    mocks.unreadCount.mockResolvedValue({ count: 0 })
    await s.refreshUnread()
    expect(s.unread.value).toBe(0)

    s.prepend(notif('1'))
    s.prepend(notif('2'))
    expect(s.unread.value).toBe(2)

    mocks.markRead.mockResolvedValue(notif('1', true))
    await s.markRead('1')
    expect(s.unread.value).toBe(1)

    mocks.markAllRead.mockResolvedValue({ updated: 1 })
    await s.markAllRead()
    expect(s.unread.value).toBe(0)
  })

  it.each(['TASK_CREATED', 'TASK_UPDATED', 'KANBAN_MOVED', 'TASK_BLOCKED', 'TASK_UNBLOCKED'])(
    '收到 %s 通知时刷新当前项目任务',
    (type) => {
      localStorage.setItem('taskify-token', 'tok')
      const s = useNotifications()
      s.connectStream()
      expect(sources).toHaveLength(1)
      sources[sources.length - 1].listeners['notification']({ data: JSON.stringify(notif('1', false, type as Notification['type'])) })
      expect(mocks.reload).toHaveBeenCalled()
      s.disconnectStream()
    },
  )

  it('收到 REMINDER 通知时不刷新任务', () => {
    localStorage.setItem('taskify-token', 'tok')
    const s = useNotifications()
    s.connectStream()
    sources[sources.length - 1].listeners['notification']({ data: JSON.stringify(notif('1', false, 'REMINDER')) })
    expect(mocks.reload).not.toHaveBeenCalled()
    s.disconnectStream()
  })
})
