<script setup lang="ts">
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Sheet :open="open" @update:open="(v) => !v && emit('close')">
    <SheetContent side="left" class="w-64 p-4">
      <SheetHeader>
        <SheetTitle>Taskify</SheetTitle>
      </SheetHeader>
      <nav class="mt-6 flex flex-col gap-2">
        <RouterLink to="/" class="rounded-md px-3 py-2 hover:bg-muted" @click="emit('close')">
          首页
        </RouterLink>
        <RouterLink to="/projects" class="rounded-md px-3 py-2 hover:bg-muted" @click="emit('close')">
          项目
        </RouterLink>
      </nav>
    </SheetContent>
  </Sheet>
</template>
