<script setup lang="ts">
import { computed } from 'vue'
import type { Task } from '@/services/taskApi'
import { formatTaskTime } from '@/services/taskTime'
import { useUsers } from '@/stores/users'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ click: [] }>()

const { byId } = useUsers()
const assigneeName = computed(() => (props.task.assigneeId ? byId.value[props.task.assigneeId] : undefined))
</script>

<template>
  <button
    class="cursor-grab rounded-md border bg-card p-3 text-left shadow-sm hover:border-primary active:cursor-grabbing"
    @click="emit('click')"
  >
    <div class="flex items-center justify-between gap-2">
      <p class="text-sm font-medium">{{ task.title }}</p>
      <span v-if="task.blocked" class="rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-700">被阻塞</span>
    </div>
    <p v-if="assigneeName" class="mt-1 text-xs text-muted-foreground">负责人：{{ assigneeName }}</p>
    <p v-if="task.startAt" class="mt-1 text-xs text-muted-foreground">
      开始：{{ formatTaskTime(task.startAt, task.startAllDay) }}
    </p>
    <p v-if="task.dueAt" class="mt-1 text-xs text-muted-foreground">
      截止：{{ formatTaskTime(task.dueAt, task.dueAllDay) }}
    </p>
  </button>
</template>
