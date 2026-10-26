# 研究：UI 组件体系重建与复用

**特性**：[spec.md](./spec.md) | **阶段**：Phase 0（研究）

本文件记录本特性的关键技术决策，用于消除规格中的不确定项。所有结论经代码库调研与 MCP 工具（shadcn-vue registry）核实。

## D1. shadcn-vue 初始化位置与目标目录

- **决策**：以 `frontend/` 为目标目录初始化 shadcn-vue，`components.json` 落于 `frontend/components.json`，其 `aliases` 指向 `./src/components`、`./src/lib/utils`、`./src/components/ui` 等；将 shadcn-vue 相关依赖补入 `frontend/package.json`（当前仅仓库根声明了 `shadcn-vue` devDependency，frontend 未声明）。
- **理由**：`components.json` 需与 Tailwind 配置、样式入口及源码同目录；`frontend/` 是自包含子项目（独立 package.json / pnpm-lock），官方组件与 `cn()` 工具均落在 `frontend/src/` 下。仓库根不是 pnpm workspace，无法让 frontend 直接复用根级 node_modules 的 shadcn-vue 二进制。
- **备选方案**：
  - A. 在仓库根运行 `shadcn-vue` 并用 `--cwd frontend` 或把 `components.json` 的 aliases 指向 `frontend/src/...`：可行但配置耦合跨层级，易混淆，且 Tailwind/CSS 路径需越级引用，维护成本高，弃用。
  - B. 仅保留仓库根安装、frontend 用 `npx shadcn-vue@latest` 临时执行：破坏可复现性，弃用。

## D2. 官方组件清单与映射（17 逻辑组件 → 19 个 registry 项）

- **决策**：安装下列 19 个官方 registry 项，对应规格中的 17 类组件：

  | 逻辑组件 | 官方 registry 项 | 当前手写文件 | 备注 |
  |---------|----------------|------------|------|
  | 按钮 | `button` | `Button.vue` | 官方多 `outline`/`link` 变体 |
  | 输入框 | `input` | `Input.vue` | |
  | 文本域 | `textarea` | `Textarea.vue` | |
  | 选择器 | `select` | `Select.vue` | API 由「options 数组」变为组合式 |
  | 复选框 | `checkbox` | `Checkbox.vue` | |
  | 开关 | `switch` | `Switch.vue` | |
  | 表单 | `form` + `field` + `label` | `FormField.vue` | 表单拆为 form/field/label 三项 |
  | 对话框 | `dialog` | `Dialog.vue` | API 由 open/title 变为组合式 |
  | 抽屉 | `drawer` | `Drawer.vue` | 同上，组合式 |
  | 下拉菜单 | `dropdown-menu` | `DropdownMenu.vue` | 组合式 |
  | 提示消息 | `sonner` | `Toast.vue` + `useToast` | 自定义 toast 替换为 vue-sonner |
  | 表格 | `table` | `Table.vue` | |
  | 标签页 | `tabs` | `Tabs.vue` | `tabs` 数组变为 TabsList/TabsTrigger/TabsContent |
  | 卡片 | `card` | `Card.vue` | |
  | 徽标 | `badge` | `Badge.vue` | |
  | 骨架屏 | `skeleton` | `Skeleton.vue` | |
  | 分页 | `pagination` | `Pagination.vue` | |

- **理由**：`form` 在 shadcn-vue 中依赖独立的 `field` 与 `label` 能力；`sonner` 是官方 toast 方案（vue-sonner），替代现有自写 `useToast`。MCP `get_add_command_for_items` 已核实全部 19 项均在 `@shadcn` registry 中存在。
- **安装命令**（由 MCP 工具返回）：
  `pnpm shadcn-vue add @shadcn/button @shadcn/input @shadcn/textarea @shadcn/select @shadcn/checkbox @shadcn/switch @shadcn/form @shadcn/field @shadcn/label @shadcn/dialog @shadcn/drawer @shadcn/dropdown-menu @shadcn/sonner @shadcn/table @shadcn/tabs @shadcn/card @shadcn/badge @shadcn/skeleton @shadcn/pagination`

## D3. API 迁移策略（数据驱动 → 组合式）

