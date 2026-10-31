<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'
import { useAuth } from '@/stores/auth'
import { useUsers } from '@/stores/users'
import type { Task, TaskStatus, Priority } from '@/services/taskApi'
import { STATUS_LABELS, STATUS_ORDER } from '@/services/taskStatus'
import { formatTaskTime } from '@/services/taskTime'
import {
  useTaskQuery,
  priorityLabel,
  type GroupBy,
  type SortBy,
  type SortDir,
  type ViewFilters,
} from '@/composables/useTaskQuery'
import { useViewConfig } from '@/composables/useViewConfig'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import BatchEditBar from './BatchEditBar.vue'

const props = defineProps<{ tasks: Task[]; projectId: string }>()
const emit = defineEmits<{ open: [task: Task] }>()

const auth = useAuth()
const { users, byId } = useUsers()
const currentUserId = computed(() => auth.user.value?.id ?? '')

const { filters, groupBy, sortBy, sortDir, groups } = useTaskQuery(
  toRef(props, 'tasks'),
  (id) => byId.value[id] ?? id,
  currentUserId,
)

// 视图配置持久化：加载既有配置，变更时保存
const { load: loadConfig, save: saveConfig } = useViewConfig(toRef(props, 'projectId'), 'table')
const persisted = loadConfig<{ filters: ViewFilters; groupBy: GroupBy; sortBy: SortBy; sortDir: SortDir }>()
if (persisted) {
  if (persisted.filters) filters.value = { ...filters.value, ...persisted.filters }
  groupBy.value = persisted.groupBy ?? null
  sortBy.value = persisted.sortBy ?? 'dueAt'
  sortDir.value = persisted.sortDir ?? 'asc'
}
watch(
  [filters, groupBy, sortBy, sortDir],
  () =>
    saveConfig({
      filters: filters.value,
      groupBy: groupBy.value,
      sortBy: sortBy.value,
      sortDir: sortDir.value,
    }),
  { deep: true },
)

// 下拉绑定：用哨兵字符串映射到可空配置
const assigneeSel = computed({
  get: () => filters.value.assigneeId ?? 'all',
  set: (v) => (filters.value.assigneeId = v === 'all' ? null : v),
})
const statusSel = computed({
  get: () => filters.value.status ?? 'all',
  set: (v) => (filters.value.status = v === 'all' ? null : (v as TaskStatus)),
})
const prioritySel = computed({
  get: () => filters.value.priority ?? 'all',
  set: (v) => (filters.value.priority = v === 'all' ? null : (v as Priority)),
})
const groupBySel = computed({
  get: () => groupBy.value ?? 'all',
  set: (v) => (groupBy.value = v === 'all' ? null : (v as GroupBy)),
})

// 行多选（US2 批量编辑使用）
const selectedIds = ref<string[]>([])
const visibleTasks = computed(() => groups.value.flatMap((g) => g.tasks))
const allSelected = computed(
  () => visibleTasks.value.length > 0 && visibleTasks.value.every((t) => selectedIds.value.includes(t.id)),
)
function toggleAll() {
  selectedIds.value = allSelected.value ? [] : visibleTasks.value.map((t) => t.id)
}
function toggleOne(id: string) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}
function toggleSortDir() {
  sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
}
</script>

