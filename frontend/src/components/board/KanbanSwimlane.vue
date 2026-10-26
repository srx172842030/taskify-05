<script setup lang="ts">
import { ref, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import TaskCard from './TaskCard.vue'
import { useBoard } from '@/composables/useBoard'
import type { Task, TaskStatus } from '@/services/taskApi'

const props = defineProps<{ label: string; tasks: Task[]; status: TaskStatus }>()
const emit = defineEmits<{ open: [task: Task] }>()

const { moveTask } = useBoard()

const items = ref<Task[]>([])

watch(
  () => props.tasks,
  (v) => {
    items.value = [...v]
  },
  { immediate: true },
)

function onAdd(evt: { newIndex?: number }) {
  const idx = evt.newIndex
  if (idx === undefined) return
  const task = items.value[idx]
  if (!task || task.status === props.status) return
  void moveTask(task, props.status)
}
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <p class="px-1 text-xs font-medium text-muted-foreground">{{ label }}（{{ items.length }}）</p>
    <VueDraggable
      v-model="items"
      group="tasks"
      item-key="id"
      :animation="150"
      ghost-class="opacity-40"
      class="flex flex-col gap-2"
      @add="onAdd"
    >
      <TaskCard v-for="t in items" :key="t.id" :task="t" @click="emit('open', t)" />
    </VueDraggable>
  </div>
</template>
