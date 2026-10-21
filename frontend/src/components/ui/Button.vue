<script setup lang="ts">
defineProps<{ variant?: 'default' | 'secondary' | 'destructive' | 'ghost'; disabled?: boolean }>()
</script>

<template>
  <button
    :disabled="disabled"
    class="inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium transition-colors disabled:opacity-60"
    :class="{
      'bg-primary text-primary-foreground hover:opacity-90': variant === 'default' || !variant,
      'bg-secondary text-secondary-foreground': variant === 'secondary',
      'bg-destructive text-destructive-foreground': variant === 'destructive',
      'hover:bg-muted': variant === 'ghost',
    }"
  >
    <slot />
  </button>
</template>
