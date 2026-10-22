<script setup lang="ts">
import { ref, watch } from 'vue'
import { X } from 'lucide-vue-next'
import type { Task } from '@/services/taskApi'
import { taskApi } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useUsers } from '@/stores/users'
import { useBoard } from '@/composables/useBoard'
import { useToast } from '@/composables/useToast'
import { STATUS_LABELS, adjacentStatuses } from '@/services/taskStatus'
import CommentList from './CommentList.vue'
import CommentForm from './CommentForm.vue'

const open = defineModel<boolean>({ default: false })
const props = defineProps<{ task: Task | null }>()

const { users } = useUsers()
const { upsert } = useTasks()
const { moveTask } = useBoard()
const { push } = useToast()

const title = ref('')
const description = ref('')
const assigneeId = ref('')
const saving = ref(false)
const commentRefresh = ref(0)

watch(
  () => props.task,
  (t) => {
    title.value = t?.title ?? ''
    description.value = t?.description ?? ''
    assigneeId.value = t?.assigneeId ?? ''
  },
  { immediate: true },
)

async function save() {
  if (!props.task) return
  if (!title.value.trim()) {
    push('标题不能为空', 'error')
    return
  }
  saving.value = true
  try {
    const updated = await taskApi.update(props.task.id, {
      title: title.value.trim(),
      description: description.value || null,
      assigneeId: assigneeId.value || null,
    })
    upsert(updated)
    push('已保存', 'success')
  } catch (e) {
    push(e instanceof Error ? e.message : '保存失败', 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Transition name="fade">
    <div v-if="open && task" class="fixed inset-0 z-40 bg-black/40" @click.self="open = false">
      <aside class="ml-auto flex h-full w-full max-w-md flex-col bg-card shadow-lg">
        <header class="flex items-center justify-between border-b px-4 py-3">
          <h2 class="font-semibold">任务详情</h2>
          <button aria-label="关闭" @click="open = false">
            <X class="h-5 w-5" />
          </button>
        </header>

        <div class="flex-1 space-y-5 overflow-y-auto p-4">
          <div>
            <p class="text-xs text-muted-foreground">当前列：{{ STATUS_LABELS[task.status] }}</p>
            <div class="mt-2 flex flex-wrap gap-2">
              <button
                v-for="s in adjacentStatuses(task.status)"
                :key="s"
                class="rounded-md border px-3 py-1.5 text-sm hover:bg-muted"
                @click="moveTask(task, s)"
              >
                移动到「{{ STATUS_LABELS[s] }}」
              </button>
            </div>
          </div>

          <div class="space-y-1">
            <label class="block text-sm font-medium">标题</label>
            <input
              v-model="title"
              class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div class="space-y-1">
            <label class="block text-sm font-medium">描述</label>
            <textarea
              v-model="description"
              rows="3"
              class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div class="space-y-1">
            <label class="block text-sm font-medium">负责人</label>
            <select
              v-model="assigneeId"
              class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              <option value="">未分配</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.username }}</option>
            </select>
          </div>

          <button
            :disabled="saving"
            class="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground disabled:opacity-60"
            @click="save"
          >
            {{ saving ? '保存中…' : '保存' }}
          </button>

          <hr class="border-t" />

          <CommentList :task-id="task.id" :refresh-key="commentRefresh" />
          <CommentForm :task-id="task.id" @added="commentRefresh++" />
        </div>
      </aside>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
