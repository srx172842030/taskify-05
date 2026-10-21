<script setup lang="ts">
import { ref } from 'vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Textarea from '@/components/ui/Textarea.vue'
import Select from '@/components/ui/Select.vue'
import Checkbox from '@/components/ui/Checkbox.vue'
import Switch from '@/components/ui/Switch.vue'
import FormField from '@/components/ui/FormField.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Drawer from '@/components/ui/Drawer.vue'
import DropdownMenu from '@/components/ui/DropdownMenu.vue'
import Table from '@/components/ui/Table.vue'
import Tabs from '@/components/ui/Tabs.vue'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import Skeleton from '@/components/ui/Skeleton.vue'
import Pagination from '@/components/ui/Pagination.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import EmptyState from '@/components/state/EmptyState.vue'
import { useToast } from '@/composables/useToast'

const input = ref('')
const textarea = ref('')
const select = ref('')
const checked = ref(true)
const switched = ref(false)
const dialogOpen = ref(false)
const drawerOpen = ref(false)
const activeTab = ref('a')
const page = ref(1)

const { push } = useToast()

const tableColumns = [
  { key: 'name', label: '名称' },
  { key: 'status', label: '状态' },
]
const tableRows = [
  { name: '条目 1', status: '正常' },
  { name: '条目 2', status: '停用' },
]
</script>

<template>
  <section class="mx-auto max-w-6xl space-y-8 px-4 py-10">
    <header>
      <h1 class="text-2xl font-bold">设计画廊</h1>
      <p class="mt-1 text-sm text-muted-foreground">组件在亮/暗两主题下展示（通过顶部主题切换验证）。</p>
    </header>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">按钮</h2>
      <div class="flex flex-wrap gap-2">
        <Button>主要按钮</Button>
        <Button variant="secondary">次要按钮</Button>
        <Button variant="destructive">危险按钮</Button>
        <Button variant="ghost">幽灵按钮</Button>
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">输入框 / 文本域</h2>
      <div class="grid max-w-xl gap-3 sm:grid-cols-2">
        <Input v-model="input" placeholder="输入框" />
        <Textarea v-model="textarea" placeholder="文本域" />
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">选择器 / 复选框 / 开关</h2>
      <div class="flex flex-wrap items-center gap-4">
        <Select
          v-model="select"
          :options="[
            { value: 'a', label: '选项 A' },
            { value: 'b', label: '选项 B' },
          ]"
          placeholder="请选择"
          class="max-w-[200px]"
        />
        <Checkbox v-model="checked">复选框</Checkbox>
        <Switch v-model="switched" />
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">表单校验提示</h2>
      <FormField label="用户名" error="该用户名已被占用" class="max-w-xs">
        <Input placeholder="输入用户名" />
      </FormField>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">对话框 / 抽屉 / 下拉菜单</h2>
      <div class="flex flex-wrap gap-2">
        <Button variant="secondary" @click="dialogOpen = true">打开对话框</Button>
        <Button variant="secondary" @click="drawerOpen = true">打开抽屉</Button>
        <DropdownMenu>
          <template #trigger>
            <Button variant="secondary">下拉菜单</Button>
          </template>
          <template #content>
            <button class="block w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-muted">操作一</button>
            <button class="block w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-muted">操作二</button>
          </template>
        </DropdownMenu>
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">提示消息</h2>
      <Button variant="secondary" @click="push('操作成功', 'success')">触发 Toast</Button>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">表格</h2>
      <Table :columns="tableColumns" :rows="tableRows" class="max-w-xl" />
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">标签页</h2>
      <Tabs
        v-model="activeTab"
        :tabs="[
          { value: 'a', label: '标签一' },
          { value: 'b', label: '标签二' },
        ]"
        class="max-w-xl"
      >
        <p v-if="activeTab === 'a'" class="text-sm text-muted-foreground">标签一内容</p>
        <p v-else class="text-sm text-muted-foreground">标签二内容</p>
      </Tabs>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">分页</h2>
      <Pagination :page="page" :total-pages="5" @change="page = $event" />
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">卡片 / 徽标 / 骨架屏</h2>
      <div class="grid max-w-xl gap-4 sm:grid-cols-2">
        <Card class="p-4">
          <p class="font-medium">卡片标题</p>
          <p class="mt-1 text-sm text-muted-foreground">卡片内容</p>
          <div class="mt-2 flex gap-2">
            <Badge>徽标</Badge>
            <Badge>示例</Badge>
          </div>
        </Card>
        <Card class="space-y-2 p-4">
          <Skeleton class="h-4 w-48" />
          <Skeleton class="h-4 w-72" />
        </Card>
      </div>
    </div>

    <div class="grid max-w-xl gap-4 sm:grid-cols-2">
      <div>
        <h2 class="mb-2 text-sm font-semibold text-muted-foreground">加载中</h2>
        <Card><LoadingState /></Card>
      </div>
      <div>
        <h2 class="mb-2 text-sm font-semibold text-muted-foreground">空数据</h2>
        <Card><EmptyState /></Card>
      </div>
    </div>

    <Dialog v-model="dialogOpen" title="对话框">
      <p class="text-sm text-muted-foreground">这是一个可复用的对话框组件。</p>
    </Dialog>
    <Drawer v-model="drawerOpen">
      <p class="text-sm text-muted-foreground">这是一个可复用的抽屉组件。</p>
    </Drawer>
  </section>
</template>
