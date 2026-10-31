<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import TaskCard from './TaskCard.vue'
import KanbanSwimlane from './KanbanSwimlane.vue'
import { useBoard } from '@/composables/useBoard'
import { useSwimlane, type SwimlaneBy } from '@/composables/useSwimlane'
import { sortTasks, type SortBy, type SortDir } from '@/composables/useTaskQuery'
import { useUsers } from '@/stores/users'
import { STATUS_LABELS } from '@/services/taskStatus'
import type { Task, TaskStatus } from '@/services/taskApi'
import { Card } from '@/components/ui/card'

const props = defineProps<{
  status: TaskStatus
  tasks: Task[]
  swimlaneBy: SwimlaneBy
  sortBy: SortBy
  sortDir: SortDir
}>()
const emit = defineEmits<{ open: [task: Task] }>()

const { moveTask } = useBoard()
const { byId } = useUsers()

const items = ref<Task[]>([])

const columnTasks = computed(() =>
  sortTasks(
    props.tasks.filter((t) => t.status === props.status),
    props.sortBy,
    props.sortDir,
  ),
)
const { lanes } = useSwimlane(columnTasks, computed(() => props.swimlaneBy), (id) => byId.value[id] ?? id)

watch(
  () => sortTasks(props.tasks.filter((t) => t.status === props.status), props.sortBy, props.sortDir),
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
  <Card class="flex flex-col rounded-lg border bg-muted/40 shadow-none">
    <header class="flex items-center justify-between px-3 py-2">
      <h3 class="text-sm font-semibold">{{ STATUS_LABELS[status] }}</h3>
      <span class="text-xs text-muted-foreground">{{ columnTasks.length }}</span>
    </header>

    <!-- 泳道分组模式 -->
    <div v-if="swimlaneBy" class="flex flex-1 flex-col gap-3 p-2">
      <KanbanSwimlane
        v-for="lane in lanes"
        :key="lane.key"
        :label="lane.label"
        :tasks="lane.tasks"
        :status="status"
        @open="emit('open', $event)"
      />
    </div>

    <!-- 普通平铺模式 -->
    <VueDraggable
      v-else
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
  </Card>
</template>
