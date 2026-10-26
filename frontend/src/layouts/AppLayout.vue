<script setup lang="ts">
import { ref } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { useAuth } from '@/stores/auth'
import { LogIn, Menu, Monitor, Moon, Sun } from 'lucide-vue-next'
import AppDrawer from './AppDrawer.vue'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

const { mode, setTheme } = useTheme()
const { isAuthenticated, user, logout } = useAuth()
const drawerOpen = ref(false)
const loginOpen = ref(false)

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
        <Button variant="ghost" size="icon" class="md:hidden" aria-label="打开导航" @click="drawerOpen = true">
          <Menu />
        </Button>

        <RouterLink to="/" class="text-lg font-semibold">Taskify</RouterLink>

        <nav class="hidden items-center gap-4 text-sm md:flex">
          <RouterLink to="/" class="text-muted-foreground hover:text-foreground">首页</RouterLink>
          <RouterLink to="/projects" class="text-muted-foreground hover:text-foreground">项目</RouterLink>
        </nav>

        <div class="ml-auto flex items-center gap-2">
          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="outline" size="icon" aria-label="切换主题">
                <Sun v-if="mode === 'light'" />
                <Moon v-else-if="mode === 'dark'" />
                <Monitor v-else />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem v-for="o in themeOptions" :key="o.value" @select="setTheme(o.value)">
                <component :is="o.icon" class="h-4 w-4" />
                {{ o.label }}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <template v-if="isAuthenticated">
            <NotificationBell />
            <span class="hidden text-sm text-muted-foreground sm:inline">{{ user?.username }}</span>
            <Button variant="outline" @click="logout()">登出</Button>
          </template>
          <Button v-else @click="loginOpen = true">
            <LogIn />
            登录
          </Button>
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
