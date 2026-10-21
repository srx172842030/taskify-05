<script setup lang="ts">
const open = defineModel<boolean>({ default: false })
defineProps<{ side?: 'left' | 'right' }>()
</script>

<template>
  <Transition name="fade">
    <div v-if="open" class="fixed inset-0 z-40 bg-black/40" @click="open = false">
      <aside
        class="h-full w-64 bg-card p-4 shadow-lg"
        :class="side === 'right' ? 'ml-auto' : ''"
        @click.stop
      >
        <slot />
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
