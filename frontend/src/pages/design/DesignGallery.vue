<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import LoadingState from '@/components/state/LoadingState.vue'
import EmptyState from '@/components/state/EmptyState.vue'

const input = ref('')
const textarea = ref('')
const select = ref('')
const checked = ref(true)
const switched = ref(false)
const dialogOpen = ref(false)
const drawerOpen = ref(false)
const activeTab = ref('a')
const page = ref(1)
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
        <Button variant="outline">描边按钮</Button>
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
        <Select v-model="select">
          <SelectTrigger class="max-w-[200px]">
            <SelectValue placeholder="请选择" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="a">选项 A</SelectItem>
            <SelectItem value="b">选项 B</SelectItem>
          </SelectContent>
        </Select>
        <label class="flex items-center gap-2 text-sm">
          <Checkbox v-model="checked" /> 复选框
        </label>
        <Switch v-model="switched" aria-label="开关" />
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">表单校验提示</h2>
      <div class="max-w-xs space-y-1">
        <Label for="demo-username">用户名</Label>
        <Input id="demo-username" placeholder="输入用户名" />
        <p class="text-xs text-destructive">该用户名已被占用</p>
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">对话框 / 抽屉 / 下拉菜单</h2>
      <div class="flex flex-wrap gap-2">
        <Button variant="secondary" @click="dialogOpen = true">打开对话框</Button>
        <Button variant="secondary" @click="drawerOpen = true">打开抽屉</Button>
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="secondary">下拉菜单</Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem>操作一</DropdownMenuItem>
            <DropdownMenuItem>操作二</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">提示消息</h2>
      <Button variant="secondary" @click="toast.success('操作成功')">触发 Toast</Button>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">表格</h2>
      <Table class="max-w-xl">
        <TableHeader>
          <TableRow>
            <TableHead>名称</TableHead>
            <TableHead>状态</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell>条目 1</TableCell>
            <TableCell>正常</TableCell>
          </TableRow>
          <TableRow>
            <TableCell>条目 2</TableCell>
            <TableCell>停用</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">标签页</h2>
      <Tabs v-model="activeTab" class="max-w-xl">
        <TabsList>
          <TabsTrigger value="a">标签一</TabsTrigger>
          <TabsTrigger value="b">标签二</TabsTrigger>
        </TabsList>
        <TabsContent value="a" class="text-sm text-muted-foreground">标签一内容</TabsContent>
        <TabsContent value="b" class="text-sm text-muted-foreground">标签二内容</TabsContent>
      </Tabs>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">分页</h2>
      <Pagination v-model:page="page" :items-per-page="1" :total="5" v-slot="{ pageCount }">
        <PaginationContent>
          <PaginationPrevious />
          <PaginationItem v-for="n in pageCount" :key="n" :value="n" :is-active="n === page">
            {{ n }}
          </PaginationItem>
          <PaginationNext />
        </PaginationContent>
      </Pagination>
    </div>

    <div>
      <h2 class="mb-2 text-sm font-semibold text-muted-foreground">卡片 / 徽标 / 骨架屏</h2>
      <div class="grid max-w-xl gap-4 sm:grid-cols-2">
        <Card>
          <CardContent>
            <p class="font-medium">卡片标题</p>
            <p class="mt-1 text-sm text-muted-foreground">卡片内容</p>
            <div class="mt-2 flex gap-2">
              <Badge>徽标</Badge>
              <Badge variant="secondary">示例</Badge>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent class="space-y-2">
            <Skeleton class="h-4 w-48" />
            <Skeleton class="h-4 w-72" />
          </CardContent>
        </Card>
      </div>
    </div>

    <div class="grid max-w-xl gap-4 sm:grid-cols-2">
      <div>
        <h2 class="mb-2 text-sm font-semibold text-muted-foreground">加载中</h2>
        <Card><CardContent><LoadingState /></CardContent></Card>
      </div>
      <div>
        <h2 class="mb-2 text-sm font-semibold text-muted-foreground">空数据</h2>
        <Card><CardContent><EmptyState /></CardContent></Card>
      </div>
    </div>

    <Dialog v-model:open="dialogOpen">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>对话框</DialogTitle>
          <DialogDescription>这是一个可复用的对话框组件。</DialogDescription>
        </DialogHeader>
      </DialogContent>
    </Dialog>

    <Drawer v-model:open="drawerOpen">
      <DrawerContent>
        <DrawerHeader>
          <DrawerTitle>抽屉</DrawerTitle>
        </DrawerHeader>
        <p class="px-6 pb-6 text-sm text-muted-foreground">这是一个可复用的抽屉组件。</p>
      </DrawerContent>
    </Drawer>
  </section>
</template>
