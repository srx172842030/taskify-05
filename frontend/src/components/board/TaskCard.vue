<script setup lang="ts">
import { computed } from 'vue'
import type { Task } from '@/services/taskApi'
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
    <p class="text-sm font-medium">{{ task.title }}</p>
    <p v-if="assigneeName" class="mt-1 text-xs text-muted-foreground">负责人：{{ assigneeName }}</p>
  </button>
</template>
