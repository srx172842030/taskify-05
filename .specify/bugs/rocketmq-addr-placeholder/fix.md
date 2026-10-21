# Bug Fix: taskify-example 测试 ROCKETMQ_ADDR 占位符解析失败

- **Slug**: rocketmq-addr-placeholder
- **Fixed**: 2026-10-21
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

给 `taskify-example` 与 `taskify-auth` 两个 `application.yml` 中所有环境变量占位符补回默认值（`${VAR:default}`），使 `mvn test` / 本地直启无需注入环境变量即可启动，docker-compose 环境变量仍可覆盖。修复后 `mvn -pl taskify-example,taskify-auth -am test` 全绿。

## Changes

| File | Change | Notes |
|------|--------|-------|
| `backend/taskify-example/src/main/resources/application.yml` | modified | `${NACOS_ADDR}`、`${POSTGRES_ADDR}`、`${POSTGRES_USER}`、`${POSTGRES_PASSWORD}`、`${REDIS_HOST}`、`${REDIS_PORT}`、`${ROCKETMQ_ADDR}` 均补默认值 |
| `backend/taskify-auth/src/main/resources/application.yml` | modified | 同上（NACOS/POSTGRES/REDIS）及 `${JWT_SECRET}` 补默认值 |

## Diff Highlights

```yaml
# 例
rocketmq:
  name-server: ${ROCKETMQ_ADDR:localhost:9876}
  ...
taskify:
  jwt:
    secret: ${JWT_SECRET:change-me-in-production-please-use-a-long-random-string}
```

## Tests Added or Updated

- 无新增；现有集成测试因默认值恢复而通过。

## Local Verification

- `mvn -q -pl taskify-example,taskify-auth -am test` → `AuthIntegrationTest 2/2`、`ExampleIntegrationTest 1/1`，0 failures / 0 errors

## Deviations from Assessment

- 无。

## Follow-ups

- 默认 `JWT_SECRET` 仅用于本地/测试，生产环境务必用环境变量覆盖。
