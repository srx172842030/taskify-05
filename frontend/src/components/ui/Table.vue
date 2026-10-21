<script setup lang="ts">
export interface TableColumn {
  key: string
  label: string
}
defineProps<{ columns: TableColumn[]; rows: Record<string, unknown>[] }>()
</script>

<template>
  <div class="overflow-x-auto rounded-md border">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b bg-muted">
          <th v-for="c in columns" :key="c.key" class="px-3 py-2 text-left font-medium">
            {{ c.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(r, i) in rows" :key="i" class="border-b last:border-0">
          <td v-for="c in columns" :key="c.key" class="px-3 py-2">{{ r[c.key] }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
