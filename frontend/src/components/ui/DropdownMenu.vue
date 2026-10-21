<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const open = ref(false)

function onClickOutside(e: MouseEvent) {
  if (open.value && !(e.target as HTMLElement).closest('[data-dropdown]')) {
    open.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<template>
  <div class="relative" data-dropdown>
    <div @click="open = !open"><slot name="trigger" /></div>
    <div
      v-if="open"
      class="absolute right-0 z-50 mt-1 w-40 rounded-md border bg-popover p-1 shadow-md"
      @click="open = false"
    >
      <slot name="content" />
    </div>
  </div>
</template>
