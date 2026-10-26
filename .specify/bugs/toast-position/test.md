# Bug Verification: Toast 弹出位置错误（未悬浮、占据主面板空间）

- **Slug**: toast-position
- **Tested**: 2026-10-25
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

根因（vue-sonner 样式未导入，导致 `[data-sonner-toaster]` 缺少 `position: fixed`）已通过补导入 `vue-sonner/style.css` 修复。新增 e2e 回归测试确认 toast 视口计算样式为 `position: fixed`，构建与 lint 均通过。原症状不再复现。

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| 回归测试（自动化复现） | `playwright test --project=chromium toast.spec.ts` | pass | 断言 `[data-sonner-toaster]` 的 `getComputedStyle(...).position === 'fixed'` |
| 构建 | `pnpm build` | pass | 产物 CSS 含 `data-sonner-toaster` 规则；vue-tsc 类型检查通过 |
| Lint | `pnpm lint` | pass | 0 错误 |

## Output Excerpts

```
[WebServer] $ vite
Running 1 test using 1 worker
  ✓  1 [chromium] › e2e/toast.spec.ts:3:1 › toast 悬浮于视口固定位置而非占据文档流 (1.8s)
  1 passed (4.2s)
```

```
✓ built in 9.50s
```

## Residual Risks

- 未在真实后端环境下手工目视核对 toast 外观（背景/圆角/阴影）与设计令牌一致；shadcn `Sonner.vue` 已通过 `toastOptions.classes` 覆盖外观，风险低。
- 未覆盖移动端 toast 定位；但 `position: fixed` 与视口宽高无关，桌面端已验证的修复同样适用于移动端。

## Recommendation

Close the bug — 根因明确、修复经回归测试与构建双重验证，原症状（toast 占据文档流）不再复现。
