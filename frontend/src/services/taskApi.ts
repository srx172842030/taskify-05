import { http, type PageResult } from './http'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'

export interface Task {
  id: string
  projectId: string
  title: string
  description: string | null
  assigneeId: string | null
  status: TaskStatus
  createdBy: string
  startAt: string | null
  startAllDay: boolean
  dueAt: string | null
  dueAllDay: boolean
  blocked: boolean
  blockerIds: string[]
  createdAt: string
  updatedAt: string
}

export interface TaskPayload {
  title: string
  description?: string | null
  assigneeId?: string | null
  startAt?: string | null
  startAllDay?: boolean
  dueAt?: string | null
  dueAllDay?: boolean
}

export const taskApi = {
  list: (projectId: string) =>
    http.get<PageResult<Task>>(`/projects/${projectId}/tasks?page=1&size=100`),
  get: (id: string) => http.get<Task>(`/tasks/${id}`),
  create: (projectId: string, d: TaskPayload) => http.post<Task>(`/projects/${projectId}/tasks`, d),
  update: (id: string, d: TaskPayload) => http.patch<Task>(`/tasks/${id}`, d),
  move: (id: string, toStatus: TaskStatus) => http.post<Task>(`/tasks/${id}/move`, { toStatus }),
}
