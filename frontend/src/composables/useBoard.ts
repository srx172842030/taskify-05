import { taskApi, type Task, type TaskStatus } from '@/services/taskApi'
import { STATUS_ORDER } from '@/services/taskStatus'
import { useTasks } from '@/stores/tasks'
import { useToast } from '@/composables/useToast'

/** 目标列是否比当前列更靠后（向前推进）。 */
function isAdvancing(from: TaskStatus, to: TaskStatus): boolean {
  return STATUS_ORDER.indexOf(to) > STATUS_ORDER.indexOf(from)
}

/**
 * 看板移动：乐观更新 + 失败回滚。后端权威校验「相邻列」与「硬阻塞」。
 * 被阻塞任务禁止向更靠后的列推进，但允许回退；进入/离开「已完成」后刷新列表以同步依赖任务的阻塞状态。
 */
export function useBoard() {
  const { upsert, reload } = useTasks()
  const { push } = useToast()

  async function moveTask(task: Task, toStatus: TaskStatus): Promise<boolean> {
    if (task.blocked && isAdvancing(task.status, toStatus)) {
      push('任务被未完成的依赖任务阻塞，无法推进', 'error')
      // 触发列重算，回弹被拖拽的任务（VueDraggable 的本地变更会因此被还原）
      upsert({ ...task })
      return false
    }
    const previous = task.status
    if (previous === toStatus) return true
    upsert({ ...task, status: toStatus })
    try {
      const updated = await taskApi.move(task.id, toStatus)
      upsert(updated)
      if (previous === 'DONE' || toStatus === 'DONE') {
        void reload()
      }
      return true
    } catch (e) {
      upsert({ ...task, status: previous })
      push(e instanceof Error ? e.message : '移动失败', 'error')
      return false
    }
  }

  return { moveTask }
}
