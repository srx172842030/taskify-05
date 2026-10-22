export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
  ) {
    super(message)
  }
}

const TOKEN_KEY = 'taskify-token'

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem(TOKEN_KEY)
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`/api/v1${path}`, { ...init, headers })
  const body = (await res.json().catch(() => null)) as ApiEnvelope<T> | { code?: string; detail?: string; status?: number } | null

  const code = body?.code ?? (res.ok ? 'SUCCESS' : 'SERVER_ERROR')
  if (!res.ok || code === 'UNAUTHENTICATED' || code === 'FORBIDDEN' || code === 'SERVER_ERROR') {
    const detail = 'detail' in (body ?? {}) ? (body as { detail?: string }).detail : '请求失败'
    throw new ApiError(code, detail ?? '请求失败', (body as { status?: number })?.status ?? res.status)
  }

  return (body as ApiEnvelope<T>).data
}

export const http = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: 'POST', body: data === undefined ? undefined : JSON.stringify(data) }),
  put: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: 'PUT', body: data === undefined ? undefined : JSON.stringify(data) }),
  patch: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: 'PATCH', body: data === undefined ? undefined : JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
