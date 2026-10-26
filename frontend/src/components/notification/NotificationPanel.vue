<script setup lang="ts">
import { useNotifications as useNotificationsStore } from '@/stores/notifications'
import { Button } from '@/components/ui/button'

const emit = defineEmits<{ markAll: [] }>()
const { notifications, markRead } = useNotificationsStore()
</script>

<template>
  <div
    class="fixed inset-x-4 top-16 z-50 rounded-md border bg-popover p-2 shadow-md md:absolute md:inset-x-auto md:right-0 md:top-auto md:mt-1 md:w-80 md:max-w-[calc(100vw-2rem)]"
  >
    <div class="flex items-center justify-between border-b pb-2">
      <span class="text-sm font-semibold">通知</span>
      <Button
        variant="ghost"
        size="sm"
        class="h-auto px-2 py-0 text-xs text-muted-foreground hover:text-foreground"
        @click="emit('markAll')"
      >
        全部已读
      </Button>
    </div>
    <div class="max-h-80 overflow-y-auto">
      <div v-if="notifications.length === 0" class="py-4 text-center text-sm text-muted-foreground">
        暂无通知
      </div>
      <Button
        v-for="n in notifications"
        :key="n.id"
        variant="ghost"
        class="flex h-auto w-full flex-col items-start gap-1 border-b py-2 text-left last:border-0"
        :class="n.read ? '' : 'font-medium'"
        @click="!n.read && markRead(n.id)"
      >
        <span class="text-sm">{{ n.message }}</span>
        <span class="text-xs text-muted-foreground">{{ new Date(n.createdAt).toLocaleString() }}</span>
      </Button>
    </div>
  </div>
</template>
