<script setup lang="ts">
defineProps<{ placeholder?: string; rows?: number }>()
const model = defineModel<string>()
</script>

<template>
  <textarea
    v-model="model"
    :placeholder="placeholder"
    :rows="rows ?? 3"
    class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  />
</template>
