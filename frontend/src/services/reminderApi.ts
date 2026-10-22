import { http } from './http'

export interface Reminder {
  id: string
  taskId: string
  mode: 'ABSOLUTE' | 'RELATIVE'
  triggerAt: string | null
  offsetMinutes: number | null
  anchor: 'START' | 'DUE' | null
  fired: boolean
  createdAt: string
}

export type ReminderInput =
  | { mode: 'ABSOLUTE'; triggerAt: string }
  | { mode: 'RELATIVE'; offsetMinutes: number; anchor: 'START' | 'DUE' }

export const reminderApi = {
  list: (taskId: string) => http.get<Reminder[]>(`/tasks/${taskId}/reminders`),
  create: (taskId: string, d: ReminderInput) => http.post<Reminder>(`/tasks/${taskId}/reminders`, d),
  update: (id: string, d: ReminderInput) => http.patch<Reminder>(`/reminders/${id}`, d),
  delete: (id: string) => http.delete<{ deleted: boolean }>(`/reminders/${id}`),
}
