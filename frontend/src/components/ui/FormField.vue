<script setup lang="ts">
defineProps<{ label?: string; error?: string }>()
</script>

<template>
  <div class="space-y-1">
    <label v-if="label" class="block text-sm font-medium">{{ label }}</label>
    <slot />
    <p v-if="error" class="text-xs text-destructive">{{ error }}</p>
  </div>
</template>
