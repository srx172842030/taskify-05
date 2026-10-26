<script setup lang="ts">
import { Skeleton } from '@/components/ui/skeleton'
</script>

<template>
  <div class="w-full space-y-2 py-8">
    <Skeleton class="h-4 w-full" />
    <Skeleton class="h-4 w-5/6" />
    <Skeleton class="h-4 w-2/3" />
  </div>
</template>
