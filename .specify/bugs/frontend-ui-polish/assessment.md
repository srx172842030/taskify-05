# Bug Assessment: 前端 UI/UX 打磨（6 项）

- **Slug**: frontend-ui-polish
- **Created**: 2026-10-22
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium（批量最高项为 medium；详见各条）

## Report（原文）

1. 前端移动版布局下，通知窗口超出屏幕边缘（仍旧是桌面版样式）。
2. 任务详情里点击切换到相邻列时，没有刷新任务状态和列表。
3. 前端暗色模式下，边框纯白色过于刺眼。
4. 前端切换项目时有旧数据残留带来的闪烁现象。
5. 导航栏按钮大小没有统一。
6. 登出没有确认弹窗。

## Findings 总览

| # | 问题 | 严重度 | 根因类型 |
|---|------|--------|---------|
| F1 | 移动端通知窗口溢出屏幕 | medium | 定位/宽度未做移动端适配 |
| F2 | 任务详情相邻列移动后状态不刷新 | medium | 抽屉 `task` prop 为陈旧对象 |
| F3 | 暗色模式边框纯白刺眼 | low | 裸 `border` 未用 token 色 |
| F4 | 切换项目旧数据闪烁 | medium | 路由复用 + 未重置/未监听参数 |
| F5 | 导航栏按钮尺寸不统一 | low | 各按钮 padding 不一致 |
| F6 | 登出无确认弹窗 | low | 缺少确认交互 |

---

## F1 移动端通知窗口溢出屏幕

**Symptom**：手机宽度下，点开通知铃铛，下拉面板仍按桌面 `w-80`（320px）固定宽度展开，`absolute right-0` 相对铃铛定位，导致面板左侧或右侧超出屏幕边缘。

**Suspected Code Paths**：
- `frontend/src/components/notification/NotificationPanel.vue` — 根节点 `class="absolute right-0 z-50 mt-1 w-80 max-w-[calc(100vw-2rem)] ..."`。`w-80` 是桌面固定宽，`right-0` 相对铃铛容器定位；移动端铃铛并非紧贴屏幕最右（其左侧还有用户名/登出等），面板向左展开 320px 会越过左缘。

**Root Cause Hypothesis**（高置信）：面板定位锚点是铃铛容器而非视口，宽度写死 `w-80`，`max-w-[calc(100vw-2rem)]` 只限制宽度不修正横向偏移，移动端未降级为视口级定位。

**Proposed Remediation**：
- 移动端（`md` 以下）改为 `fixed inset-x-4 top-16`（相对视口，左右各留 16px 边距），桌面保留 `absolute right-0 w-80`。可用 Tailwind 断点组合 `absolute right-0 w-80 max-w-[calc(100vw-2rem)]` + `fixed inset-x-4 top-16`（加 `md:absolute md:w-80` 等），或直接用 `useMediaQuery` 分支。
- 备选：给面板外层加 `fixed` 定位并 `left-1/2 -translate-x-1/2` 居中，但不如右侧对齐自然。

---

## F2 任务详情相邻列移动后状态不刷新

**Symptom**：在任务详情抽屉点击「移动到「进行中」」后，抽屉里的「当前列」与相邻列按钮不更新（仍显示移动前状态），需重开抽屉才正确。

**Suspected Code Paths**：
- `frontend/src/pages/projects/ProjectDetailPage.vue` — `selectedTask = ref<Task|null>(null)` 保存的是点击时的任务对象；`openTask(t)` 赋值后传给抽屉。
- `frontend/src/components/task/TaskDetailDrawer.vue` — `props.task` 是陈旧对象，`STATUS_LABELS[task.status]` 与 `adjacentStatuses(task.status)` 都基于旧 `status`。
- `frontend/src/composables/useBoard.ts` — `moveTask` 通过 `useTasks().upsert(updated)` 更新了 store 的 `tasks` 数组，但**没有**把更新后的任务回传给 `selectedTask`。

**Root Cause Hypothesis**（高置信）：`moveTask` 更新了 store（看板因此正确刷新），但 `selectedTask`（以及抽屉的 `task` prop）仍指向移动前的对象，未做同步。

**Proposed Remediation**：
- 在 `ProjectDetailPage` 里 `watch(tasks, ...)`：当 `selectedTask.id` 在 store 中找到新对象时更新 `selectedTask`；或让 `useBoard.moveTask` 返回更新后的 `Task`，由抽屉 `emit('updated', task)` 回传给页面更新 `selectedTask`。
- 最简单：抽屉内部改用「按 id 从 store 取当前任务」的 `computed`，而非依赖传入的陈旧 `task`。

---

## F3 暗色模式边框纯白刺眼

**Symptom**：暗色模式下，卡片/面板/按钮边框呈近纯白，刺眼、与暗色主题不协调。

**Suspected Code Paths**：
- `frontend/tailwind.config.js` — `colors.extend` 定义了 `border: 'var(--border)'`、`input: 'var(--input)'`（暗色下 `--border` 已是深灰），但**未覆盖 `borderColor.DEFAULT`**。
- 大量组件使用裸 `border`（仅 `border-width`，颜色走 Tailwind preflight 的默认 `borderColor.DEFAULT = gray-200` ≈ `#e5e7eb`），如 `frontend/src/layouts/AppLayout.vue`、`frontend/src/components/ui/Dialog.vue`、`frontend/src/components/board/*.vue`、`frontend/src/components/task/*.vue`、`frontend/src/components/notification/*.vue` 等。
- 已正确使用 token 的写法是 `border-input`/`border-border`（如 `frontend/src/components/ui/Input.vue` 的 `border border-input`）。

