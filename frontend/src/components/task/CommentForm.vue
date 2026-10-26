<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { commentApi } from '@/services/commentApi'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'

const props = defineProps<{ taskId: string }>()
const emit = defineEmits<{ added: [] }>()

const content = ref('')
const submitting = ref(false)

async function submit() {
  if (!content.value.trim()) {
    toast.error('内容不能为空')
    return
  }
  submitting.value = true
  try {
    await commentApi.create(props.taskId, content.value.trim())
    content.value = ''
    emit('added')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '发表失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="flex flex-col gap-2" @submit.prevent="submit">
    <Textarea v-model="content" rows="2" placeholder="发表评论…" />
    <Button type="submit" :disabled="submitting" class="self-end">
      {{ submitting ? '发表中…' : '评论' }}
    </Button>
  </form>
</template>
