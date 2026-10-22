import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({ unreadCount: vi.fn(), markRead: vi.fn(), markAllRead: vi.fn() }))

vi.mock('@/services/notificationApi', () => ({
  notificationApi: {
    list: vi.fn().mockResolvedValue({ items: [], page: 1, size: 50, total: 0, totalPages: 0 }),
    unreadCount: mocks.unreadCount,
    markRead: mocks.markRead,
    markAllRead: mocks.markAllRead,
  },
}))

import { useNotifications } from '@/stores/notifications'
import type { Notification } from '@/services/notificationApi'

function notif(id: string, read = false): Notification {
  return { id, type: 'TASK_CREATED', taskId: 't', message: 'm', read, createdAt: '' }
}

describe('notifications store 未读计数与已读', () => {
  beforeEach(() => {
    mocks.unreadCount.mockReset()
    mocks.markRead.mockReset()
    mocks.markAllRead.mockReset()
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
})
