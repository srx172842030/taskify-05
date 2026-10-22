import { http } from './http'

export interface UserInfo {
  id: string
  username: string
}

export const userApi = {
  list: () => http.get<UserInfo[]>('/users'),
}
