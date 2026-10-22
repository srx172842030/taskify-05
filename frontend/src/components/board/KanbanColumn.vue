<script setup lang="ts">
import { ref, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import TaskCard from './TaskCard.vue'
import { useBoard } from '@/composables/useBoard'
import { STATUS_LABELS } from '@/services/taskStatus'
import type { Task, TaskStatus } from '@/services/taskApi'

const props = defineProps<{ status: TaskStatus; tasks: Task[] }>()
const emit = defineEmits<{ open: [task: Task] }>()

const { moveTask } = useBoard()

const items = ref<Task[]>([])

watch(
  () => props.tasks.filter((t) => t.status === props.status),
  (v) => {
    items.value = v
  },
  { immediate: true },
)

function onAdd(evt: { newIndex?: number }) {
  const idx = evt.newIndex
  if (idx === undefined) return
  const task = items.value[idx]
  if (!task || task.status === props.status) return
  // 从其他列拖入：交给 moveTask（乐观更新 + 后端相邻列校验，失败自动回滚）
  void moveTask(task, props.status)
}
</script>

<template>
  <section class="flex flex-col rounded-lg border bg-muted/40">
    <header class="flex items-center justify-between px-3 py-2">
      <h3 class="text-sm font-semibold">{{ STATUS_LABELS[status] }}</h3>
      <span class="text-xs text-muted-foreground">{{ items.length }}</span>
    </header>

    <VueDraggable
      v-model="items"
      group="tasks"
      item-key="id"
      :animation="150"
      ghost-class="opacity-40"
      class="flex min-h-[6rem] flex-1 flex-col gap-2 p-2"
      @add="onAdd"
    >
      <TaskCard v-for="t in items" :key="t.id" :task="t" @click="emit('open', t)" />
    </VueDraggable>
  </section>
</template>
