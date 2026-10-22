<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { reminderApi, type Reminder } from '@/services/reminderApi'
import { formatTaskTime } from '@/services/taskTime'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ taskId: string }>()
const { push } = useToast()

const reminders = ref<Reminder[]>([])
const mode = ref<'ABSOLUTE' | 'RELATIVE'>('ABSOLUTE')
const triggerAt = ref('')
const offsetMinutes = ref(1440)
const anchor = ref<'START' | 'DUE'>('DUE')
const adding = ref(false)

async function load() {
  reminders.value = await reminderApi.list(props.taskId)
}
onMounted(load)

async function add() {
  adding.value = true
  try {
    if (mode.value === 'ABSOLUTE') {
      await reminderApi.create(props.taskId, {
        mode: 'ABSOLUTE',
        triggerAt: new Date(triggerAt.value).toISOString(),
      })
    } else {
      await reminderApi.create(props.taskId, {
        mode: 'RELATIVE',
        offsetMinutes: offsetMinutes.value,
        anchor: anchor.value,
      })
    }
    triggerAt.value = ''
    await load()
    push('已添加提醒', 'success')
  } catch (e) {
    push(e instanceof Error ? e.message : '添加失败', 'error')
  } finally {
    adding.value = false
  }
}

async function remove(id: string) {
  try {
    await reminderApi.delete(id)
    await load()
  } catch (e) {
    push(e instanceof Error ? e.message : '删除失败', 'error')
  }
}
</script>

<template>
  <div class="space-y-3">
    <p class="text-sm font-medium">提醒</p>
    <div v-if="reminders.length === 0" class="text-xs text-muted-foreground">暂无提醒</div>
    <div
      v-for="r in reminders"
      :key="r.id"
      class="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
    >
      <span>
        <template v-if="r.mode === 'ABSOLUTE'">{{ formatTaskTime(r.triggerAt, false) }}</template>
        <template v-else>提前 {{ r.offsetMinutes }} 分钟（{{ r.anchor === 'START' ? '开始前' : '截止前' }}）</template>
      </span>
      <button class="text-xs text-muted-foreground hover:text-destructive" @click="remove(r.id)">删除</button>
    </div>

    <div class="space-y-2 rounded-md border p-3">
      <div class="flex gap-2">
        <button
          class="rounded border px-2 py-1 text-xs"
          :class="mode === 'ABSOLUTE' ? 'bg-muted' : ''"
          @click="mode = 'ABSOLUTE'"
        >
          绝对时间
        </button>
        <button
          class="rounded border px-2 py-1 text-xs"
          :class="mode === 'RELATIVE' ? 'bg-muted' : ''"
          @click="mode = 'RELATIVE'"
        >
          相对提前
        </button>
      </div>
      <div v-if="mode === 'ABSOLUTE'" class="space-y-1">
        <label class="block text-xs text-muted-foreground">触发时间</label>
        <input
          v-model="triggerAt"
          type="datetime-local"
          class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
        />
      </div>
      <div v-else class="space-y-1">
        <label class="block text-xs text-muted-foreground">提前分钟数</label>
        <input
          v-model.number="offsetMinutes"
          type="number"
          min="1"
          class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
        />
        <label class="block text-xs text-muted-foreground">锚定时间</label>
        <select
          v-model="anchor"
          class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="DUE">截止时间</option>
          <option value="START">开始时间</option>
        </select>
      </div>
      <button
        :disabled="adding"
        class="rounded-md bg-primary px-3 py-1.5 text-xs text-primary-foreground disabled:opacity-60"
        @click="add"
      >
        添加提醒
      </button>
    </div>
  </div>
</template>
