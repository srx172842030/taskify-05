# Bug Verification: taskify-example 测试 ROCKETMQ_ADDR 占位符解析失败

- **Slug**: rocketmq-addr-placeholder
- **Tested**: 2026-10-21
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

原始症状（未注入环境变量时 `taskify-example`/`taskify-auth` 测试因 `ROCKETMQ_ADDR` 占位符无法解析而启动失败）已不再复现：占位符均补回默认值后，`mvn -pl taskify-example,taskify-auth -am test` 全绿。

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `mvn -pl taskify-example,taskify-auth -am test`（未设置相关环境变量） | pass | AuthIntegrationTest 2/2、ExampleIntegrationTest 1/1，0 failures/errors |
| Placeholder default check | `grep '${VAR:default}'` | pass | 两文件各 7 个占位符均带默认值，无裸 `${VAR}` 残留 |
| Regression（同模块集成测试） | 同上 | pass | 覆盖改动模块 |

## Output Excerpts

- `AuthIntegrationTest`: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `ExampleIntegrationTest`: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- 占位符默认值：`taskify-auth/application.yml:7`、`taskify-example/application.yml:7`

## Residual Risks

- 默认 `JWT_SECRET` 仅用于本地/测试，生产须用环境变量覆盖（docker-compose 已注入）。

## Recommendation

关闭该 bug——已验证端到端：无环境变量时测试通过，占位符默认值齐全，无回归。
