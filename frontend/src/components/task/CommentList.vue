<script setup lang="ts">
import { ref, watch } from 'vue'
import { commentApi, type Comment } from '@/services/commentApi'
import { useAuth } from '@/stores/auth'
import { useUsers } from '@/stores/users'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ taskId: string; refreshKey: number }>()

const { user } = useAuth()
const { byId } = useUsers()
const { push } = useToast()

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
    push(e instanceof Error ? e.message : '编辑失败', 'error')
  }
}

async function remove(c: Comment) {
  try {
    await commentApi.remove(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
  } catch (e) {
    push(e instanceof Error ? e.message : '删除失败', 'error')
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
        <textarea
          v-model="editingContent"
          rows="2"
          class="mt-2 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
        />
        <div class="mt-2 flex gap-2">
          <button class="rounded-md bg-primary px-3 py-1 text-xs text-primary-foreground" @click="saveEdit(c)">
            保存
          </button>
          <button class="rounded-md border px-3 py-1 text-xs" @click="editingId = null">取消</button>
        </div>
      </template>
      <p v-else class="mt-1 whitespace-pre-wrap text-sm">{{ c.content }}</p>
      <div v-if="!editingId && isAuthor(c)" class="mt-2 flex gap-2">
        <button class="text-xs text-muted-foreground hover:text-foreground" @click="startEdit(c)">编辑</button>
        <button class="text-xs text-destructive" @click="remove(c)">删除</button>
      </div>
    </div>
  </div>
</template>
