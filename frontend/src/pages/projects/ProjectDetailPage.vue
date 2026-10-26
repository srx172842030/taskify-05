<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { projectApi, type Project } from '@/services/projectApi'
import { taskApi, type Task, type TaskStatus, type Priority } from '@/services/taskApi'
import { useTasks } from '@/stores/tasks'
import { useUsers } from '@/stores/users'
import { useAuth } from '@/stores/auth'
import { STATUS_LABELS, STATUS_ORDER } from '@/services/taskStatus'
import { toast } from 'vue-sonner'
import KanbanBoard from '@/components/board/KanbanBoard.vue'
import KanbanColumn from '@/components/board/KanbanColumn.vue'
import TaskTableView from '@/components/table/TaskTableView.vue'
import TaskDetailDrawer from '@/components/task/TaskDetailDrawer.vue'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { SwimlaneBy } from '@/composables/useSwimlane'
import { useViewConfig } from '@/composables/useViewConfig'
import type { SortBy, SortDir } from '@/composables/useTaskQuery'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'

const route = useRoute()
const projectId = computed(() => route.params.id as string)
const isDesktop = useMediaQuery('(min-width: 768px)')

const { tasks, load, upsert } = useTasks()
const { users, load: loadUsers } = useUsers()

const project = ref<Project | null>(null)
const viewMode = ref<'board' | 'table'>('board')
const mobileColumn = ref<TaskStatus>('TODO')
const selectedTask = ref<Task | null>(null)
const drawerOpen = ref(false)

const swimlaneBy = ref<SwimlaneBy>(null)
const swimlaneSel = computed({
  get: () => swimlaneBy.value ?? 'all',
  set: (v) => (swimlaneBy.value = v === 'all' ? null : (v as SwimlaneBy)),
})
const boardMineOnly = ref(false)
const boardSortBy = ref<SortBy>('dueAt')
const boardSortDir = ref<SortDir>('asc')

// 看板视图配置持久化（泳道分组 + 只看我的任务 + 排序）
const { load: loadBoardConfig, save: saveBoardConfig } = useViewConfig(projectId, 'board')
const persistedBoard = loadBoardConfig<{
  swimlaneBy: SwimlaneBy
  mineOnly: boolean
  sortBy: SortBy
  sortDir: SortDir
}>()
if (persistedBoard) {
  if (persistedBoard.swimlaneBy) swimlaneBy.value = persistedBoard.swimlaneBy
  boardMineOnly.value = persistedBoard.mineOnly ?? false
  boardSortBy.value = persistedBoard.sortBy ?? 'dueAt'
  boardSortDir.value = persistedBoard.sortDir ?? 'asc'
}
watch([swimlaneBy, boardMineOnly, boardSortBy, boardSortDir], () =>
  saveBoardConfig({
    swimlaneBy: swimlaneBy.value,
    mineOnly: boardMineOnly.value,
    sortBy: boardSortBy.value,
    sortDir: boardSortDir.value,
  }),
)

const auth = useAuth()
const currentUserId = computed(() => auth.user.value?.id ?? '')
const boardTasks = computed(() =>
  boardMineOnly.value ? tasks.value.filter((t) => t.assigneeId === currentUserId.value) : tasks.value,
)

function toggleBoardSortDir() {
  boardSortDir.value = boardSortDir.value === 'asc' ? 'desc' : 'asc'
}

const createOpen = ref(false)
const newTitle = ref('')
const newDescription = ref('')
const newAssignee = ref('')
const newPriority = ref('')
const newPrioritySel = computed({
  get: () => newPriority.value || 'none',
  set: (v) => (newPriority.value = v === 'none' ? '' : v),
})
const creating = ref(false)

const initialLoading = ref(true)

async function loadProject() {
  try {
    project.value = await projectApi.get(projectId.value)
    await load(projectId.value)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    initialLoading.value = false
  }
}

onMounted(() => {
  loadProject()
  loadUsers()
})

watch(projectId, () => loadProject())