- **决策**：业务组件迁移到官方组件时，将现有「数据驱动」简写 API 改写为官方「组合式」API；业务组件对外的 props/slots/事件保持不变，仅内部实现改用官方组件。
- **理由**：现有手写组件暴露的是简化接口（如 `Select` 的 `options` 数组、`Tabs` 的 `tabs` 数组、`Dialog` 的 `open`/`title`），而官方组件采用 `SelectTrigger/SelectContent/SelectItem`、`TabsList/TabsTrigger/TabsContent`、`DialogRoot/DialogTrigger/DialogContent/...` 的组合式结构。二者无法直接平替，必须逐点改写调用处。
- **备选方案**：
  - A. 在官方组件外包一层薄封装、保留原 `options`/`tabs` 接口：会新增一套「非官方」抽象，违反章程「单一实现来源」，且封装层自身又成维护点，弃用。
  - B. 保留手写组件、仅补依赖：违背本特性核心目标（消除仿制品），弃用。

## D4. 提示消息（Toast）迁移方案

- **决策**：用官方 `sonner`（`Toaster` + `toast()`）替换自写 `Toast.vue` 与 `useToast` 组合式函数；`App.vue` 中挂载 `<Toaster />`；调用点由 `useToast().push(...)` 迁移为 `toast(...)`。
- **理由**：官方方案提供可访问性、键盘交互与定位控制，避免自写。现有 `useToast` 支持 `default/success/error` 三种类型与自动 3 秒关闭，sonner 均原生支持（`toast.success` / `toast.error` / `duration`）。
- **备选方案**：保留 `useToast` 作为 sonner 的薄适配层。仅在调用点众多、逐点替换风险高时采用；默认直接替换以消除中间层。

## D5. 状态组件的处置

- **决策**：`LoadingState` 内部手写脉冲圆点改为复用 `Skeleton`；`EmptyState` 无基础控件（仅文案 + 图标），保留为业务/状态组件，不迁入 `ui/`。
- **理由**：`LoadingState` 属于「自写基础控件」（加载骨架），应复用 `Skeleton`；`EmptyState` 不实现任何基础控件，无需迁移，符合章程「仅在确无可复用组件时新增」。二者均留在 `components/state/`。

## D6. 页面范围边界

- **决策**：核心重写范围为 `components/`、`layouts/`、`/design` 页面。其余页面（`ProjectDetailPage`、`ProjectListPage`、`HomeView`、`error/*`）仅做最小适配——若引用了被删除的 `components/ui/` 旧组件（当前仅 `ProjectDetailPage` 引用 `Tabs.vue`），则更新导入路径；不改其业务行为。
- **理由**：规格明确将完整重写限定于 `components/`、`layouts/` 与 `/design`；其余页面的自写控件（`ProjectDetailPage` 8 处、`ProjectListPage` 1 处）属规格边界外，留待后续特性处理，以免本次重构范围失控。
- **备注**：`ProjectDetailPage` 的 `Tabs` 引用必须随 `ui/Tabs.vue` 删除而更新，否则构建失败——这是「最小适配」的强制项。

## D7. 依赖与工具链

- **决策**：补入 `reka-ui`、`class-variance-authority`、`clsx`、`tailwind-merge`、`tailwindcss-animate`（tailwind 插件）、`vue-sonner`；创建 `src/lib/utils.ts` 导出 `cn()`；`tailwind.config.js` 增加 `tailwindcss-animate` 插件（现有 shadcn 主题 token 已在 `tokens.css` 与 tailwind 配置中就位，无需重建）。
- **理由**：官方 shadcn-vue 组件的实现依赖上述包与 `cn()`；`lucide-vue-next`、vee-validate/zod 已存在，无需重复安装。
- **审计方式**：安装后使用 MCP `get_audit_checklist` 返回的清单核对（导入方式、依赖齐全、无 lint 告警、无 TS 错误）。

## 审计清单（MCP 工具返回）

- [ ] 确保导入正确（具名 vs 默认导入）
- [ ] 确保所有依赖已安装
- [ ] 检查 lint 错误或告警
- [ ] 检查 TypeScript 错误
- [ ] 使用 Playwright（如可用）做浏览器级验证

## 关键风险与对策

| 风险 | 对策 |
|------|------|
| 组合式 API 迁移导致大量调用点改动、易漏改 | 用 `grep` 扫描 `<button>/<input>/<select>/<textarea>` 残留；逐文件替换后即时核对；以构建通过为门禁 |
| 删除 `ui/` 后有旧导入残留导致构建失败 | 全量 `vue-tsc -b` + `vite build` 把关，`grep -rn "components/ui/"` 复核 |
| `Select`/`Tabs`/`Dialog`/`Drawer`/`Dropdown` API 差异大 | 先迁移 API 差异大的组件调用点，逐个验证交互后再推进 |
| 迁移引入行为回归 | 既有单元/e2e 测试 + 手工核对关键用户旅程（登录、看板、任务详情、通知） |
