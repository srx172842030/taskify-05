<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { projectApi, type Project } from '@/services/projectApi'
import { taskApi, type Task, type TaskStatus } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useUsers } from '@/stores/users'
import { STATUS_LABELS, STATUS_ORDER } from '@/services/taskStatus'
import { formatTaskTime } from '@/services/taskTime'
import { useToast } from '@/composables/useToast'
import KanbanBoard from '@/components/board/KanbanBoard.vue'
import KanbanColumn from '@/components/board/KanbanColumn.vue'
import TaskDetailDrawer from '@/components/task/TaskDetailDrawer.vue'
import Tabs from '@/components/ui/Tabs.vue'

const route = useRoute()
const projectId = computed(() => route.params.id as string)
const isDesktop = useMediaQuery('(min-width: 768px)')

const { tasks, loading, load, upsert, reset } = useTasks()
const { users, load: loadUsers } = useUsers()
const { push } = useToast()

const project = ref<Project | null>(null)
const viewMode = ref<'board' | 'list'>('board')
const mobileColumn = ref<TaskStatus>('TODO')
const selectedTask = ref<Task | null>(null)
const drawerOpen = ref(false)

const createOpen = ref(false)
const newTitle = ref('')
const newDescription = ref('')
const newAssignee = ref('')
const creating = ref(false)

const statusTabs = STATUS_ORDER.map((s) => ({ value: s, label: STATUS_LABELS[s] }))

async function loadProject() {
  try {
    project.value = null
    reset()
    selectedTask.value = null
    drawerOpen.value = false
    project.value = await projectApi.get(projectId.value)
    await load(projectId.value)
  } catch (e) {
    push(e instanceof Error ? e.message : '加载失败', 'error')
  }
}

onMounted(() => {
  loadProject()
  loadUsers()
})

watch(projectId, () => loadProject())

watch(tasks, (list) => {
  if (selectedTask.value) {
    const updated = list.find((t) => t.id === selectedTask.value!.id)
    if (updated) selectedTask.value = updated
  }
}, { deep: true })

function openTask(t: Task) {
  selectedTask.value = t
  drawerOpen.value = true
}

async function createTask() {
  if (!newTitle.value.trim()) {
    push('标题不能为空', 'error')
    return
  }
  creating.value = true
  try {
    const t = await taskApi.create(projectId.value, {
      title: newTitle.value.trim(),
      description: newDescription.value || undefined,
      assigneeId: newAssignee.value || null,
    })
    upsert(t)
    newTitle.value = ''
    newDescription.value = ''
    newAssignee.value = ''
    createOpen.value = false
  } catch (e) {
    push(e instanceof Error ? e.message : '创建失败', 'error')
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 py-8">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <RouterLink to="/projects" class="text-sm text-muted-foreground hover:text-foreground">← 项目</RouterLink>
        <h1 class="mt-1 text-2xl font-bold">{{ project?.name ?? '…' }}</h1>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex rounded-md border">
          <button
            class="px-3 py-1.5 text-sm"
            :class="viewMode === 'board' ? 'bg-muted' : ''"
            @click="viewMode = 'board'"
          >
            看板
          </button>
          <button
            class="px-3 py-1.5 text-sm"
            :class="viewMode === 'list' ? 'bg-muted' : ''"
            @click="viewMode = 'list'"
          >
            列表
          </button>
        </div>
        <button
          class="rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground"
          @click="createOpen = !createOpen"
        >
          新建任务
        </button>
      </div>
    </div>

    <div v-if="createOpen" class="mt-4 space-y-3 rounded-lg border p-4">
      <input
        v-model="newTitle"
        placeholder="任务标题"
        class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
      />
      <textarea
        v-model="newDescription"
        rows="2"
        placeholder="描述（可选）"
        class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
      />
      <select
        v-model="newAssignee"
        class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
      >
        <option value="">未分配</option>
        <option v-for="u in users" :key="u.id" :value="u.id">{{ u.username }}</option>
      </select>
      <button
        :disabled="creating"
        class="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground disabled:opacity-60"
        @click="createTask"
      >
        {{ creating ? '创建中…' : '创建' }}
      </button>
    </div>

    <p v-if="loading" class="mt-6 text-muted-foreground">加载中…</p>
    <template v-else>
      <div v-if="viewMode === 'board'" class="mt-6">
        <KanbanBoard v-if="isDesktop" :tasks="tasks" @open="openTask" />
        <div v-else>
          <Tabs v-model="mobileColumn" :tabs="statusTabs" />
          <KanbanColumn class="mt-3" :status="mobileColumn" :tasks="tasks" @open="openTask" />
        </div>
      </div>
      <div v-else class="mt-6 space-y-2">
        <div v-for="t in tasks" :key="t.id" class="flex items-center justify-between rounded-md border p-3">
          <div>
            <p class="font-medium">{{ t.title }}</p>
            <p class="text-xs text-muted-foreground">
              {{ STATUS_LABELS[t.status] }}
              <span v-if="t.blocked" class="ml-2 text-amber-600">被阻塞</span>
            </p>
            <p v-if="t.dueAt" class="text-xs text-muted-foreground">截止：{{ formatTaskTime(t.dueAt, t.dueAllDay) }}</p>
          </div>
          <button class="text-sm text-primary" @click="openTask(t)">详情</button>
        </div>
      </div>
    </template>

    <TaskDetailDrawer v-model="drawerOpen" :task="selectedTask" />
  </section>
</template>
