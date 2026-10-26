<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { useProjects } from '@/stores/projects'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'

const open = defineModel<boolean>({ default: false })
const emit = defineEmits<{ created: [] }>()

const { create } = useProjects()

const name = ref('')
const description = ref('')
const submitting = ref(false)

async function submit() {
  if (!name.value.trim()) {
    toast.error('名称不能为空')
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
    toast.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Dialog v-model:open="open">
    <DialogContent class="max-w-md">
      <DialogHeader>
        <DialogTitle>新建项目</DialogTitle>
        <DialogDescription class="sr-only">填写项目名称与可选描述</DialogDescription>
      </DialogHeader>

      <form class="space-y-3" @submit.prevent="submit">
        <div class="space-y-1">
          <Label for="project-name">项目名称</Label>
          <Input id="project-name" v-model="name" placeholder="项目名称" />
        </div>
        <div class="space-y-1">
          <Label for="project-description">描述（可选）</Label>
          <Textarea id="project-description" v-model="description" rows="3" placeholder="描述（可选）" />
        </div>
        <Button type="submit" :disabled="submitting" class="w-full">
          {{ submitting ? '创建中…' : '创建' }}
        </Button>
      </form>
    </DialogContent>
  </Dialog>
</template>
