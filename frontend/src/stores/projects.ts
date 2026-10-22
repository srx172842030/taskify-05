import { ref } from 'vue'
import { projectApi, type Project } from '@/services/projectApi'

const projects = ref<Project[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    projects.value = (await projectApi.list()).items
  } finally {
    loading.value = false
  }
}

async function create(name: string, description?: string) {
  const p = await projectApi.create({ name, description })
  projects.value.unshift(p)
  return p
}

export function useProjects() {
  return { projects, loading, load, create }
}
