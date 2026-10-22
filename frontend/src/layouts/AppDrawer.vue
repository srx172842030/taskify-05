<script setup lang="ts">
import { X } from 'lucide-vue-next'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Transition name="fade">
    <div
      v-if="open"
      class="fixed inset-0 z-40 bg-black/40 md:hidden"
      @click="emit('close')"
    >
      <aside
        class="h-full w-64 bg-card p-4 shadow-lg"
        @click.stop
      >
        <div class="flex items-center justify-between">
          <span class="font-semibold">Taskify</span>
          <button aria-label="关闭" @click="emit('close')">
            <X class="h-5 w-5" />
          </button>
        </div>
        <nav class="mt-6 flex flex-col gap-2">
          <RouterLink
            to="/"
            class="rounded-md px-3 py-2 hover:bg-muted"
            @click="emit('close')"
          >
            首页
          </RouterLink>
          <RouterLink
            to="/projects"
            class="rounded-md px-3 py-2 hover:bg-muted"
            @click="emit('close')"
          >
            项目
          </RouterLink>
        </nav>
      </aside>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
