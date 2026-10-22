<script setup lang="ts">
import { ref } from 'vue'
import { X } from 'lucide-vue-next'
import { useProjects } from '@/stores/projects'
import { useToast } from '@/composables/useToast'

const open = defineModel<boolean>({ default: false })
const emit = defineEmits<{ created: [] }>()

const { create } = useProjects()
const { push } = useToast()

const name = ref('')
const description = ref('')
const submitting = ref(false)

async function submit() {
  if (!name.value.trim()) {
    push('名称不能为空', 'error')
    return
  }
  submitting.value = true
  try {
    await create(name.value.trim(), description.value || undefined)
    name.value = ''
    description.value = ''
    open.value = false
    emit('created')
  } catch (e) {
    push(e instanceof Error ? e.message : '创建失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Transition name="fade">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" @click.self="open = false">
      <div class="w-full max-w-md rounded-lg border bg-card p-6 shadow-lg">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">新建项目</h2>
          <button aria-label="关闭" @click="open = false">
            <X class="h-5 w-5" />
          </button>
        </div>

        <form class="mt-4 space-y-3" @submit.prevent="submit">
          <input
            v-model="name"
            placeholder="项目名称"
            class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
          <textarea
            v-model="description"
            rows="3"
            placeholder="描述（可选）"
            class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
          <button
            type="submit"
            :disabled="submitting"
            class="w-full rounded-md bg-primary py-2 text-sm text-primary-foreground disabled:opacity-60"
          >
            {{ submitting ? '创建中…' : '创建' }}
          </button>
        </form>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
