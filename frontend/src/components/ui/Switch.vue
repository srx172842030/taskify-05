<script setup lang="ts">
const model = defineModel<boolean>()
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-checked="model"
    class="inline-flex h-6 w-11 items-center rounded-full border px-0.5 transition-colors"
    :class="model ? 'bg-primary' : 'bg-muted'"
    @click="model = !model"
  >
    <span
      class="h-5 w-5 rounded-full bg-background shadow transition-transform"
      :class="model ? 'translate-x-5' : 'translate-x-0'"
    />
  </button>
</template>
