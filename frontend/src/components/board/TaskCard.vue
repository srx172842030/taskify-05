<script setup lang="ts">
import { computed } from 'vue'
import type { Task } from '@/services/taskApi'
import { formatTaskTime } from '@/services/taskTime'
import { useUsers } from '@/stores/users'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{ click: [] }>()

const { byId } = useUsers()
const assigneeName = computed(() => (props.task.assigneeId ? byId.value[props.task.assigneeId] : undefined))
</script>

<template>
  <Card
    class="cursor-grab select-none p-3 transition-colors hover:border-primary active:cursor-grabbing"
    role="button"
    tabindex="0"
    @click="emit('click')"
    @keydown.enter.prevent="emit('click')"
    @keydown.space.prevent="emit('click')"
  >
    <div class="flex items-center justify-between gap-2">
      <p class="text-sm font-medium">{{ task.title }}</p>
      <Badge v-if="task.blocked" variant="secondary" class="shrink-0 bg-amber-100 text-amber-700">
        被阻塞
      </Badge>
    </div>
    <p v-if="assigneeName" class="mt-1 text-xs text-muted-foreground">负责人：{{ assigneeName }}</p>
    <p v-if="task.startAt" class="mt-1 text-xs text-muted-foreground">
      开始：{{ formatTaskTime(task.startAt, task.startAllDay) }}
    </p>
    <p v-if="task.dueAt" class="mt-1 text-xs text-muted-foreground">
      截止：{{ formatTaskTime(task.dueAt, task.dueAllDay) }}
    </p>
  </Card>
</template>
