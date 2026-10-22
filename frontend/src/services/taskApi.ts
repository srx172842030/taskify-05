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
  createdAt: string
  updatedAt: string
}

export const taskApi = {
  list: (projectId: string) =>
    http.get<PageResult<Task>>(`/projects/${projectId}/tasks?page=1&size=100`),
  get: (id: string) => http.get<Task>(`/tasks/${id}`),
  create: (projectId: string, d: { title: string; description?: string; assigneeId?: string | null }) =>
    http.post<Task>(`/projects/${projectId}/tasks`, d),
  update: (id: string, d: { title: string; description?: string | null; assigneeId?: string | null }) =>
    http.patch<Task>(`/tasks/${id}`, d),
  move: (id: string, toStatus: TaskStatus) => http.post<Task>(`/tasks/${id}/move`, { toStatus }),
}
