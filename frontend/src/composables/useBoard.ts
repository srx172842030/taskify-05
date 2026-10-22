import { taskApi, type Task, type TaskStatus } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useToast } from '@/composables/useToast'

/**
 * 看板移动：乐观更新 + 失败回滚。后端权威校验「相邻列」，跨列移动会被拒绝。
 */
export function useBoard() {
  const { upsert } = useTasks()
  const { push } = useToast()

  async function moveTask(task: Task, toStatus: TaskStatus): Promise<boolean> {
    const previous = task.status
    if (previous === toStatus) return true
    upsert({ ...task, status: toStatus })
    try {
      const updated = await taskApi.move(task.id, toStatus)
      upsert(updated)
      return true
    } catch (e) {
      upsert({ ...task, status: previous })
      push(e instanceof Error ? e.message : '移动失败', 'error')
      return false
    }
  }

  return { moveTask }
}
