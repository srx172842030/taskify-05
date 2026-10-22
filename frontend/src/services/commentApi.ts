import { http, type PageResult } from './http'

export interface Comment {
  id: string
  taskId: string
  authorId: string
  content: string
  createdAt: string
  updatedAt: string
}

export const commentApi = {
  list: (taskId: string) => http.get<PageResult<Comment>>(`/tasks/${taskId}/comments?page=1&size=50`),
  create: (taskId: string, content: string) => http.post<Comment>(`/tasks/${taskId}/comments`, { content }),
  update: (id: string, content: string) => http.patch<Comment>(`/comments/${id}`, { content }),
  remove: (id: string) => http.delete<{ deleted: boolean }>(`/comments/${id}`),
}
