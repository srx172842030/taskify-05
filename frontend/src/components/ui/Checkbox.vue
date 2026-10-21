<script setup lang="ts">
const model = defineModel<boolean>()
</script>

<template>
  <label class="inline-flex cursor-pointer items-center gap-2 text-sm">
    <input
      v-model="model"
      type="checkbox"
      class="h-4 w-4 rounded border-input accent-primary"
    />
    <span><slot /></span>
  </label>
</template>
