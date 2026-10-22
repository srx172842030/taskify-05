<script setup lang="ts">
import { ref } from 'vue'
import { Bell } from 'lucide-vue-next'
import { useNotifications } from '@/composables/useNotifications'
import NotificationPanel from './NotificationPanel.vue'

const { unread, load, markAllRead } = useNotifications()
const open = ref(false)

function toggle() {
  open.value = !open.value
  if (open.value) load()
}
</script>

<template>
  <div class="relative">
    <button class="relative h-9 rounded-md border px-2" aria-label="通知" @click="toggle">
      <Bell class="h-4 w-4" />
      <span
        v-if="unread > 0"
        class="absolute -right-1 -top-1 rounded-full bg-destructive px-1 text-[10px] leading-4 text-white"
      >
        {{ unread > 99 ? '99+' : unread }}
      </span>
    </button>
    <NotificationPanel v-if="open" @mark-all="markAllRead()" />
  </div>
</template>
