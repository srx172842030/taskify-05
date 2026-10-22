<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dependencyApi, type Dependency } from '@/services/dependencyApi'
import type { Task } from '@/services/taskApi'
import { STATUS_LABELS } from '@/services/taskStatus'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ taskId: string; tasks: Task[] }>()
const { push } = useToast()

const deps = ref<Dependency | null>(null)
const blockerId = ref('')

async function load() {
  deps.value = await dependencyApi.list(props.taskId)
}
onMounted(load)

function label(status: string): string {
  return STATUS_LABELS[status as keyof typeof STATUS_LABELS] ?? status
}

function title(id: string): string {
  return props.tasks.find((t) => t.id === id)?.title ?? id
}

async function add() {
  if (!blockerId.value) return
  try {
    await dependencyApi.create(props.taskId, blockerId.value)
    blockerId.value = ''
    await load()
    push('已建立阻塞关系', 'success')
  } catch (e) {
    push(e instanceof Error ? e.message : '建立失败', 'error')
  }
}

async function remove(blockerTaskId: string) {
  try {
    await dependencyApi.remove(props.taskId, blockerTaskId)
    await load()
  } catch (e) {
    push(e instanceof Error ? e.message : '解除失败', 'error')
  }
}
</script>

<template>
  <div class="space-y-3">
    <p class="text-sm font-medium">阻塞关系</p>

    <div v-if="deps?.blockers?.length" class="text-xs text-muted-foreground">被这些任务阻塞：</div>
    <div
      v-for="b in deps?.blockers ?? []"
      :key="b.taskId"
      class="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
    >
      <span>{{ b.title ?? title(b.taskId) }}（{{ label(b.status) }}）</span>
      <button class="text-xs text-muted-foreground hover:text-destructive" @click="remove(b.taskId)">解除</button>
    </div>

    <div v-if="deps?.dependents?.length" class="text-xs text-muted-foreground">阻塞这些任务：</div>
    <div
      v-for="d in deps?.dependents ?? []"
      :key="d.taskId"
      class="rounded-md border px-3 py-2 text-sm"
    >
      {{ d.title ?? title(d.taskId) }}（{{ label(d.status) }}）
    </div>

    <div class="flex gap-2">
      <select
        v-model="blockerId"
        class="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
      >
        <option value="">选择阻塞任务…</option>
        <option v-for="t in props.tasks.filter((x) => x.id !== props.taskId)" :key="t.id" :value="t.id">
          {{ t.title }}
        </option>
      </select>
      <button class="rounded-md bg-primary px-3 py-1.5 text-xs text-primary-foreground" @click="add">添加</button>
    </div>
  </div>
</template>
