<script setup lang="ts">
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
defineProps<{ page: number; totalPages: number }>()
const emit = defineEmits<{ change: [page: number] }>()
</script>

<template>
  <div class="flex items-center gap-2 text-sm">
    <button
      class="rounded-md border px-2 py-1 disabled:opacity-50"
      :disabled="page <= 1"
      @click="emit('change', page - 1)"
    >
      <ChevronLeft class="h-4 w-4" />
    </button>
    <span class="text-muted-foreground">第 {{ page }} / {{ totalPages }} 页</span>
    <button
      class="rounded-md border px-2 py-1 disabled:opacity-50"
      :disabled="page >= totalPages"
      @click="emit('change', page + 1)"
    >
      <ChevronRight class="h-4 w-4" />
    </button>
  </div>
</template>
