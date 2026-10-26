<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { toast } from 'vue-sonner'
import { reminderApi, type Reminder } from '@/services/reminderApi'
import { formatTaskTime } from '@/services/taskTime'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const props = defineProps<{ taskId: string }>()

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
    toast.success('已添加提醒')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '添加失败')
  } finally {
    adding.value = false
  }
}

async function remove(id: string) {
  try {
    await reminderApi.delete(id)
    await load()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '删除失败')
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
      <Button
        variant="ghost"
        size="sm"
        class="h-auto px-2 py-0 text-xs text-muted-foreground hover:text-destructive"
        @click="remove(r.id)"
      >
        删除
      </Button>
    </div>

    <div class="space-y-2 rounded-md border p-3">
      <div class="flex gap-2">
        <Button
          type="button"
          size="sm"
          :variant="mode === 'ABSOLUTE' ? 'secondary' : 'outline'"
          @click="mode = 'ABSOLUTE'"
        >
          绝对时间
        </Button>
        <Button
          type="button"
          size="sm"
          :variant="mode === 'RELATIVE' ? 'secondary' : 'outline'"
          @click="mode = 'RELATIVE'"
        >
          相对提前
        </Button>
      </div>

      <div v-if="mode === 'ABSOLUTE'" class="space-y-1">
        <Label for="reminder-trigger">触发时间</Label>
        <Input id="reminder-trigger" v-model="triggerAt" type="datetime-local" />
      </div>
      <div v-else class="space-y-1">
        <Label for="reminder-offset">提前分钟数</Label>
        <Input id="reminder-offset" v-model.number="offsetMinutes" type="number" min="1" />
        <Label for="reminder-anchor">锚定时间</Label>
        <Select v-model="anchor">
          <SelectTrigger id="reminder-anchor">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="DUE">截止时间</SelectItem>
            <SelectItem value="START">开始时间</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Button :disabled="adding" @click="add">添加提醒</Button>
    </div>
  </div>
</template>