**Root Cause Hypothesis**（高置信）：裸 `border` 取到 Tailwind 默认浅灰 `gray-200`，而非暗色感知的 `--border` token。

**Proposed Remediation**：
- 首选（一处修复全局）：在 `tailwind.config.js` 的 `theme.extend` 里加 `borderColor: { DEFAULT: 'var(--border)' }`，让所有裸 `border` 走语义 token。
- 备选：把各组件裸 `border` 逐个改为 `border-border`（改动面大、易漏）。
- 注意 `--border` 暗色值 `hsl(240 3.7% 15.9%)` 已存在，无需改 tokens.css。

---

## F4 切换项目旧数据闪烁

**Symptom**：在项目 A 与项目 B 之间导航（`/projects/:id` 变化）时，页面先显示旧项目的任务列表，短暂闪烁后再加载新项目。

**Suspected Code Paths**：
- `frontend/src/pages/projects/ProjectDetailPage.vue` — `const projectId = route.params.id as string` 只取一次；`onMounted` 只执行一次，未 `watch` 路由参数；`useTasks().load(projectId)` 与 `useTasks().reset()` 未在切换时调用。

**Root Cause Hypothesis**（高置信）：Vue Router 复用同一路由组件，`:id` 变化不触发 `onMounted`，`projectId` 与 `tasks` 保持旧值，直到下一次手动加载才更新。

**Proposed Remediation**：
- `watch(() => route.params.id, async (id) => { tasks.reset(); await load(id); project.value = await projectApi.get(id); })`，`onMounted` 仅做首次加载。加载前先 `reset()` 清空旧任务，消除闪烁。

---

## F5 导航栏按钮尺寸不统一

**Symptom**：顶栏按钮（主题切换、通知铃铛、登出、登录）高度/内边距不一致，视觉参差。

**Suspected Code Paths**：
- `frontend/src/layouts/AppLayout.vue` — 主题切换按钮 `px-2 py-1`，登出 `px-3 py-1.5`，登录 `px-3 py-1.5`。
- `frontend/src/components/notification/NotificationBell.vue` — 铃铛 `px-2 py-1.5`。

**Root Cause Hypothesis**（高置信）：各按钮内边距硬编码不一致。

**Proposed Remediation**：
- 统一为同一高度（如 `h-9`）与一致内边距（如 `px-3`），或抽一个共享「顶栏按钮」样式/组件；至少把主题切换与铃铛的 `py`/`px` 与登录登出对齐。

---

## F6 登出无确认弹窗

**Symptom**：点击「登出」立即登出，无二次确认。

**Suspected Code Paths**：
- `frontend/src/layouts/AppLayout.vue` — `@click="logout()"` 直接调用 `useAuth().logout()`。

**Root Cause Hypothesis**（高置信）：缺少确认交互（产品上属低风险，但用户期望确认）。

**Proposed Remediation**：
- 登出前弹确认对话框（复用 `frontend/src/components/ui/Dialog.vue`），确认后再 `logout()`。备选：若产品不要求确认，则维持现状并在文档中说明。

---

## 统一 Proposed Remediation（要改的文件）

- `frontend/src/components/notification/NotificationPanel.vue`（F1）
- `frontend/src/pages/projects/ProjectDetailPage.vue`（F2、F4）
- `frontend/src/components/task/TaskDetailDrawer.vue`（F2）
- `frontend/tailwind.config.js`（F3，首选单点修复）
- `frontend/src/layouts/AppLayout.vue`（F5、F6）
- `frontend/src/components/notification/NotificationBell.vue`（F5）

## Tests to add or update

- F1：移动端视口下通知面板不溢出（Playwright 移动项目断言面板 `boundingBox` 在视口内）。
- F2：抽屉移动后「当前列」文本随状态更新（Vitest 组件测试：`moveTask` 后断言抽屉状态）。
- F3：暗色模式下边框色为 token 值（可用视觉回归或类名断言）。
- F4：切换 `:id` 后旧任务被清空并加载新任务（Vitest：watch 触发 + reset）。
- F6：登出点击后出现确认弹窗（Vitest/Playwright）。

## Risks & Considerations

- F3 的 `borderColor.DEFAULT` 覆盖会影响全站所有裸 `border`，需回归检查暗/亮两主题与 001 既有页面（设计画廊等）。
- F4 需保证首次挂载与参数变化两条路径都正确，避免重复请求或竞态（快速切换时以最新 id 为准）。
- F2 若改为「按 id 从 store 取任务」，需处理任务被删除/不存在的边界。

## Open Questions

- [NEEDS CLARIFICATION: F6 登出确认是否属于产品硬性要求，还是可选增强？]
- [NEEDS CLARIFICATION: F3 是否统一采用 `borderColor.DEFAULT` 单点覆盖，还是逐组件改为 `border-border`？]
