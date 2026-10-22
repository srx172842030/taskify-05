<script setup lang="ts">
import { STATUS_ORDER } from '@/services/taskStatus'
import type { Task } from '@/services/taskApi'
import KanbanColumn from './KanbanColumn.vue'

defineProps<{ tasks: Task[] }>()
const emit = defineEmits<{ open: [task: Task] }>()
</script>

<template>
  <div class="hidden grid-cols-4 gap-4 md:grid">
    <KanbanColumn
      v-for="status in STATUS_ORDER"
      :key="status"
      :status="status"
      :tasks="tasks"
      @open="emit('open', $event)"
    />
  </div>
</template>
