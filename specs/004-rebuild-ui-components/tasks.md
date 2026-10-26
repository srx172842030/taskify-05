# 任务清单：UI 组件体系重建与复用

**输入**：`/specs/004-rebuild-ui-components/` 下的设计文档

**前置文档**：plan.md（必填）、spec.md（必填，用户故事）、research.md、data-model.md、contracts/

**测试**：本特性为重构（无新业务行为），不新增「测试先行」任务；但须**保持既有单元/e2e 测试通过**，并对因组件结构变化而失效的既有测试做最小更新（对应章程原则 II「测试标准」）。

**组织方式**：按用户故事分组，使每个故事可独立实现、独立测试、独立交付。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件，无未完成依赖）
- **[Story]**：所属用户故事（US1/US2/US3）
- 描述含精确文件路径（相对 `frontend/`）

---

## Phase 1: Setup（共享基础设施）

**目的**：初始化 shadcn-vue 与依赖，为安装官方组件做准备。

- [X] T001 在 `frontend/` 初始化 shadcn-vue：生成 `frontend/components.json`（`aliases` 指向 `./src/components`、`./src/lib/utils`、`./src/components/ui`），并按 research.md D1 将 shadcn-vue 相关依赖声明补入 `frontend/package.json`（当前仅在仓库根声明）
- [X] T002 [P] 安装并确认官方组件所需依赖齐全：`reka-ui`、`class-variance-authority`、`clsx`、`tailwind-merge`、`vue-sonner`、`tailwindcss-animate`（写入 `frontend/package.json`）
- [X] T003 [P] 创建 `frontend/src/lib/utils.ts`，导出 `cn()`（`clsx` + `tailwind-merge`）
- [X] T004 [P] 在 `frontend/tailwind.config.js` 的 `plugins` 中追加 `tailwindcss-animate` 插件（现有 shadcn 主题 token 已在 `tokens.css` 与 config 中就位，不重建）

---

## Phase 2: User Story 1 - 重建基础 UI 组件库（优先级：P1）🎯 MVP

**目标**：删除 17 个手写仿制组件，改用官方 registry 的 19 项组件，使基础组件拥有单一可信来源。

**独立测试**：官方组件单独渲染正常、基础交互正常、满足可访问性，且应用仍可构建（详见 spec.md US1）。

- [X] T005 [US1] 删除 `frontend/src/components/ui/` 下全部 17 个手写组件：`Button.vue`、`Input.vue`、`Textarea.vue`、`Select.vue`、`Checkbox.vue`、`Switch.vue`、`FormField.vue`、`Dialog.vue`、`Drawer.vue`、`DropdownMenu.vue`、`Toast.vue`、`Table.vue`、`Tabs.vue`、`Card.vue`、`Badge.vue`、`Skeleton.vue`、`Pagination.vue`
- [X] T006 [US1] 用 MCP `get_add_command_for_items` 返回的安装命令，运行 `pnpm shadcn-vue add @shadcn/button @shadcn/input @shadcn/textarea @shadcn/select @shadcn/checkbox @shadcn/switch @shadcn/form @shadcn/field @shadcn/label @shadcn/dialog @shadcn/drawer @shadcn/dropdown-menu @shadcn/sonner @shadcn/table @shadcn/tabs @shadcn/card @shadcn/badge @shadcn/skeleton @shadcn/pagination`
- [X] T007 [US1] 用 MCP `get_audit_checklist` 核对安装结果：导入方式正确（具名/默认）、依赖齐全、无 lint 告警、无 TypeScript 错误
- [X] T008 [US1] 更新 `frontend/src/App.vue`：移除对已删 `Toast.vue` 的引用，改为 `import { Toaster } from '@/components/ui/sonner'` 并挂载 `<Toaster />`，保持应用可构建
- [X] T009 [P] [US1] 更新或删除 `frontend/tests/unit/Button.spec.ts`（其直接挂载的旧手写 `Button.vue` 已删除；改为对官方 `Button` 的冒烟测试或移除该用例）

