# Bug Fix: Toast 弹出位置错误（未悬浮、占据主面板空间）

- **Slug**: toast-position
- **Fixed**: 2026-10-25
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

在 `frontend/src/main.ts` 补导入 `import 'vue-sonner/style.css'`，加载 vue-sonner 的定位样式（`[data-sonner-toaster] { position: fixed; … }`），使 toast 视口恢复固定悬浮定位，不再退化为文档流中的块元素。

## Changes

| File | Change | Notes |
|------|--------|-------|
| `frontend/src/main.ts` | modified | 新增一行 `import 'vue-sonner/style.css'` |
| `frontend/e2e/toast.spec.ts` | added | 回归测试：断言 `[data-sonner-toaster]` 的 `position` 为 `fixed` |

## Diff Highlights

```ts
// frontend/src/main.ts
import './styles/tokens.css'
import 'vue-sonner/style.css'   // ← 新增：加载 vue-sonner 定位样式
```

## Tests Added or Updated

- `frontend/e2e/toast.spec.ts` — 打开 `/design` 触发 toast 后，断言 `[data-sonner-toaster]` 存在且 `getComputedStyle(...).position === 'fixed'`，锁定「toast 悬浮定位」不再回归。

## Local Verification

- `pnpm build` → ✅ 通过；产物 CSS（`dist/assets/index-*.css`）由 ~32.5 kB 增至 ~48 kB，且包含 `data-sonner-toaster` 规则，确认样式已打包。
- `playwright test --project=chromium toast.spec.ts` → ✅ 1 passed。
- 说明：本次跑全量 e2e 时 auth/kanban/notification/scheduling 等用例失败，系后端未运行所致（这些用例依赖后端），与本次修复无关；toast 用例不依赖后端，单独运行通过。

## Deviations from Assessment

- 无实质偏离。评估建议的「e2e 断言固定定位」已实现；具体断言改用 `toHaveCount(1)` + `getComputedStyle` 而非 `toBeVisible()`——因空态 `<ol>` 视口为零尺寸，`toBeVisible()` 会误判为「隐藏」，与「是否 fixed 定位」无关。

## Follow-ups

- 后端恢复运行后可重跑全量 `pnpm e2e` 以确认无回归。
- 目视检查亮/暗两主题下 toast 外观是否与设计令牌一致（shadcn `Sonner.vue` 已通过 `toastOptions.classes` 覆盖外观，应无冲突）。
