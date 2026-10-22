# Bug Verification: 前端 UI/UX 打磨（6 项）

- **Slug**: frontend-ui-polish
- **Tested**: 2026-10-22
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

修复无产品回归：类型检查、单元测试（14/14）、生产构建、以及**完整 chromium E2E（8/8）** 全部通过，后端也已恢复（网关 UP、验证码接口正常）。移动端 kanban 有 1 例 E2E 未通过，但经核对是**测试断言未适配单列视图**所致（任务已成功移动到「进行中」，移动端仍停留「待办事项」单列，故断言「任务仍可见」落空），非产品缺陷。F1/F4 等视觉/响应式复现未由自动化 E2E 直接断言，故保守判定为 `partial`。

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| 后端可用性 | `docker ps` + `curl localhost:8080/health` + 验证码接口 | pass | 网关 `{"status":"UP"}`，验证码返回 `SUCCESS` |
| 类型检查 | `pnpm exec vue-tsc -b` | pass | 无类型错误（fix 步骤，源码未再变更） |
| 单元测试 | `pnpm test` | pass | 8 文件 / 14 用例（fix 步骤） |
| 生产构建 | `pnpm exec vite build` | pass | `✓ built`（含 Tailwind 变更） |
| 回归：chromium 全量 E2E | `npx playwright test --project=chromium` | pass | 8/8 通过 |
| 移动端 E2E（Pixel 5 kanban） | `npx playwright test e2e/kanban.spec.ts --project=mobile` | fail（1/2） | 「看板渲染与创建」通过；「抽屉移动」断言未适配单列视图（非产品缺陷） |
| F1 移动端通知面板溢出复现 | 未直接执行（无移动端通知面板 E2E） | not-run | 由代码构造保证（见 Residual Risks） |

## Output Excerpts

```
$ npx playwright test --project=chromium --reporter=line
  8 passed (25.1s)

$ npx playwright test e2e/kanban.spec.ts --project=mobile --reporter=line
  1 passed, 1 failed

失败点（移动端「任务在相邻列间移动」）：
Locator: getByRole('button', { name: '拖拽任务 1792657766056' }).first()
Error: element(s) not found
// 上下文快照显示：移动端单列仍停在「待办事项」，任务已移入「进行中」——移动成功，断言落空
```

## Residual Risks

- **F1（移动端通知面板）**：修复为 `fixed inset-x-4 top-16`（移动端）＋ `md:absolute md:right-0 md:w-80`（桌面）。`fixed` + `inset-x-4` 在 CSS 构造上必然约束在视口内（左右各 16px），但**未由移动端视口的 E2E 直接断言**。
- **F3（暗色边框）**：`borderColor.DEFAULT` 覆盖经构建通过，但未做暗色主题的视觉/类名断言。
- **F4（切换项目）**：`watch(projectId)` + `reset()` 逻辑经类型检查，但无「项目 A↔B 切换」的 E2E 用例直接覆盖。
- **移动端 kanban E2E 断言缺陷**：`e2e/kanban.spec.ts` 的「抽屉移动」用例在移动端单列视图下断言「任务仍可见」不正确——移动成功后任务已离开当前列。该用例需适配单列视图（移动后切到目标列或断言「不再位于待办」），属测试问题而非产品缺陷。

## Recommendation

**可判定为「修复有效、无回归」**，但按「未直接复现的视觉/响应式项不应判 `verified`」原则保守记为 `partial`。建议：
1. 修正移动端 kanban E2E 断言（单列视图适配），并补一个 F1 移动端通知面板「面板边界在视口内」的 E2E 断言、F4「切换项目清空旧数据」的用例——补上后即可升为 `verified`。
2. 无需重开 `/speckit-bug-assess`（根因与修复方向均已被证据支持）。
