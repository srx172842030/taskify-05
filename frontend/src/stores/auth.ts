import { computed, ref } from 'vue'
import { authApi, type Credentials, type UserInfo } from '@/services/auth'

const TOKEN_KEY = 'taskify-token'
const USER_KEY = 'taskify-user'

const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
const user = ref<UserInfo | null>(safeParse(localStorage.getItem(USER_KEY)))

function safeParse(raw: string | null): UserInfo | null {
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

function setSession(t: string, u: UserInfo) {
  token.value = t
  user.value = u
  localStorage.setItem(TOKEN_KEY, t)
  localStorage.setItem(USER_KEY, JSON.stringify(u))
}

function clearSession() {
  token.value = null
  user.value = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function useAuth() {
  const isAuthenticated = computed(() => !!token.value)

  async function login(d: Credentials) {
    const r = await authApi.login(d)
    setSession(r.token, r.user)
  }

  async function register(d: Credentials) {
    const r = await authApi.register(d)
    setSession(r.token, r.user)
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }

  return { token, user, isAuthenticated, login, register, logout }
}
