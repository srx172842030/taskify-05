# Bug Fix: 前端 E2E 测试失败

- **Slug**: frontend-e2e-failure
- **Fixed**: 2026-10-21
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

修复 E2E 测试定位器与 UI 不一致（主题切换按钮缺 aria-label、按钮文案不一致、认证测试依赖后端验证码），并在二次修复中发现并修复 `LoginDialog` 弹窗重开未重置 `mode` 导致「登录」步骤误走「注册」的缺陷。后端以 `taskify.captcha.enabled=false` 启动后，完整流程跑通。最终 `pnpm exec playwright test` 为 **10 通过 / 0 失败 / 0 跳过**。

## Changes

| File | Change | Notes |
|------|--------|-------|
| `frontend/src/layouts/AppLayout.vue` | modified | 主题切换按钮新增 `aria-label="切换主题"` |
| `frontend/src/pages/design/DesignGallery.vue` | modified | 按钮文案「主要/次要/危险/幽灵」→「主要按钮/…」 |
| `frontend/src/components/auth/LoginDialog.vue` | modified | 弹窗打开时重置 `mode.value = 'login'`（二次修复新增） |
| `frontend/e2e/theme.spec.ts` | modified | 主题切换定位器改为 `切换主题` |
| `frontend/e2e/design.spec.ts` | modified | 主题切换定位器 + 按钮文案对齐 |
| `frontend/e2e/auth.spec.ts` | modified（含用户改动） | 「登录弹窗渲染」去掉验证码图片断言；全流程测试已重新启用（用户去除 `test.skip`） |
| `frontend/playwright.config.ts` | modified | 新增 `workers: 1` |

> 注：`backend/taskify-auth/src/main/resources/application.yml` 的 `taskify.captcha.enabled=false` 与 `auth.spec.ts` 的 `test.skip` → `test` 由用户在外部修改，本命令未覆盖，仅在报告中记录。

## Tests Added or Updated

- `frontend/e2e/theme.spec.ts` — 主题切换用可访问名「切换主题」
- `frontend/e2e/design.spec.ts` — 主题切换定位 + 按钮文案「主要按钮」
- `frontend/e2e/auth.spec.ts` — 「登录弹窗渲染」不依赖验证码图片；全流程「注册→登出→登录」已恢复运行

## Local Verification

- `pnpm exec playwright test` → `10 passed, 0 failed, 0 skipped`（9.6s）
- 后端以 `taskify.captcha.enabled=false` 运行，`curl localhost:8080/health` 返回 UP

## Deviations from Assessment

- 原评估未发现「LoginDialog 弹窗重开未重置 mode」的缺陷；启用全流程测试后暴露，属于修复过程中新发现的根因，已在本次一并修复。
- 完整认证流程原建议「受控测试或独立 profile」，最终采用「后端 `captcha.enabled=false` 启动 + 恢复 `test`」的方式（由用户配置后门）。

## Follow-ups

- 若 CI 需常驻完整认证 E2E，可新增独立 Playwright project + 后端后门 profile。
