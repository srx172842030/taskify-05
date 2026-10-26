# 数据模型：UI 组件体系重建与复用

**特性**：[spec.md](./spec.md) | **阶段**：Phase 1（设计）

本特性为纯前端代码结构重构，**不引入新的持久化数据**。此处的「数据模型」为**组件清单与映射关系**，用于界定重构范围与核对迁移完整性（对应规格「关键实体」）。

## 实体：基础 UI 组件（17 类 → 19 个 registry 项）

来源：shadcn-vue 官方 registry（`@shadcn`）。目标目录：`frontend/src/components/ui/`。

| # | 逻辑组件 | 官方 registry 项 | 旧文件（将删除） | 关键能力 |
|---|---------|----------------|----------------|---------|
| 1 | 按钮 | `button` | `Button.vue` | variant、disabled、as-child |
| 2 | 输入框 | `input` | `Input.vue` | v-model、type、placeholder |
| 3 | 文本域 | `textarea` | `Textarea.vue` | v-model、多行 |
| 4 | 选择器 | `select` | `Select.vue` | Trigger/Content/Item 组合式 |
| 5 | 复选框 | `checkbox` | `Checkbox.vue` | v-model:checked |
| 6 | 开关 | `switch` | `Switch.vue` | v-model:checked |
| 7 | 表单 | `form`+`field`+`label` | `FormField.vue` | 与 vee-validate/zod 集成 |
| 8 | 对话框 | `dialog` | `Dialog.vue` | Root/Trigger/Content/Header/Title/Footer |
| 9 | 抽屉 | `drawer` | `Drawer.vue` | 同上组合式 |
| 10 | 下拉菜单 | `dropdown-menu` | `DropdownMenu.vue` | Trigger/Content/Item/Separator |
| 11 | 提示消息 | `sonner` | `Toast.vue`+`useToast.ts` | Toaster + toast() |
| 12 | 表格 | `table` | `Table.vue` | Table/Caption/Header/Body/Row/Cell |
| 13 | 标签页 | `tabs` | `Tabs.vue` | List/Trigger/Content |
| 14 | 卡片 | `card` | `Card.vue` | Header/Title/Description/Content/Footer |
| 15 | 徽标 | `badge` | `Badge.vue` | variant |
| 16 | 骨架屏 | `skeleton` | `Skeleton.vue` | 加载占位 |
| 17 | 分页 | `pagination` | `Pagination.vue` | Prev/Item/Next |

**约束**：全部 17 类必须来自官方 registry；`components/ui/` 内不得保留任何手写仿制品（FR-003）。

## 实体：业务/状态组件（14 个，重写复用）

目录 `frontend/src/components/`，按领域分组；「自写控件数」为当前 `<button>/<input>/<select>/<textarea>` 出现次数，迁移后应归零。

| 组件 | 路径 | 自写控件数 | 迁移要点 |
|------|------|-----------|---------|
| 登录对话框 | `auth/LoginDialog.vue` | 7 | Dialog + Form + Input + Button |
| 看板 | `board/KanbanBoard.vue` | 0（复用 TaskCard） | 结构组件，核对子组件 |
| 看板列 | `board/KanbanColumn.vue` | 0 | Card 容器 |
| 任务卡片 | `board/TaskCard.vue` | 1 | Card + Badge |
| 通知铃铛 | `notification/NotificationBell.vue` | 1 | Button（图标按钮） |
| 通知面板 | `notification/NotificationPanel.vue` | 2 | Dropdown 或 Drawer + Badge |
| 项目创建表单 | `project/ProjectCreateForm.vue` | 4 | Form + Input + Button |
| 加载态 | `state/LoadingState.vue` | 0（手写脉冲点） | Skeleton |
| 空态 | `state/EmptyState.vue` | 0（无基础控件） | 保留，不迁移 |
| 评论表单 | `task/CommentForm.vue` | 2 | Form + Textarea + Button |
| 评论列表 | `task/CommentList.vue` | 5 | 列表 + Badge + Button |
| 依赖编辑器 | `task/DependencyEditor.vue` | 3 | Select + Button |
| 提醒编辑器 | `task/ReminderEditor.vue` | 7 | Form + Select + Input + Button |
| 任务详情抽屉 | `task/TaskDetailDrawer.vue` | 10 | Drawer + Tabs + Form + 多控件 |

## 实体：布局（2 个，重写复用）

| 布局 | 路径 | 自写控件数 | 迁移要点 |
|------|------|-----------|---------|
| 应用布局 | `layouts/AppLayout.vue` | 5 | 顶栏/导航按钮、侧边栏 → Button/Dropdown |
| 应用抽屉 | `layouts/AppDrawer.vue` | 1 | Drawer/Sheet + Button |

## 实体：页面（范围区分）

| 页面 | 处置 | 说明 |
|------|------|------|
| `/design`（`pages/design/DesignGallery.vue`） | **完整重写** | 复用全部官方组件，移除内联自定义控件样式 |
| `pages/projects/ProjectDetailPage.vue` | 最小适配 | 更新 `Tabs` 导入路径；8 处自写控件属范围外 |
| `pages/projects/ProjectListPage.vue` | 最小适配 | 1 处自写控件属范围外 |
| 其他页面 | 不动 | 无 `components/ui/` 引用 |

## 关系

- 业务组件、布局、`/design` 页面 **依赖** 基础 UI 组件（单向，禁止反向依赖）。
- `composables/useToast.ts` 被 `sonner` 替代后删除；其调用点改为 `toast()`。
- `App.vue` 从挂载 `<Toast />` 改为挂载 `<Toaster />`。

## 完整性核对规则（与成功标准对应）

- SC-001：`components/ui/` 下 19 项全部来自官方 registry，手写仿制品 = 0。
- SC-002：上表业务组件与布局的「自写控件数」在重构后全部归零（`grep` 复核）。
- SC-003：`/design` 页面界面元素 100% 复用官方组件，无内联自定义控件样式。
