<script setup lang="ts">
export interface SelectOption {
  value: string
  label: string
}
defineProps<{ options: SelectOption[]; placeholder?: string }>()
const model = defineModel<string>()
</script>

<template>
  <select
    v-model="model"
    class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  >
    <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
    <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
  </select>
</template>