**检查点**：User Story 1 完成——官方组件渲染、应用可构建，`tests/unit/Button.spec.ts` 通过或移除。

---

## Phase 3: User Story 2 - 业务组件与布局复用基础组件（优先级：P2）

**目标**：重写 `components/` 与 `layouts/` 下所有业务组件与布局，复用官方 UI 组件，消除自写基础控件与重复实现。

**独立测试**：审查每个业务组件/布局不再自写基础控件；既有功能（登录、项目、看板、任务、通知）行为不变（详见 spec.md US2）。

- [X] T010 [P] [US2] 重写 `frontend/src/components/auth/LoginDialog.vue`：自写按钮/输入/遮罩改为官方 `Dialog` + `Form`/`Field`/`Label` + `Input` + `Button`
- [X] T011 [P] [US2] 重写 `frontend/src/components/project/ProjectCreateForm.vue`：改为官方 `Form` + `Input` + `Button`，并将 `useToast().push` 迁移为 `toast()`（sonner）
- [X] T012 [P] [US2] 重写 `frontend/src/components/board/TaskCard.vue`：自写卡片与状态样式改为官方 `Card` + `Badge`
- [X] T013 [P] [US2] 核查并改造 `frontend/src/components/board/KanbanBoard.vue` 与 `frontend/src/components/board/KanbanColumn.vue`：列容器/卡片复用官方 `Card` 等结构组件，无自写基础控件
- [X] T014 [P] [US2] 重写 `frontend/src/components/notification/NotificationBell.vue`：自写图标按钮改为官方 `Button`（`ghost` + `size`）
- [X] T015 [P] [US2] 重写 `frontend/src/components/notification/NotificationPanel.vue`：改为官方 `DropdownMenu` 或 `Drawer` + `Badge`
- [X] T016 [P] [US2] 重写 `frontend/src/components/task/CommentForm.vue`：改为官方 `Form` + `Textarea` + `Button`，并迁移 `useToast` → `toast()`
- [X] T017 [P] [US2] 重写 `frontend/src/components/task/CommentList.vue`：列表与操作按钮改为官方 `Card`/`Badge` + `Button`，并迁移 `useToast` → `toast()`
- [X] T018 [P] [US2] 重写 `frontend/src/components/task/DependencyEditor.vue`：原生 `<select>` 改为官方 `Select`（组合式），并迁移 `useToast` → `toast()`
- [X] T019 [P] [US2] 重写 `frontend/src/components/task/ReminderEditor.vue`：改为官方 `Form` + `Select` + `Input` + `Button`，并迁移 `useToast` → `toast()`
- [X] T020 [P] [US2] 重写 `frontend/src/components/task/TaskDetailDrawer.vue`：自写抽屉/标签/表单改为官方 `Drawer` + `Tabs` + `Form` 等，并迁移 `useToast` → `toast()`
- [X] T021 [P] [US2] 重写 `frontend/src/components/state/LoadingState.vue`：移除手写脉冲圆点，改为官方 `Skeleton`（`EmptyState.vue` 无基础控件，保持不动）
- [X] T022 [P] [US2] 重写 `frontend/src/layouts/AppLayout.vue`：顶栏/导航按钮改为官方 `Button`/`DropdownMenu`
- [X] T023 [P] [US2] 重写 `frontend/src/layouts/AppDrawer.vue`：改为官方 `Drawer`/`Sheet` + `Button`
- [X] T024 [P] [US2] 更新 `frontend/src/composables/useBoard.ts`：将其内部 `useToast` 调用迁移为 `toast()`（sonner）
- [X] T025 [US2] 更新既有单元测试以匹配新实现：`frontend/tests/unit/CommentList.spec.ts`、`tests/unit/TaskDetailDrawer.spec.ts`、`tests/unit/ProjectCreateForm.spec.ts`、`tests/unit/useBoard.spec.ts`（依赖 T011/T017/T020/T024）
- [X] T026 [US2] 扫描校验：`grep` 确认 `frontend/src/components/` 与 `frontend/src/layouts/` 中 `<button>/<input>/<select>/<textarea>` 与旧组件引用归零（依赖 T010–T024）

