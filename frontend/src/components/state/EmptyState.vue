<script setup lang="ts">
defineProps<{ description?: string }>()
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-2 py-12 text-center text-muted-foreground">
    <div class="text-sm">{{ description ?? '暂无数据' }}</div>
  </div>
</template>
