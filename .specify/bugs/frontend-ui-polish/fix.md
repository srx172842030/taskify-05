# Bug Fix: 前端 UI/UX 打磨（6 项）

- **Slug**: frontend-ui-polish
- **Fixed**: 2026-10-22
- **Assessment**: ./assessment.md
- **Status**: applied（F1–F5 已应用；F6 按用户说明作为可选增强跳过）

## Summary

修复前端 5 项 UI/UX 问题：通知面板移动端溢出、任务抽屉移动后状态不刷新、暗色模式边框刺眼、切换项目旧数据闪烁、导航栏按钮尺寸不统一。F6（登出确认）经确认属可选增强，未实现。F3 采用用户指定的单点 `borderColor.DEFAULT` 覆盖方案。

## Changes

| File | Change | Notes |
|------|--------|-------|
| `frontend/tailwind.config.js` | modified | 新增 `borderColor.DEFAULT = 'var(--border)'`，让所有裸 `border` 走暗色感知 token（F3） |
| `frontend/src/components/notification/NotificationPanel.vue` | modified | 面板定位改为移动端 `fixed inset-x-4 top-16`、桌面 `md:absolute md:right-0 md:w-80`（F1） |
| `frontend/src/pages/projects/ProjectDetailPage.vue` | modified | `projectId` 改为 `computed`；抽 `loadProject()` 并在切换时 `reset()` 清空旧数据 + 关闭抽屉（F4）；`watch(tasks)` 同步 `selectedTask` 使抽屉状态刷新（F2） |
| `frontend/src/layouts/AppLayout.vue` | modified | 主题切换/登出/登录按钮统一 `h-9` 高度（F5） |
| `frontend/src/components/notification/NotificationBell.vue` | modified | 铃铛按钮统一 `h-9` 高度（F5） |

## Diff Highlights

```js
// tailwind.config.js —— F3 单点修复
borderColor: {
  DEFAULT: 'var(--border)',
},
```

```html
<!-- NotificationPanel.vue —— F1 响应式定位 -->
<div class="fixed inset-x-4 top-16 ... md:absolute md:right-0 md:top-auto md:mt-1 md:w-80 ...">
```

```ts
// ProjectDetailPage.vue —— F2 + F4
const projectId = computed(() => route.params.id as string)
watch(projectId, () => loadProject())
watch(tasks, (list) => {
  if (selectedTask.value) {
    const updated = list.find((t) => t.id === selectedTask.value!.id)
    if (updated) selectedTask.value = updated
  }
}, { deep: true })
```

## Tests Added or Updated

未新增单元测试：本批改动以**呈现层（CSS/定位/尺寸）**与**响应式接线（watch/computed）**为主，现有 `frontend/e2e/kanban.spec.ts`、`frontend/e2e/notification.spec.ts` 已覆盖看板移动与通知面板行为路径。见 Follow-ups。

## Local Verification

- `pnpm exec vue-tsc -b` → 通过（无类型错误）
- `pnpm test` → 8 个测试文件、14 个用例全部通过
- `pnpm exec vite build` → 成功（`✓ built in 6.56s`，含 Tailwind 配置变更）

## Deviations from Assessment

- **F6 未实现**：评估中 `[NEEDS CLARIFICATION]` 询问「登出确认是否硬性要求」，用户答复「登出属于可选增强」，故跳过，未添加确认弹窗。
- **F3 采用单点方案**：按用户指定采用 `borderColor.DEFAULT` 覆盖（而非逐组件 `border-border`）。
- **未新增评估建议的专项测试**：改动为呈现层 + 响应式接线，未新增 Vitest/Playwright 专项用例，已在 Follow-ups 中列出。

## Follow-ups

- 为 F4 项目切换补一个 `ProjectDetailPage` 的组件测试（mock `useTasks`/`projectApi`，断言 `:id` 变化触发 `reset` + 重新加载）。
- 为 F2 补一个抽屉「移动后当前列随状态更新」的断言（可扩展现有 `TaskDetailDrawer.spec.ts`）。
- 视觉回归（F1/F3/F5）：可用 Playwright 移动项目断言通知面板边界在视口内、暗色主题下边框色为 token 值。
