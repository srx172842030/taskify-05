<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { taskApi } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useUsers } from '@/stores/users'
import { STATUS_LABELS, STATUS_ORDER } from '@/services/taskStatus'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Button } from '@/components/ui/button'

const props = defineProps<{ taskIds: string[]; projectId: string }>()
const emit = defineEmits<{ done: [] }>()

const { users } = useUsers()
const { upsert } = useTasks()

const assigneeVal = ref('')
const priorityVal = ref('')
const statusVal = ref('')
const dueDate = ref('')

async function apply(patch: Record<string, unknown>) {
  if (props.taskIds.length === 0) return
  try {
    const updated = await taskApi.batchUpdate(props.projectId, { taskIds: props.taskIds, patch })
    updated.forEach((t) => upsert(t))
    toast.success(`已更新 ${updated.length} 项`)
    emit('done')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '批量编辑失败')
  }
}

function onAssignee(v: unknown) {
  assigneeVal.value = ''
  if (v == null || v === '') return
  apply({ assigneeId: String(v) === 'none' ? null : String(v) })
}
function onPriority(v: unknown) {
  priorityVal.value = ''
  if (v == null || v === '') return
  apply({ priority: String(v) === 'none' ? null : String(v) })
}
function onStatus(v: unknown) {
  statusVal.value = ''
  if (v == null || v === '') return
  apply({ status: String(v) })
}
function applyDueDate() {
  if (!dueDate.value) return
  const iso = new Date(`${dueDate.value}T23:59:59`).toISOString()
  dueDate.value = ''
  apply({ dueAt: iso, dueAllDay: true })
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-2 rounded-lg border bg-muted/30 p-2">
    <span class="px-2 text-sm font-medium">已选 {{ taskIds.length }} 项</span>

    <Select v-model="assigneeVal" @update:model-value="onAssignee">
      <SelectTrigger class="h-9 w-36">
        <SelectValue placeholder="设置负责人" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="none">未分配</SelectItem>
        <SelectItem v-for="u in users" :key="u.id" :value="u.id">{{ u.username }}</SelectItem>
      </SelectContent>
    </Select>

    <Select v-model="priorityVal" @update:model-value="onPriority">
      <SelectTrigger class="h-9 w-32">
        <SelectValue placeholder="设置优先级" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="none">无优先级</SelectItem>
        <SelectItem value="HIGH">高</SelectItem>
        <SelectItem value="MEDIUM">中</SelectItem>
        <SelectItem value="LOW">低</SelectItem>
      </SelectContent>
    </Select>

    <Select v-model="statusVal" @update:model-value="onStatus">
      <SelectTrigger class="h-9 w-32">
        <SelectValue placeholder="移动到" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem v-for="s in STATUS_ORDER" :key="s" :value="s">{{ STATUS_LABELS[s] }}</SelectItem>
      </SelectContent>
    </Select>

    <input v-model="dueDate" type="date" class="h-9 rounded-md border border-input bg-background px-3 text-sm" />
    <Button variant="outline" size="sm" class="h-9" @click="applyDueDate">设截止日</Button>
  </div>
</template>
