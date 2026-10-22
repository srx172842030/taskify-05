import { http, type PageResult } from './http'

export interface Notification {
  id: string
  type: 'TASK_CREATED' | 'TASK_UPDATED' | 'KANBAN_MOVED' | 'REMINDER' | 'TASK_BLOCKED' | 'TASK_UNBLOCKED'
  taskId: string | null
  message: string
  read: boolean
  createdAt: string
}

export const notificationApi = {
  list: () => http.get<PageResult<Notification>>('/notifications?page=1&size=50'),
  unreadCount: () => http.get<{ count: number }>('/notifications/unread-count'),
  markRead: (id: string) => http.post<Notification>(`/notifications/${id}/read`),
  markAllRead: () => http.post<{ updated: number }>('/notifications/read-all'),
}
