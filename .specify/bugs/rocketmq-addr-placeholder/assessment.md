# Bug Assessment: taskify-example 测试 ROCKETMQ_ADDR 占位符解析失败

- **Slug**: rocketmq-addr-placeholder
- **Created**: 2026-10-21
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report（原文）

> 解决 taskify-example 模块测试报错问题，报错信息：Caused by: org.springframework.util.PlaceholderResolutionException: Could not resolve placeholder 'ROCKETMQ_ADDR' in value "${ROCKETMQ_ADDR}"

## Symptom

`taskify-example`（及 `taskify-auth`）模块跑测试时，Spring 上下文启动失败，抛 `PlaceholderResolutionException: Could not resolve placeholder 'ROCKETMQ_ADDR'`。原因是 `application.yml` 里的环境变量占位符（`${ROCKETMQ_ADDR}` 等）没有默认值，而测试/本地运行环境未注入这些环境变量。

## Reproduction

1. `cd backend && mvn -pl taskify-example -am test`（未设置 `ROCKETMQ_ADDR` 等环境变量）
2. 观察 `PlaceholderResolutionException: Could not resolve placeholder 'ROCKETMQ_ADDR'`

## Suspected Code Paths

- `backend/taskify-example/src/main/resources/application.yml` — `rocketmq.name-server: ${ROCKETMQ_ADDR}`（无默认值）；同文件 `${NACOS_ADDR}`、`${POSTGRES_ADDR}`、`${POSTGRES_USER}`、`${POSTGRES_PASSWORD}`、`${REDIS_HOST}`、`${REDIS_PORT}` 均无默认值
- `backend/taskify-auth/src/main/resources/application.yml` — `${JWT_SECRET}`、`${NACOS_ADDR}`、`${POSTGRES_*}`、`${REDIS_*}` 均无默认值

## Root Cause Hypothesis

为适配 `docker-compose.app.yml` 通过环境变量注入，`application.yml` 的占位符被改成了裸 `${...}`（去掉了原来的默认值）。docker 运行时会注入这些变量，但 `mvn test` / 本地直启不会，Spring 解析到无默认值的占位符即抛异常。`ROCKETMQ_ADDR` 是 example 模块最先被 rocketmq starter 读取的占位符，故报错落在此处。置信度：**高**。

## Proposed Remediation

**Preferred**：给两个 `application.yml` 中所有环境变量占位符补回默认值（本地/测试可用，docker 环境变量仍可覆盖）：

- `${NACOS_ADDR:localhost:8848}`
- `${POSTGRES_ADDR:localhost:5432}`
- `${POSTGRES_USER:taskify}` / `${POSTGRES_PASSWORD:taskify}`
- `${REDIS_HOST:localhost}` / `${REDIS_PORT:6379}`
- `${ROCKETMQ_ADDR:localhost:9876}`
- `${JWT_SECRET:change-me-in-production-please-use-a-long-random-string}`

**Alternatives**：在集成测试 `@SpringBootTest(properties=...)` 里为这些占位符补齐测试值——治标，且每次新增占位符都要维护，不推荐。

**Files likely to change**：
- `backend/taskify-example/src/main/resources/application.yml`
- `backend/taskify-auth/src/main/resources/application.yml`

**Tests to add or update**：
- 无需新增；现有 `ExampleIntegrationTest`、`AuthIntegrationTest` 在补默认值后即可通过。

## Risks & Considerations

- 默认 `JWT_SECRET` 仅用于本地/测试，生产必须用环境变量覆盖（已有注释提示）。
- 补默认值不影响 docker-compose.app.yml 的环境变量注入（环境变量优先于默认值）。

## Open Questions

- 无。
