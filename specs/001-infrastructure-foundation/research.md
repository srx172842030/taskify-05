# 研究与决策记录：第一阶段 — 基础设施建设

本文件记录关键技术决策、理由与被否定的备选方案。技术栈已由用户在 `/speckit-plan` 输入中完整指定，故本文件聚焦「如何落地」的次级决策，而非重新选型。

## 1. 前端基础

**决策**：Vue 3（Composition API + `<script setup>`）+ TypeScript strict + pnpm + Tailwind CSS。
**理由**：用户指定；`<script setup>` 与 strict TS 提升类型安全与可维护性，符合章程原则 I/III。
**备选**：React / Nuxt 等——用户已明确排除。

**决策**：组件分两层——`components/ui/` 存 shadcn-vue 源码（只读，不直接修改），`components/` 存业务封装。
**理由**：隔离「不可改的上游」与「可改的业务」，便于升级 shadcn-vue 且不污染业务代码（原则 I/V）。
**备选**：直接改 shadcn-vue 源码——升级困难、易产生分叉。

## 2. 设计 token

**决策**：CSS 变量作为设计 token，语义化命名（`background`、`foreground`、`primary`、`muted`、`border`、`destructive` 等），禁用具体颜色名（如 `blue-500`）；亮/暗两套定义于 `styles/tokens.css`。
**理由**：语义化命名让「换肤/换主题」只改 token 值不改组件，契合三主题需求与原则 III。
**备选**：直接使用 Tailwind 颜色类（`bg-blue-500`）——换主题需到处改类名，违背一致性。
**输出**：token 清单与用法见 `contracts/design-tokens.md` 及实现期 `frontend/docs/design-tokens.md`。

## 3. 暗色模式

**决策**：@vueuse/core 的 `useColorMode`（class 策略），默认跟随系统，入口处加内联脚本避免首屏闪烁（FOUC）。
**理由**：class 策略与 Tailwind `dark:` 变体天然契合；内联脚本在 CSS/JS 加载前就设好根元素 class，避免闪烁。
**备选**：CSS `prefers-color-scheme` 媒体查询单独实现——难以支持用户手动覆盖后的持久化。

## 4. 后端架构与模块拆分

**决策**：Maven 多模块微服务，拆为 `taskify-common`、`taskify-gateway`、`taskify-auth`、`taskify-example` 四模块（模块统一前缀 `taskify-`）；Nacos 负责注册与配置，OpenFeign 负责同步调用，RocketMQ 预留异步通信。
**理由**：用户指定微服务路线；`taskify-common` 沉淀统一底座（响应/错误/校验/日志/请求 ID），业务服务依赖它。
**备选**：模块化单体——更简（原则 V），但用户已定微服务；复杂度已记录于 plan.md「复杂度追踪」。

**决策**：第一阶段实际注册到 Nacos 的服务为 `taskify-gateway`、`taskify-auth`、`taskify-example`；`taskify-common` 为库模块不独立部署。
**理由**：仅部署有运行时价值的进程，减少本地与 CI 负担。

**决策**：`taskify-gateway` 采用 WebFlux 版网关依赖 `spring-cloud-starter-gateway-server-webflux`（非 MVC 版）。
**理由**：Spring Cloud 2025 将网关拆为 webflux / mvc 两版；网关为反应式，选 webflux 版本。

## 5. 统一响应与错误码

**决策**：错误采用 `ProblemDetail`（RFC 9457）+ 自定义 `ErrorCode` 枚举 + `@RestControllerAdvice` 全局兜底；成功采用统一封装 `ApiResponse<T>`。
**理由**：`ProblemDetail` 是标准机器可读错误格式；`ErrorCode` 提供业务侧稳定错误码；`@RestControllerAdvice` 让业务代码无需自行捕获异常（原则 I）。
**备选**：自定义非标准错误结构——放弃 RFC 兼容与 springdoc 内置支持。
**契约**：见 `contracts/api-conventions.md`。

## 6. 无状态会话与认证

**决策**：采用签名 JWT 作为无状态会话凭据；access token 短生命周期；登出为客户端丢弃 token（第一阶段不引入服务端吊销列表）。
**理由**：与「无状态会话」要求一致；网关单点校验 token，下游服务信任网关透传的身份。
**备选**：服务端 session 存储——违背无状态要求；不透明 token 需集中校验——增加单点依赖。
**风险说明**：无吊销列表时，登出后 token 在有效期内仍可被重放；第一阶段可接受，后续如需即时吊销再引入短 TTL + 刷新令牌或吊销列表。