**检查点**：User Stories 1 + 2 均可独立工作——业务组件/布局无自写基础控件，既有功能无回归。

---

## Phase 4: User Story 3 - 重写 /design 设计画廊页复用 UI 组件（优先级：P3）

**目标**：`/design` 页面所有界面元素复用官方组件，作为设计系统唯一权威展示。

**独立测试**：页面正常渲染并展示全部官方组件，源码无自写控件/游离样式，手机宽度无横向滚动（详见 spec.md US3）。

- [X] T027 [US3] 重写 `frontend/src/pages/design/DesignGallery.vue`：所有界面元素复用官方组件（`Button`/`Input`/`Textarea`/`Select`/`Checkbox`/`Switch`/`Form`/`Dialog`/`Drawer`/`DropdownMenu`/`Table`/`Tabs`/`Card`/`Badge`/`Skeleton`/`Pagination`），并将 `useToast` → `toast()`
- [X] T028 [US3] 移除 `frontend/src/pages/design/DesignGallery.vue` 内联自定义控件样式，并确保手机宽度（16px 侧边距）下无横向滚动（依赖 T027）
- [X] T029 [P] [US3] 更新 `frontend/e2e/design.spec.ts` 中的选择器，以匹配官方组件（reka-ui）渲染结构（依赖 T027）

**检查点**：所有用户故事独立可用——`/design` 页面 100% 复用官方组件。

---

## Phase 5: Polish & Cross-Cutting Concerns

**目的**：跨故事收尾、最小适配与质量门禁。

- [X] T030 最小适配 `frontend/src/pages/projects/ProjectDetailPage.vue`（更新 `Tabs` 导入路径 + 迁移 `useToast` → `toast()`）与 `frontend/src/pages/projects/ProjectListPage.vue`（如有旧 `components/ui/` 引用一并适配）
- [X] T031 删除 `frontend/src/composables/useToast.ts`（先以 `grep -rn "useToast" src` 确认全仓无残留引用）
- [X] T032 运行质量门禁：`pnpm lint` + `pnpm test` + `pnpm build`（`vue-tsc -b` + `vite build`），确认无缺失依赖、无导入错误、无新增告警
- [X] T033 运行 `pnpm e2e`（如配置），并手工核对关键用户旅程：登录、项目列表/详情、看板、任务详情抽屉、通知、`/design`
- [X] T034 用 MCP `get_audit_checklist` 复查三项：`ui/` 组件来源、业务组件复用情况、`/design` 页面组件使用
- [X] T035 章程符合性自检：原则 III（一致性 + WCAG AA）、VI（文档中文化）、VII（复用 + 无跳过 MCP 调研直接手写；确需自实现处注释 `registry 无此组件`）

---

## 依赖与执行顺序

### 阶段依赖

- **Setup（Phase 1）**：无依赖，可立即开始。
- **User Story 1（Phase 2）**：依赖 Setup 完成；**它是本特性的「基础阶段」，阻塞 US2/US3**（官方组件未安装前，业务组件与页面无从复用）。
- **User Story 2（Phase 3）**：依赖 US1 完成；各文件任务相互独立，可并行。
- **User Story 3（Phase 4）**：依赖 US1 完成；与 US2 相互独立，可并行。
- **Polish（Phase 5）**：依赖 US1/US2/US3 完成（尤其 T031 删除 `useToast.ts` 须等所有引用迁移后）。

### 用户故事依赖

- **US1（P1）**：仅依赖 Setup，不依赖其他故事。
- **US2（P2）**：依赖 US1；与 US3 无相互依赖。
- **US3（P3）**：依赖 US1；与 US2 无相互依赖。

### 故事内顺序

