<script setup lang="ts">
import { ref } from 'vue'
import { commentApi } from '@/services/commentApi'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ taskId: string }>()
const emit = defineEmits<{ added: [] }>()

const { push } = useToast()
const content = ref('')
const submitting = ref(false)

async function submit() {
  if (!content.value.trim()) {
    push('内容不能为空', 'error')
    return
  }
  submitting.value = true
  try {
    await commentApi.create(props.taskId, content.value.trim())
    content.value = ''
    emit('added')
  } catch (e) {
    push(e instanceof Error ? e.message : '发表失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="flex flex-col gap-2" @submit.prevent="submit">
    <textarea
      v-model="content"
      rows="2"
      placeholder="发表评论…"
      class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
    />
    <button
      type="submit"
      :disabled="submitting"
      class="self-end rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground disabled:opacity-60"
    >
      {{ submitting ? '发表中…' : '评论' }}
    </button>
  </form>
</template>