## 7. 图形验证码

**决策**：服务端生成图形验证码，返回 `captchaId` + 图片（base64）；答案以 `captchaId` 为键存储于 Redis（TTL 5 分钟，一次性消费）；注册/登录时校验。
**理由**：`captchaId` 使验证码无状态可横向扩展；一次性消费防止重放。
**备选**：无状态签名验证码（答案编码进 token）——需防篡改且无法强制一次性；纯前端验证——不安全。
**风险说明**：答案存于 Redis 共享存储，天然支持多实例；用 Redis TTL 实现过期、DEL 实现一次性消费。

## 8. 分页约定

**决策**：统一页码分页：请求 `page`（从 1 起）+ `size`（默认 20，上限 100）；响应返回 `items`、`page`、`size`、`total`、`totalPages`。
**理由**：规格明确「统一分页约定为页码」；页码比游标更简单、贴合管理端场景。
**备选**：游标分页——适合无限流但复杂度更高，本阶段不需要。
**契约**：见 `contracts/api-conventions.md`。

## 9. UUID v7 与数据库

**决策**：主键使用 UUID v7，由应用生成（不自增、不由数据库生成）；PostgreSQL + Flyway 迁移；表前缀 `taskify_`。
**理由**：UUID v7 时间有序（利于索引局部性与分页），且分布式无冲突；应用生成避免数据库自增在微服务下的耦合。
**备选**：自增 BIGINT——多服务/未来分库分表不便；UUID v4——无时间有序性，索引友好度差。

## 10. 健康检查

**决策**：`/health` 报告存活（进程可响应），`/ready` 报告就绪（依赖如 PostgreSQL 可达）；网关与各业务服务均暴露。
**理由**：区分存活与就绪是容器编排（重启/摘流）的标准前提，符合规格 FR-027。
**备选**：单一健康端点——无法区分「进程活着」与「依赖不可用」。

## 11. 中间件与本地启动

**决策**：`backend/docker-compose.yml` 一键拉起 PostgreSQL、RocketMQ、Nacos、Redis；应用本身可由 Maven 启动。
**理由**：满足规格 FR-028「本地开发环境一键启动」。
**备选**：手工安装中间件——重复且易错。

## 12. 测试策略

**决策**：后端 JUnit 5 + Testcontainers（对 PostgreSQL 的真实集成测试）；前端分两层——Vitest + @vue/test-utils（单元与组件测试）+ Playwright（E2E 覆盖关键旅程：注册/登录/主题切换/设计画廊）。
**理由**：Testcontainers 提供真实依赖的确定性集成测试（原则 II）；Vitest 提供组件逻辑的快速反馈，Playwright 覆盖 UI 端到端。
**备选**：H2 内存库替代 Testcontainers——与 PostgreSQL 行为有差异，不满足真实契约测试要求。

## 13. CI 位置

**决策**：GitHub Actions 工作流统一放置在仓库根目录 `.github/workflows/`；一个仓库级工作流矩阵构建后端各模块，另配前端工作流。
**理由**：GitHub 仅识别仓库根目录下的 `.github/workflows/`，分散到 `backend/`、`frontend/` 子目录会被忽略。
**备选**：各子目录各自放置——GitHub 无法识别，不可用。

## 14. 缓存（Redis + Redisson）

**决策**：采用 Redis 作为缓存/共享存储，客户端统一使用 Redisson（`redisson-spring-boot-starter`）；验证码答案等短期共享数据存放于 Redis。
**理由**：Redisson 提供分布式锁、对象缓存等高级能力，与 Spring Boot 集成良好；Redis 作为共享存储解决多实例下内存态数据不共享的问题（如验证码）。
**备选**：Spring Data Redis + Lettuce——更轻但缺分布式锁等能力；本地内存缓存——无法跨实例共享。

## 15. 微服务容器化与一键启动

**决策**：微服务用独立 `docker-compose.app.yml` 管理（与中间件 `docker-compose.yml` 分开）；各可部署模块（gateway/auth/example）提供简单 Dockerfile——先本地 `mvn clean package -Dmaven.test.skip=true` 打包出 jar，再用 `docker build` 打镜像，镜像内 `java -jar` 启动。
**理由**：分离中间件与应用生命周期；本地先打包再打镜像，Dockerfile 简单、构建快、易排查。
**备选**：Docker 内多阶段构建（镜像内执行 `mvn package`）——自包含但镜像大、构建慢、Dockerfile 复杂；不符合「简单 Dockerfile、本地先打包」的要求。
