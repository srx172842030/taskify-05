<script setup lang="ts">
import { ref } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { useAuth } from '@/stores/auth'
import { LogIn, Menu, Monitor, Moon, Sun } from 'lucide-vue-next'
import AppDrawer from './AppDrawer.vue'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import NotificationBell from '@/components/notification/NotificationBell.vue'

const { mode, setTheme } = useTheme()
const { isAuthenticated, user, logout } = useAuth()
const drawerOpen = ref(false)
const loginOpen = ref(false)
const themeOpen = ref(false)

const themeOptions = [
  { value: 'light' as const, label: '亮色', icon: Sun },
  { value: 'dark' as const, label: '暗色', icon: Moon },
  { value: 'system' as const, label: '跟随系统', icon: Monitor },
]
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <header class="sticky top-0 z-30 border-b bg-card">
      <div class="mx-auto flex h-14 max-w-6xl items-center gap-4 px-4">
        <button class="md:hidden" aria-label="打开导航" @click="drawerOpen = true">
          <Menu class="h-5 w-5" />
        </button>

        <RouterLink to="/" class="text-lg font-semibold">Taskify</RouterLink>

        <nav class="hidden items-center gap-4 text-sm md:flex">
          <RouterLink to="/" class="text-muted-foreground hover:text-foreground">
            首页
          </RouterLink>
          <RouterLink to="/projects" class="text-muted-foreground hover:text-foreground">
            项目
          </RouterLink>
        </nav>

        <div class="ml-auto flex items-center gap-2">
          <div class="relative">
            <button
              class="flex h-9 items-center gap-1 rounded-md border px-2 text-sm"
              aria-label="切换主题"
              @click="themeOpen = !themeOpen"
            >
              <Sun v-if="mode === 'light'" class="h-4 w-4" />
              <Moon v-else-if="mode === 'dark'" class="h-4 w-4" />
              <Monitor v-else class="h-4 w-4" />
            </button>
            <div
              v-if="themeOpen"
              class="absolute right-0 mt-1 w-32 rounded-md border bg-popover p-1 shadow-md"
            >
              <button
                v-for="o in themeOptions"
                :key="o.value"
                class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted"
                @click="setTheme(o.value); themeOpen = false"
              >
                <component :is="o.icon" class="h-4 w-4" />
                {{ o.label }}
              </button>
            </div>
          </div>

          <template v-if="isAuthenticated">
            <NotificationBell />
            <span class="text-sm text-muted-foreground">{{ user?.username }}</span>
            <button
              class="h-9 rounded-md border px-3 text-sm"
              @click="logout()"
            >
              登出
            </button>
          </template>
          <button
            v-else
            class="flex h-9 items-center gap-1 rounded-md bg-primary px-3 text-sm text-primary-foreground"
            @click="loginOpen = true"
          >
            <LogIn class="h-4 w-4" />
            登录
          </button>
        </div>
      </div>
    </header>

    <AppDrawer :open="drawerOpen" @close="drawerOpen = false" />

    <main class="flex-1">
      <slot />
    </main>

    <footer class="border-t py-6 text-center text-sm text-muted-foreground">
      Taskify © 2026
    </footer>

    <LoginDialog v-model="loginOpen" />
  </div>
</template>
