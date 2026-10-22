import { http } from './http'

export interface DependencyTaskRef {
  taskId: string
  title: string | null
  status: string
}

export interface Dependency {
  blockers: DependencyTaskRef[]
  dependents: DependencyTaskRef[]
}

export const dependencyApi = {
  list: (taskId: string) => http.get<Dependency>(`/tasks/${taskId}/dependencies`),
  create: (dependentId: string, blockerTaskId: string) =>
    http.post<{ created: boolean }>(`/tasks/${dependentId}/dependencies`, { blockerTaskId }),
  remove: (dependentId: string, blockerTaskId: string) =>
    http.delete<{ deleted: boolean }>(`/tasks/${dependentId}/dependencies/${blockerTaskId}`),
}
