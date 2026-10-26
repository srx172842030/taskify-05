<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import type { Task, TaskStatus } from '@/services/taskApi'
import { taskApi } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useUsers } from '@/stores/users'
import { useBoard } from '@/composables/useBoard'
import { STATUS_LABELS, adjacentStatuses } from '@/services/taskStatus'
import { toDatetimeLocal, fromDatetimeLocal } from '@/services/taskTime'
import CommentList from './CommentList.vue'
import CommentForm from './CommentForm.vue'
import ReminderEditor from './ReminderEditor.vue'
import DependencyEditor from './DependencyEditor.vue'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'

const open = defineModel<boolean>({ default: false })
const props = defineProps<{ task: Task | null }>()

const { users } = useUsers()
const { upsert, tasks } = useTasks()
const { moveTask } = useBoard()

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
    toast.error('标题不能为空')
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
    toast.success('已保存')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onMove(s: TaskStatus) {
  if (!props.task) return
  const ok = await moveTask(props.task, s)
  if (ok) open.value = false
}
</script>

<template>
  <Sheet v-if="task" v-model:open="open">
    <SheetContent side="right" class="flex w-full flex-col gap-0 p-0 sm:max-w-md">
      <SheetHeader class="border-b px-4 py-3">
        <SheetTitle>任务详情</SheetTitle>
        <SheetDescription class="sr-only">查看与编辑任务详情</SheetDescription>
      </SheetHeader>

      <div class="flex-1 space-y-5 overflow-y-auto p-4">
        <div
          v-if="task.blocked"
          class="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800"
        >
          ⚠ 此任务被未完成的依赖任务阻塞，无法开始。
          <span v-if="blockerTitles.length">阻塞任务：{{ blockerTitles.join('、') }}</span>
        </div>

        <div>
          <p class="text-xs text-muted-foreground">当前列：{{ STATUS_LABELS[task.status] }}</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <Button
              v-for="s in adjacentStatuses(task.status)"
              :key="s"
              variant="outline"
              size="sm"
              @click="onMove(s)"
            >
              移动到「{{ STATUS_LABELS[s] }}」
            </Button>
          </div>
        </div>

        <div class="space-y-1">
          <Label for="task-title">标题</Label>
          <Input id="task-title" v-model="title" />
        </div>

        <div class="space-y-1">
          <Label for="task-description">描述</Label>
          <Textarea id="task-description" v-model="description" rows="3" />
        </div>

        <div class="space-y-1">
          <Label>负责人</Label>
          <Select v-model="assigneeId">
            <SelectTrigger>
              <SelectValue placeholder="未分配" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="u in users" :key="u.id" :value="u.id">{{ u.username }}</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-1">
          <Label for="task-start">开始时间</Label>
          <div class="flex items-center gap-2">
            <Input id="task-start" v-model="startAtInput" type="datetime-local" class="flex-1" />
            <label class="flex items-center gap-2 text-xs text-muted-foreground">
              <Checkbox v-model="startAllDay" /> 全天
            </label>
          </div>
        </div>

        <div class="space-y-1">
          <Label for="task-due">截止时间</Label>
          <div class="flex items-center gap-2">
            <Input id="task-due" v-model="dueAtInput" type="datetime-local" class="flex-1" />
            <label class="flex items-center gap-2 text-xs text-muted-foreground">
              <Checkbox v-model="dueAllDay" /> 全天
            </label>
          </div>
        </div>

        <Button :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </Button>

        <hr class="border-t" />

        <ReminderEditor :task-id="task.id" />

        <hr class="border-t" />

        <DependencyEditor :task-id="task.id" :tasks="tasks" />

        <hr class="border-t" />

        <CommentList :task-id="task.id" :refresh-key="commentRefresh" />
        <CommentForm :task-id="task.id" @added="commentRefresh++" />
      </div>
    </SheetContent>
  </Sheet>
</template>
