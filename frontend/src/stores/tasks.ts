import { ref } from 'vue'
import { taskApi, type Task } from '@/services/taskApi'

const tasks = ref<Task[]>([])
const loading = ref(false)
const currentProjectId = ref<string | null>(null)

async function load(projectId: string) {
  currentProjectId.value = projectId
  loading.value = true
  try {
    tasks.value = (await taskApi.list(projectId)).items
  } finally {
    loading.value = false
  }
}

/** 重新加载当前项目任务（用于阻塞状态变化后的实时刷新）。 */
async function reload() {
  if (currentProjectId.value) await load(currentProjectId.value)
}

function upsert(task: Task) {
  const i = tasks.value.findIndex((t) => t.id === task.id)
  if (i >= 0) tasks.value[i] = task
  else tasks.value.unshift(task)
}

function reset() {
  tasks.value = []
  currentProjectId.value = null
}

export function useTasks() {
  return { tasks, loading, load, reload, upsert, reset }
}
