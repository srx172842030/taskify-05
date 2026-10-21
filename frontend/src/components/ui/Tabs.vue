<script setup lang="ts">
export interface TabItem {
  value: string
  label: string
}
defineProps<{ tabs: TabItem[] }>()
const active = defineModel<string>()
</script>

<template>
  <div>
    <div class="flex gap-1 border-b">
      <button
        v-for="t in tabs"
        :key="t.value"
        class="px-3 py-2 text-sm transition-colors"
        :class="
          active === t.value
            ? 'border-b-2 border-primary font-medium text-foreground'
            : 'text-muted-foreground hover:text-foreground'
        "
        @click="active = t.value"
      >
        {{ t.label }}
      </button>
    </div>
    <div class="pt-3"><slot /></div>
  </div>
</template>