<template>
  <div class="space-y-3">
    <!-- 筛选 / 分组 / 排序工具栏 -->
    <div class="flex flex-wrap items-center gap-2">
      <label class="flex items-center gap-2 text-sm">
        <Checkbox v-model="filters.mineOnly" />
        只看我的任务
      </label>

      <Select v-model="assigneeSel">
        <SelectTrigger class="h-9 w-32">
          <SelectValue placeholder="负责人" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">全部负责人</SelectItem>
          <SelectItem v-for="u in users" :key="u.id" :value="u.id">{{ u.username }}</SelectItem>
        </SelectContent>
      </Select>

      <Select v-model="statusSel">
        <SelectTrigger class="h-9 w-32">
          <SelectValue placeholder="状态" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">全部状态</SelectItem>
          <SelectItem v-for="s in STATUS_ORDER" :key="s" :value="s">{{ STATUS_LABELS[s] }}</SelectItem>
        </SelectContent>
      </Select>

      <Select v-model="prioritySel">
        <SelectTrigger class="h-9 w-32">
          <SelectValue placeholder="优先级" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">全部优先级</SelectItem>
          <SelectItem value="HIGH">高</SelectItem>
          <SelectItem value="MEDIUM">中</SelectItem>
          <SelectItem value="LOW">低</SelectItem>
        </SelectContent>
      </Select>

      <Select v-model="groupBySel">
        <SelectTrigger class="h-9 w-32">
          <SelectValue placeholder="分组" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">不分组</SelectItem>
          <SelectItem value="assignee">按负责人</SelectItem>
          <SelectItem value="priority">按优先级</SelectItem>
        </SelectContent>
      </Select>

      <Select v-model="sortBy">
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
      <Button variant="outline" size="sm" class="h-9" @click="toggleSortDir">
        {{ sortDir === 'asc' ? '升序' : '降序' }}
      </Button>
    </div>

    <!-- 批量编辑工具条（有选中项时出现） -->
    <BatchEditBar
      v-if="selectedIds.length > 0"
      :task-ids="selectedIds"
      :project-id="projectId"
      @done="selectedIds = []"
    />

    <!-- 桌面表格 -->
    <div class="hidden overflow-hidden rounded-lg border md:block">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead class="w-10">
              <Checkbox :checked="allSelected" @update:checked="toggleAll" />
            </TableHead>
            <TableHead>标题</TableHead>
            <TableHead class="w-24">状态</TableHead>
            <TableHead class="w-28">负责人</TableHead>
            <TableHead class="w-20">优先级</TableHead>
            <TableHead class="w-32">开始时间</TableHead>
            <TableHead class="w-32">截止时间</TableHead>
            <TableHead class="w-16"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <template v-for="g in groups" :key="g.key">
            <TableRow v-if="g.label" class="bg-muted/40">
              <TableCell colspan="8" class="text-xs font-medium text-muted-foreground">
                {{ g.label }}（{{ g.tasks.length }}）
              </TableCell>
            </TableRow>
            <TableRow v-for="t in g.tasks" :key="t.id">
              <TableCell>
                <Checkbox :checked="selectedIds.includes(t.id)" @update:checked="toggleOne(t.id)" />
              </TableCell>
              <TableCell class="font-medium">{{ t.title }}</TableCell>
              <TableCell>
                <Badge variant="outline">{{ STATUS_LABELS[t.status] }}</Badge>
              </TableCell>
              <TableCell class="text-sm text-muted-foreground">
                {{ t.assigneeId ? (byId[t.assigneeId] ?? t.assigneeId) : '未分配' }}
              </TableCell>
              <TableCell class="text-sm text-muted-foreground">{{ priorityLabel(t.priority) }}</TableCell>
              <TableCell class="text-sm text-muted-foreground">
                {{ t.startAt ? formatTaskTime(t.startAt, t.startAllDay) : '—' }}
              </TableCell>
              <TableCell class="text-sm text-muted-foreground">
                {{ t.dueAt ? formatTaskTime(t.dueAt, t.dueAllDay) : '—' }}
              </TableCell>
              <TableCell>
                <Button variant="ghost" size="sm" @click="emit('open', t)">详情</Button>
              </TableCell>
            </TableRow>
          </template>
          <TableRow v-if="visibleTasks.length === 0">
            <TableCell colspan="8" class="py-8 text-center text-muted-foreground">无匹配任务</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>

    <!-- 手机卡片列表 -->
    <div class="space-y-3 md:hidden">
      <template v-for="g in groups" :key="g.key">
        <p v-if="g.label" class="text-xs font-medium text-muted-foreground">
          {{ g.label }}（{{ g.tasks.length }}）
        </p>
        <div
          v-for="t in g.tasks"
          :key="t.id"
          class="flex items-center justify-between rounded-lg border p-3"
        >
          <div>
            <p class="font-medium">{{ t.title }}</p>
            <p class="text-xs text-muted-foreground">
              {{ STATUS_LABELS[t.status] }} · {{ priorityLabel(t.priority) }} ·
              {{ t.assigneeId ? (byId[t.assigneeId] ?? t.assigneeId) : '未分配' }}
            </p>
            <p v-if="t.startAt" class="text-xs text-muted-foreground">
              开始：{{ formatTaskTime(t.startAt, t.startAllDay) }}
            </p>
            <p v-if="t.dueAt" class="text-xs text-muted-foreground">
              截止：{{ formatTaskTime(t.dueAt, t.dueAllDay) }}
            </p>
          </div>
          <Button variant="ghost" size="sm" @click="emit('open', t)">详情</Button>
        </div>
      </template>
      <p v-if="visibleTasks.length === 0" class="py-8 text-center text-muted-foreground">无匹配任务</p>
    </div>
  </div>
</template>