watch(
  tasks,
  (list) => {
    if (selectedTask.value) {
      const updated = list.find((t) => t.id === selectedTask.value!.id)
      if (updated) selectedTask.value = updated
    }
  },
  { deep: true },
)

// 移动端单列视图：任务移动后跟随其新列，确保卡片仍可见
watch(
  () => selectedTask.value?.status,
  (s) => {
    if (s) mobileColumn.value = s
  },
)

function openTask(t: Task) {
  selectedTask.value = t
  drawerOpen.value = true
}

async function createTask() {
  if (!newTitle.value.trim()) {
    toast.error('标题不能为空')
    return
  }
  creating.value = true
  try {
    const t = await taskApi.create(projectId.value, {
      title: newTitle.value.trim(),
      description: newDescription.value || undefined,
      assigneeId: newAssignee.value || null,
      priority: (newPriority.value || null) as Priority | null,
    })
    upsert(t)
    newTitle.value = ''
    newDescription.value = ''
    newAssignee.value = ''
    newPriority.value = ''
    createOpen.value = false
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 py-8">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <RouterLink to="/projects" class="text-sm text-muted-foreground hover:text-foreground"
          >← 项目</RouterLink
        >
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
            :class="viewMode === 'table' ? 'bg-muted' : ''"
            @click="viewMode = 'table'"
          >
            表格
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
      <Select v-model="newPrioritySel">
        <SelectTrigger class="w-full">
          <SelectValue placeholder="优先级（可选）" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="none">无优先级</SelectItem>
          <SelectItem value="HIGH">高</SelectItem>
          <SelectItem value="MEDIUM">中</SelectItem>
          <SelectItem value="LOW">低</SelectItem>
        </SelectContent>
      </Select>
      <button
        :disabled="creating"
        class="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground disabled:opacity-60"
        @click="createTask"
      >
        {{ creating ? '创建中…' : '创建' }}
      </button>
    </div>

    <p v-if="initialLoading" class="mt-6 text-muted-foreground">加载中…</p>
    <template v-else>
      <div v-if="viewMode === 'board'" class="mt-6">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Select v-model="swimlaneSel">
            <SelectTrigger class="h-9 w-36">
              <SelectValue placeholder="泳道" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">无泳道</SelectItem>
              <SelectItem value="assignee">按负责人</SelectItem>
              <SelectItem value="priority">按优先级</SelectItem>
            </SelectContent>
          </Select>
          <label class="flex items-center gap-2 text-sm">
            <Checkbox v-model="boardMineOnly" />
            只看我的任务
          </label>
          <Select v-model="boardSortBy">
            <SelectTrigger class="h-9 w-36">
              <SelectValue placeholder="排序" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="dueAt">按截止日</SelectItem>
              <SelectItem value="createdAt">按创建时间</SelectItem>
              <SelectItem value="title">按标题</SelectItem>
              <SelectItem value="priority">按优先级</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" size="sm" class="h-9" @click="toggleBoardSortDir">
            {{ boardSortDir === 'asc' ? '升序' : '降序' }}
          </Button>
        </div>
        <KanbanBoard
          v-if="isDesktop"
          :tasks="boardTasks"
          :swimlane-by="swimlaneBy"
          :sort-by="boardSortBy"
          :sort-dir="boardSortDir"
          @open="openTask"
        />
        <div v-else>
          <Tabs v-model="mobileColumn">
            <TabsList>
              <TabsTrigger v-for="s in STATUS_ORDER" :key="s" :value="s">
                {{ STATUS_LABELS[s] }}
              </TabsTrigger>
            </TabsList>
          </Tabs>
          <KanbanColumn
            class="mt-3"
            :status="mobileColumn"
            :tasks="boardTasks"
            :swimlane-by="swimlaneBy"
            :sort-by="boardSortBy"
            :sort-dir="boardSortDir"
            @open="openTask"
          />
        </div>
      </div>
      <div v-else class="mt-6">
        <TaskTableView :key="projectId" :tasks="tasks" :project-id="projectId" @open="openTask" />
      </div>
    </template>

    <TaskDetailDrawer v-model="drawerOpen" :task="selectedTask" />
  </section>
</template>
