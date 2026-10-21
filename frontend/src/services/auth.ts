import { http } from './http'

export interface UserInfo {
  id: string
  username: string
}

export interface AuthResult {
  token: string
  user: UserInfo
}

export interface CaptchaResult {
  captchaId: string
  image: string
}

export interface Credentials {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}

export const authApi = {
  captcha: () => http.post<CaptchaResult>('/auth/captcha'),
  register: (d: Credentials) => http.post<AuthResult>('/auth/register', d),
  login: (d: Credentials) => http.post<AuthResult>('/auth/login', d),
  logout: () => http.post<{ loggedOut: boolean }>('/auth/logout'),
}
