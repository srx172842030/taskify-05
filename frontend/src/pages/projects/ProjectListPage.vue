<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useProjects } from '@/stores/projects'
import { useAuth } from '@/stores/auth'
import ProjectCreateForm from '@/components/project/ProjectCreateForm.vue'

const { projects, loading, load } = useProjects()
const { isAuthenticated } = useAuth()
const showCreate = ref(false)

onMounted(() => {
  if (isAuthenticated.value) load().catch(() => {})
})
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 py-8">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">项目</h1>
      <button
        v-if="isAuthenticated"
        class="rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground"
        @click="showCreate = true"
      >
        新建项目
      </button>
    </div>

    <p v-if="!isAuthenticated" class="mt-4 text-muted-foreground">请先登录后使用。</p>
    <p v-else-if="loading" class="mt-6 text-muted-foreground">加载中…</p>
    <div v-else class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <RouterLink
        v-for="p in projects"
        :key="p.id"
        :to="`/projects/${p.id}`"
        class="rounded-lg border p-4 hover:border-primary"
      >
        <h2 class="font-semibold">{{ p.name }}</h2>
        <p class="mt-1 text-sm text-muted-foreground">{{ p.description }}</p>
      </RouterLink>
    </div>

    <ProjectCreateForm v-model="showCreate" @created="showCreate = false" />
  </section>
</template>
