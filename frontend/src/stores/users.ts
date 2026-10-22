import { computed, ref } from 'vue'
import { userApi, type UserInfo } from '@/services/userApi'

const users = ref<UserInfo[]>([])

async function load() {
  users.value = await userApi.list()
}

export function useUsers() {
  const byId = computed<Record<string, string>>(() =>
    Object.fromEntries(users.value.map((u) => [u.id, u.username])),
  )
  return { users, byId, load }
}
