<script setup lang="ts">
import { ref } from 'vue'
import { Bell } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
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
    <Button variant="outline" size="icon" aria-label="通知" @click="toggle">
      <Bell />
    </Button>
    <Badge
      v-if="unread > 0"
      variant="destructive"
      class="pointer-events-none absolute -right-1 -top-1 h-4 min-w-4 justify-center rounded-full px-1 text-[10px]"
    >
      {{ unread > 99 ? '99+' : unread }}
    </Badge>
    <NotificationPanel v-if="open" @mark-all="markAllRead()" />
  </div>
</template>
