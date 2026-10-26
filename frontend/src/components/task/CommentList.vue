<script setup lang="ts">
import { ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { commentApi, type Comment } from '@/services/commentApi'
import { useAuth } from '@/stores/auth'
import { useUsers } from '@/stores/users'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'

const props = defineProps<{ taskId: string; refreshKey: number }>()

const { user } = useAuth()
const { byId } = useUsers()

const comments = ref<Comment[]>([])
const editingId = ref<string | null>(null)
const editingContent = ref('')

async function load() {
  comments.value = (await commentApi.list(props.taskId)).items
}

watch(() => [props.taskId, props.refreshKey] as const, load, { immediate: true })

function isAuthor(c: Comment) {
  return user.value?.id === c.authorId
}

function startEdit(c: Comment) {
  editingId.value = c.id
  editingContent.value = c.content
}

async function saveEdit(c: Comment) {
  try {
    const updated = await commentApi.update(c.id, editingContent.value.trim())
    const i = comments.value.findIndex((x) => x.id === c.id)
    if (i >= 0) comments.value[i] = updated
    editingId.value = null
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '编辑失败')
  }
}

async function remove(c: Comment) {
  try {
    await commentApi.remove(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '删除失败')
  }
}

function fmt(iso: string) {
  return new Date(iso).toLocaleString()
}
</script>

<template>
  <div class="space-y-3">
    <h4 class="text-sm font-semibold">评论（{{ comments.length }}）</h4>
    <div v-if="comments.length === 0" class="text-sm text-muted-foreground">暂无评论</div>
    <div v-for="c in comments" :key="c.id" class="rounded-md border bg-background p-3">
      <div class="flex items-center justify-between text-xs text-muted-foreground">
        <span>{{ byId[c.authorId] ?? '未知用户' }}</span>
        <span>{{ fmt(c.createdAt) }}</span>
      </div>
      <template v-if="editingId === c.id">
        <Textarea v-model="editingContent" rows="2" class="mt-2" />
        <div class="mt-2 flex gap-2">
          <Button size="sm" @click="saveEdit(c)">保存</Button>
          <Button size="sm" variant="outline" @click="editingId = null">取消</Button>
        </div>
      </template>
      <p v-else class="mt-1 whitespace-pre-wrap text-sm">{{ c.content }}</p>
      <div v-if="!editingId && isAuthor(c)" class="mt-2 flex gap-2">
        <Button
          variant="ghost"
          size="sm"
          class="h-auto px-2 py-0 text-xs text-muted-foreground hover:text-foreground"
          @click="startEdit(c)"
        >
          编辑
        </Button>
        <Button
          variant="ghost"
          size="sm"
          class="h-auto px-2 py-0 text-xs text-destructive"
          @click="remove(c)"
        >
          删除
        </Button>
      </div>
    </div>
  </div>
</template>
