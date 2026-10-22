# Bug Verification: 排期/阻塞功能四类缺陷

- **Slug**: scheduling-dependency-bugs
- **Tested**: 2026-10-22
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

四项修复在自动化测试层面全部通过：后端全量 17/17、前端 22/22、类型检查通过，且 F4（进行中的阻塞任务禁止推进/允许回退）由集成测试精确复现并验证。但评估中列出的浏览器端复现（F1 实时刷新、F2 拖拽回弹）与网关运行态转发（F3）需要完整运行栈 + 浏览器 + 网关重启才能端到端演练，本环境未启动完整栈，故未实际执行——按验证纪律降级为「partial」而非「verified」。

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| 后端全量回归 | `mvn -q test -pl taskify-project,taskify-notification -am` | pass | 17/17，`MVN_EXIT=0` |
| F4 新用例 | `TaskDependencyIntegrationTest`（3 tests） | pass | 含 `blockedTaskCannotAdvanceButCanRevert` |
| 前端单测 | `pnpm test` | pass | 22/22 |
| 前端类型检查 | `pnpm exec vue-tsc -b` | pass | 无错误 |
| F3 配置就位 | `grep reminders gateway/application.yml` | pass | `/api/v1/reminders/**` 已加入 project 路由 |
| F4 代码就位 | `grep "target.order() > current.order()"` | pass | `TaskService.java:108` |
| F1 浏览器实时刷新 | 需完整栈 + 浏览器 + SSE | not-run | 仅由单测覆盖 |
| F2 浏览器拖拽回弹 | 需完整栈 + 浏览器 | not-run | 仅由单测覆盖 |
| F3 网关运行态转发 | 需重启 gateway + 完整栈 | not-run | 仅配置级验证 |

## Output Excerpts

```
MVN_EXIT=0
Tests run: 4, Failures: 0, Errors: 0 -- NotificationIntegrationTest
Tests run: 2, Failures: 0, Errors: 0 -- ReminderIntegrationTest
Tests run: 2, Failures: 0, Errors: 0 -- TaskTimeIntegrationTest
Tests run: 2, Failures: 0, Errors: 0 -- TaskIntegrationTest
Tests run: 2, Failures: 0, Errors: 0 -- CommentIntegrationTest
Tests run: 3, Failures: 0, Errors: 0 -- TaskDependencyIntegrationTest
Tests run: 2, Failures: 0, Errors: 0 -- ProjectIntegrationTest
```

前端：`Test Files 9 passed (9) — Tests 22 passed (22)`；`vue-tsc -b` 无输出（通过）。

## Residual Risks

- **F3 运行态**：网关路由为启动时加载的静态配置，`DELETE/PATCH /api/v1/reminders/{id}` 需重启 `taskify-gateway` 后才实际放行；本环境未重启网关，未做 404→200 的真实请求验证。
- **F1/F2 浏览器行为**：实时刷新依赖 SSE → `reload()`、拖拽回弹依赖 store 变更驱动列重算，均已由单测覆盖逻辑，但未在真实浏览器（Playwright E2E）中演练。
- **F1 抖动**：SSE 连续多条 block/unblock 事件会触发多次 `reload()`，未做防抖（规模下需优化）。

## Recommendation

- **暂不关闭**，保持「partial」：自动化测试与配置级检查均通过，但 F1/F2/F3 的端到端复现需启动完整栈（`docker compose up -d` + 后端 + `pnpm dev` + 重启网关）后在浏览器演练一次。
- 建议在 staging/本地完整栈就绪后，复跑评估中的 4 个复现步骤，或将本报告升级为「verified」；若复现仍出现，重新运行 `/speckit-bug-assess`。
