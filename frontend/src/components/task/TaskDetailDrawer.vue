<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { X } from 'lucide-vue-next'
import type { Task } from '@/services/taskApi'
import { taskApi } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useUsers } from '@/stores/users'
import { useBoard } from '@/composables/useBoard'
import { useToast } from '@/composables/useToast'
import { STATUS_LABELS, adjacentStatuses } from '@/services/taskStatus'
import { toDatetimeLocal, fromDatetimeLocal } from '@/services/taskTime'
import CommentList from './CommentList.vue'
import CommentForm from './CommentForm.vue'
import ReminderEditor from './ReminderEditor.vue'
import DependencyEditor from './DependencyEditor.vue'

const open = defineModel<boolean>({ default: false })
const props = defineProps<{ task: Task | null }>()

const { users } = useUsers()
const { upsert, tasks } = useTasks()
const { moveTask } = useBoard()
const { push } = useToast()

const title = ref('')
const description = ref('')
const assigneeId = ref('')
const startAtInput = ref('')
const startAllDay = ref(false)
const dueAtInput = ref('')
const dueAllDay = ref(false)
const saving = ref(false)
const commentRefresh = ref(0)

const blockerTitles = computed(() =>
  (props.task?.blockerIds ?? [])
    .map((id) => tasks.value.find((t) => t.id === id)?.title)
    .filter((x): x is string => Boolean(x)),
)

watch(
  () => props.task,
  (t) => {
    title.value = t?.title ?? ''
    description.value = t?.description ?? ''
    assigneeId.value = t?.assigneeId ?? ''
    startAtInput.value = toDatetimeLocal(t?.startAt)
    startAllDay.value = t?.startAllDay ?? false
    dueAtInput.value = toDatetimeLocal(t?.dueAt)
    dueAllDay.value = t?.dueAllDay ?? false
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
      startAt: fromDatetimeLocal(startAtInput.value),
      startAllDay: startAllDay.value,
      dueAt: fromDatetimeLocal(dueAtInput.value),
      dueAllDay: dueAllDay.value,
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
          <div v-if="task.blocked" class="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
            ⚠ 此任务被未完成的依赖任务阻塞，无法开始。
            <span v-if="blockerTitles.length">阻塞任务：{{ blockerTitles.join('、') }}</span>
          </div>

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

          <div class="space-y-1">
            <label class="block text-sm font-medium">开始时间</label>
            <div class="flex items-center gap-2">
              <input
                v-model="startAtInput"
                type="datetime-local"
                class="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
              />
              <label class="flex items-center gap-1 text-xs text-muted-foreground">
                <input v-model="startAllDay" type="checkbox" /> 全天
              </label>
            </div>
          </div>

          <div class="space-y-1">
            <label class="block text-sm font-medium">截止时间</label>
            <div class="flex items-center gap-2">
              <input
                v-model="dueAtInput"
                type="datetime-local"
                class="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
              />
              <label class="flex items-center gap-1 text-xs text-muted-foreground">
                <input v-model="dueAllDay" type="checkbox" /> 全天
              </label>
            </div>
          </div>

          <button
            :disabled="saving"
            class="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground disabled:opacity-60"
            @click="save"
          >
            {{ saving ? '保存中…' : '保存' }}
          </button>

          <hr class="border-t" />

          <ReminderEditor :task-id="task.id" />

          <hr class="border-t" />

          <DependencyEditor :task-id="task.id" :tasks="tasks" />

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
