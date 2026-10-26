# UI 组件契约：业务组件必须遵循的官方组件接口

**特性**：[../spec.md](../spec.md) | **阶段**：Phase 1（设计）

本文件定义重构后业务组件、布局与页面与基础 UI 组件之间的**接口契约**：即业务代码只能使用 shadcn-vue 官方组件提供的公开 API，不得自写基础控件或绕过官方组件直接拼 HTML 控件。该契约是「UI contract for applications」——它约束的是应用内部各层之间的调用面，而非外部接口。

## 契约总则

1. **来源唯一**：任何按钮、输入、选择、开关、弹层、表格、标签、卡片、徽标、骨架、分页、提示等基础控件，必须引用 `components/ui/` 下的官方组件，禁止在业务组件内用 `<button>`/`<input>`/`<select>`/`<textarea>` 等原生标签拼出等价控件。
2. **组合式使用**：官方组件采用组合式 API，业务代码通过官方导出的子组件（如 `SelectTrigger`、`TabsTrigger`、`DialogContent`）组合使用，不得再包一层自定义的 `options`/`tabs` 数组接口。
3. **扩展而非复制**：能力不足时组合或扩展官方组件；仅在 MCP 调研确认 registry 确无对应组件后，方可在 `components/ui/` 之外自实现，并注释 `registry 无此组件`（章程原则 VII）。
4. **可访问性**：使用官方组件即继承其键盘导航、ARIA 语义；业务代码不得剥离这些属性。

## 组件契约表（业务代码可用接口）

| 官方组件 | 业务代码应使用的关键导出/用法 | 禁止的替代做法 |
|---------|------------------------------|--------------|
| `button` | `Button`（`variant`、`size`、`as-child`） | 自写 `<button>` + 手写 hover 样式 |
| `input` | `Input`（`v-model`） | 自写 `<input>` |
| `textarea` | `Textarea`（`v-model`） | 自写 `<textarea>` |
| `select` | `Select`/`SelectTrigger`/`SelectContent`/`SelectItem` | 原生 `<select>` |
| `checkbox` | `Checkbox`（`v-model:checked`） | 自写 `<input type=checkbox>` |
| `switch` | `Switch`（`v-model:checked`） | 自写开关 |
| `form`/`field`/`label` | `FormField`/`FormItem`/`FormLabel`/`FormControl`/`FormMessage`（配合 vee-validate） | 自写 `FormField.vue` |
| `dialog` | `Dialog`/`DialogTrigger`/`DialogContent`/`DialogHeader`/`DialogTitle`/`DialogDescription`/`DialogFooter` | 自写遮罩 + `Transition` |
| `drawer` | `Drawer`/`DrawerTrigger`/`DrawerContent`/`DrawerHeader`/`DrawerTitle` | 自写侧滑面板 |
| `dropdown-menu` | `DropdownMenu`/`DropdownMenuTrigger`/`DropdownMenuContent`/`DropdownMenuItem` | 自写下拉 |
| `sonner` | `<Toaster />` + `toast()` / `toast.success()` / `toast.error()` | 自写 `useToast` + `Toast.vue` |
| `table` | `Table`/`TableHeader`/`TableBody`/`TableRow`/`TableCell` | 自写表格布局 |
| `tabs` | `Tabs`/`TabsList`/`TabsTrigger`/`TabsContent` | 自写标签切换 |
| `card` | `Card`/`CardHeader`/`CardTitle`/`CardContent`/`CardFooter` | 自写卡片容器 |
| `badge` | `Badge`（`variant`） | 自写徽标样式 |
| `skeleton` | `Skeleton` | 自写脉冲占位 |
| `pagination` | `Pagination`/`PaginationItem`/`PaginationPrevious`/`PaginationNext` | 自写分页按钮 |

## 迁移映射（旧自写 API → 官方 API）

| 旧用法（将被删除） | 新用法（官方组件） |
|------------------|------------------|
| `Button` `variant: 'default'|'secondary'|'destructive'|'ghost'` | `Button` 官方 `variant`（新增 `outline`/`link`） |
| `Input`/`Textarea` 直接绑定 | `Input`/`Textarea` 官方（`v-model` 语义一致） |
| `Select` 传 `options` 数组 + `v-model` | `Select` + `SelectItem` 循环渲染 |
| `FormField` 传 `label`/`error` | `FormField` + `FormLabel` + `FormMessage`（vee-validate） |
| `Dialog` 传 `title` + `v-model:open` | `Dialog` + `DialogTitle` + 受控 `open` |
| `Tabs` 传 `tabs` 数组 + `v-model` | `Tabs` + `TabsList`/`TabsTrigger`/`TabsContent` |
| `useToast().push(msg, type)` | `toast(msg)` / `toast.success` / `toast.error` |
| `LoadingState` 手写脉冲圆点 | `Skeleton` |

## 校验方式

- 静态：`grep` 扫描业务组件/布局/页面中的 `<button>/<input>/<select>/<textarea>` 与旧组件导入，确保归零。
- 构建：`vue-tsc -b` + `vite build` 通过，无缺失依赖/导入错误。
- 运行时：Playwright 核对关键用户旅程（登录、看板、任务详情、通知、/design）。
