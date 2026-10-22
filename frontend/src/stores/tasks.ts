import { ref } from 'vue'
import { taskApi, type Task } from '@/services/taskApi'

const tasks = ref<Task[]>([])
const loading = ref(false)

async function load(projectId: string) {
  loading.value = true
  try {
    tasks.value = (await taskApi.list(projectId)).items
  } finally {
    loading.value = false
  }
}

function upsert(task: Task) {
  const i = tasks.value.findIndex((t) => t.id === task.id)
  if (i >= 0) tasks.value[i] = task
  else tasks.value.unshift(task)
}

function reset() {
  tasks.value = []
}

export function useTasks() {
  return { tasks, loading, load, upsert, reset }
}
