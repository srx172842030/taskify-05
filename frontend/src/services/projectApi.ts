import { http, type PageResult } from './http'

export interface Project {
  id: string
  name: string
  description: string | null
  createdBy: string
  createdAt: string
  updatedAt: string
}

export const projectApi = {
  list: () => http.get<PageResult<Project>>('/projects?page=1&size=100'),
  get: (id: string) => http.get<Project>(`/projects/${id}`),
  create: (d: { name: string; description?: string }) => http.post<Project>('/projects', d),
}
