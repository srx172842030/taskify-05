<script setup lang="ts">
defineProps<{ type?: string; placeholder?: string }>()
const model = defineModel<string>()
</script>

<template>
  <input
    v-model="model"
    :type="type ?? 'text'"
    :placeholder="placeholder"
    class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  />
</template>
