# Bug Assessment: 前端 E2E 测试失败

- **Slug**: frontend-e2e-failure
- **Created**: 2026-10-21
- **Source**: pasted text（"后端服务已启动，重新排查e2e测试问题。 --slug=frontend-e2e-failure"）
- **Verdict**: valid
- **Severity**: medium

## Report（原文，二次排查）

> 后端服务已启动，重新排查e2e测试问题。

复跑时已确认：`curl http://localhost:8080/health` 返回 `{"status":"UP"}`，后端可达。

## Symptom

后端启动后重跑 `pnpm exec playwright test`，结果反而为 **10 失败 / 0 通过**（首次为 8 失败 / 2 通过，仅 404 测试通过）。失败仍集中在 theme/design/auth，另首次通过的 `states.spec.ts` 本次也因 `page.goto: net::ERR_ABORTED; maybe frame was detached?` 与 `Tearing down "context" exceeded the test timeout` 失败——属 Playwright `webServer` 启动/回收竞态的一次性抖动（复跑时 5173 端口已空闲，非残留进程）。

## Reproduction

1. 启动后端（gateway/auth/example + 中间件），`curl localhost:8080/health` 确认 UP
2. `cd frontend && pnpm exec playwright test`
3. 观察 10 failed / 0 passed

## Suspected Code Paths

- `frontend/src/layouts/AppLayout.vue`（约 L41-47）— 主题切换按钮只有 SVG 图标、无 `aria-label`
- `frontend/src/pages/design/DesignGallery.vue:55` — 按钮文案为「主要」，测试期望「主要按钮」
- `frontend/e2e/theme.spec.ts:7` — `getByRole('button', { name: /Sun|Moon|Monitor/ })` 匹配不到
- `frontend/e2e/design.spec.ts:6,9` — 文案不一致 + 主题切换定位失败
- `frontend/e2e/auth.spec.ts` — 完整注册/登录流程依赖后端开启验证码后门 `taskify.captcha.enabled=false`
- `frontend/playwright.config.ts` — `webServer` 只起前端，未起后端，且与多 project/worker 存在启动竞态

## Root Cause Hypothesis

核心是**前端测试定位器与实际 UI 不一致（与后端无关）**，二次排查证实：后端已 UP，但 theme/design/states 仍失败。① AppLayout 主题切换按钮仅含图标、无 `aria-label`，可访问名为空，`getByRole('button', { name: /Sun|Moon|Monitor/ })` 无法命中；② design.spec.ts 期望「主要按钮」而 DesignGallery 渲染「主要」；③ auth 全流程依赖后端且需 `taskify.captcha.enabled=false`（验证码后门），否则注册/登录无法自动化。另观察到的 `net::ERR_ABORTED` 为 webServer 竞态抖动，非稳定复现。置信度：**高**。

## Proposed Remediation

**Preferred**：
1. `AppLayout.vue` 主题切换按钮加 `aria-label="切换主题"`（顺带修复可访问性），`theme.spec.ts`/`design.spec.ts` 改用 `getByRole('button', { name: '切换主题' })`。
2. 统一按钮文案：`DesignGallery.vue` 的「主要/次要/危险/幽灵」改为「主要按钮/…」或反向改测试，二选一保持一致。
3. `auth.spec.ts` 拆分：前端纯 UI 断言（弹窗/输入框渲染）不依赖后端；完整「注册→登出→登录」改为受控测试（后端以 `taskify.captcha.enabled=false` 启动）或独立 profile。
4. `playwright.config.ts` 显式设置 `workers: 1`（或调低并发），降低 webServer 竞态抖动。

**Alternatives**：
- 用 Playwright `page.route` mock 后端 `/api/v1/auth/*`，让 auth 流程不依赖真实后端——但需维护 mock 契约。
- `webServer` 同时拉起后端——依赖重、启动慢，不推荐作为默认 E2E。

**Files likely to change**：
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/pages/design/DesignGallery.vue`（或仅改测试）
- `frontend/e2e/theme.spec.ts`
- `frontend/e2e/design.spec.ts`
- `frontend/e2e/auth.spec.ts`
- `frontend/playwright.config.ts`

**Tests to add or update**：
- 修正主题切换与按钮文案定位器。
- auth 前端断言与后端流程分离。

## Risks & Considerations

- 加 `aria-label` 会改变可访问名，需同步更新所有引用该按钮的测试。
- auth 全流程 E2E 依赖后端 + 验证码后门，跨进程编排较脆弱。
- 多 worker 并行会放大 webServer 竞态，建议固定 `workers`。

## Open Questions

- [NEEDS CLARIFICATION: 后端当前是否以 `taskify.captcha.enabled=false` 启动？这决定 auth 全流程 E2E 是否能直接跑通。]
