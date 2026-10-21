<script setup lang="ts">
import { X } from 'lucide-vue-next'
const open = defineModel<boolean>({ default: false })
defineProps<{ title?: string }>()
</script>

<template>
  <Transition name="fade">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="open = false"
    >
      <div class="w-full max-w-md rounded-lg border bg-card p-6 shadow-lg">
        <div class="flex items-center justify-between">
          <h2 v-if="title" class="text-lg font-semibold">{{ title }}</h2>
          <button class="ml-auto" aria-label="关闭" @click="open = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <slot />
      </div>
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
