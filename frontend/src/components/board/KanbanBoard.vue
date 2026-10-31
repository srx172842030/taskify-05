<script setup lang="ts">
import { STATUS_ORDER } from '@/services/taskStatus'
import type { Task } from '@/services/taskApi'
import type { SwimlaneBy } from '@/composables/useSwimlane'
import type { SortBy, SortDir } from '@/composables/useTaskQuery'
import KanbanColumn from './KanbanColumn.vue'

defineProps<{ tasks: Task[]; swimlaneBy: SwimlaneBy; sortBy: SortBy; sortDir: SortDir }>()
const emit = defineEmits<{ open: [task: Task] }>()
</script>

<template>
  <div class="hidden grid-cols-4 gap-4 md:grid">
    <KanbanColumn
      v-for="status in STATUS_ORDER"
      :key="status"
      :status="status"
      :tasks="tasks"
      :swimlane-by="swimlaneBy"
      :sort-by="sortBy"
      :sort-dir="sortDir"
      @open="emit('open', $event)"
    />
  </div>
</template>