- US1：删除旧组件 → 安装官方组件 → 审计 → 修正 `App.vue` → 修正按钮测试。
- US2：各业务文件迁移（可并行）→ 更新既有测试 → 残留扫描校验。
- US3：重建页面 → 移除内联样式/响应式 → 更新 e2e。

### 并行机会

- Setup 中 T002/T003/T004 可并行。
- US2 中 T010–T024（除 T025/T026）均为不同文件，可并行。
- US2 与 US3 在 US1 完成后可由不同成员并行推进。
- US3 中 T029 与 T028 可并行（不同关注点，T028 同文件依赖 T027 完成后）。

---

## 并行示例：User Story 2

```bash
# 各业务组件文件互不重叠，可并行下发：
任务: "重写 frontend/src/components/auth/LoginDialog.vue ..."
任务: "重写 frontend/src/components/project/ProjectCreateForm.vue ..."
任务: "重写 frontend/src/components/board/TaskCard.vue ..."
任务: "重写 frontend/src/components/task/TaskDetailDrawer.vue ..."
任务: "重写 frontend/src/layouts/AppLayout.vue ..."
# ... 其余 [P] 任务同批并行
```

---

## 实施策略

### MVP 优先（仅 User Story 1）

1. 完成 Phase 1：Setup。
2. 完成 Phase 2：User Story 1（重建基础 UI 组件库）。
3. **停下验证**：官方组件渲染、应用可构建、无缺失依赖。
4. 可视情况演示/交付。

### 增量交付

1. Setup + US1 → 基础组件库就绪。
2. 加 US2 → 业务组件/布局复用 → 验证无回归 → 交付。
3. 加 US3 → `/design` 页面复用 → 验证 → 交付。
4. 每步在不破坏前序故事的前提下增值。

### 并行团队策略

- US1 完成后：
  - 成员 A：US2（业务组件/布局）
  - 成员 B：US3（/design 页面）
- 两者完成后汇合做 Polish（最小适配、删除 `useToast.ts`、质量门禁）。

---

## 备注

- `[P]` = 不同文件、无未完成依赖。
- `[Story]` 标签将任务映射到用户故事，便于追溯。
- 每个用户故事应可独立完成与测试。
- 每次任务或逻辑分组后提交。
- 可在任一检查点停下验证故事独立性。
- 避免：模糊任务、同文件冲突、破坏故事独立性的跨故事依赖。

---

## Phase 6: Convergence

**目的**：收敛 `/speckit-implement` 后 `pnpm e2e` 遗留的 5 处失败（13 处通过）。失败均由 UI 组件重构引入或暴露，涉及抽屉关闭、主题下拉选择器、移动端顶栏与移动端看板单列视图。

- [X] T036 在 `frontend/src/components/task/TaskDetailDrawer.vue` 中，任务经「移动到…」按钮成功移动后关闭抽屉（`open = false`），使 modal Sheet 释放背景 `aria-hidden`、移动后的卡片重新可见，修复 chromium 下 `kanban.spec.ts` 失败 per T020/SC-004 (partial)
- [X] T037 修复 `frontend/e2e/theme.spec.ts` 选择器：主题下拉项现为 `DropdownMenuItem`（role=menuitem），将 `getByRole('button', { name: '暗色' })` 改为 `getByRole('menuitem', { name: '暗色' })`，与 `design.spec.ts` 保持一致 per T022/US2 (partial)
- [X] T038 修复 `frontend/src/layouts/AppLayout.vue` 移动端顶栏溢出：窄屏下右侧（主题/通知/用户名/登出）溢出导致「登出」按钮被父容器拦截无法点击，确保手机宽度下「登出」按钮可达 per T022/FR-008 (partial)
- [X] T039 修复 `frontend/src/pages/projects/ProjectDetailPage.vue` 移动端看板单列视图：任务移动到其他列后更新 `mobileColumn`（使视图跟随移动），或调整 `kanban.spec.ts` 移动端断言，使卡片在移动端仍可见 per FR-008/SC-004 (partial)
