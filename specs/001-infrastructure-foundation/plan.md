# 实施计划：第一阶段 — 基础设施建设

**分支**：`001-infrastructure-foundation` | **日期**：2026-10-17 | **规格**：[spec.md](./spec.md)

**输入**：功能规格来自 `/specs/001-infrastructure-foundation/spec.md`

## 摘要

本阶段为 Taskify 搭建统一的界面基础与后端底座，使后续业务功能只需「组合现有部件」（前端）与「编写 service 与路由」（后端），不再各自处理样式、基础组件、响应格式、错误处理、鉴权、日志与配置等通用问题。

前端采用 Vue 3 + TypeScript（strict）+ Tailwind CSS + shadcn-vue，提供全局布局、三主题切换、17 个基础组件、通用状态页与隐藏的 `/design` 设计画廊。后端采用 Java 21 + Spring Boot 3.5.x 微服务架构（网关 + 认证服务 + 示例服务 + 公共模块），提供统一响应/错误码体系、入参校验、页码分页、无状态认证（账号密码 + 图形验证码）、默认拒绝的授权、示例表 CRUD 参考实现、CORS/安全响应头、结构化日志与请求 ID、缓存（Redis + Redisson）、健康检查，以及 docker-compose 一键拉起中间件。

## 技术上下文

**语言/版本**：
- 前端：Vue 3（Composition API + `<script setup>`）、TypeScript（strict）
- 后端：Java 21（Maven 构建）

**主要依赖**：
- 前端：Tailwind CSS、shadcn-vue、@vueuse/core（useColorMode）、vee-validate + zod、lucide-vue-next、vue-i18n、unplugin-fonts、ESLint + Prettier、Vitest、@vue/test-utils、Playwright
- 后端：Spring Boot 3.5.x、Mybatis-Plus、Spring Validation、Spring Security、Spring Cloud Gateway（WebFlux）、Nacos、OpenFeign、MapStruct、springdoc-openapi + Knife4j、MessageSource、Micrometer、Flyway、Testcontainers、Redis（Redisson 客户端）

**存储**：PostgreSQL（Flyway 迁移；UUID v7 由应用生成）；Redis（缓存，Redisson 客户端）

**测试**：前端 Vitest + @vue/test-utils（单元/组件）+ Playwright（E2E）；后端 JUnit 5 + Testcontainers

**目标平台**：Linux 服务器（容器化部署）；前端为响应式 Web（手机宽度 + 桌面）

**项目类型**：Web 应用（前端 SPA + 后端微服务）

**性能目标**：注册/登录端到端 <3s；健康/就绪端点即时响应；本地一键启动 <5min

**约束**：无状态会话；授权默认拒绝；CORS 白名单；安全响应头；文档必须中文（章程原则 VI）

**规模/范围**：第一阶段基础设施——17 个基础组件、5 个状态页、1 张示例表 CRUD、认证体系、统一底座

## 章程符合性检查

*门禁：Phase 0 研究前必须通过；Phase 1 设计后复检。*

- **I. 代码质量优先** — 通过：统一 ESLint/Prettier 与 Maven 构建规范；shadcn-vue 源码只读、业务封装独立。
- **II. 测试标准（不可协商）** — 通过：前端 Vitest + @vue/test-utils（单元/组件）+ Playwright（E2E）、后端 JUnit 5 + Testcontainers；示例 CRUD 提供完整测试作为后续样板。
- **III. 用户体验一致性** — 通过：设计 token（语义化命名）+ 共享组件库 + 三主题模式为核心交付。
- **IV. 性能要求** — 通过：成功标准含性能预算（<3s 注册登录、<5min 本地启动）；结构化为无状态 + 网关，可度量。
- **V. 简洁性与可维护性** — **需说明（已记录于复杂度追踪）**：微服务（网关 + 服务注册 + 消息队列 + 多模块）引入显著复杂度；此为用户明确的架构方向。
- **VI. 文档中文化** — 通过：本计划及全部生成文档使用中文。

## 项目结构

### 文档（本功能）

```text
specs/001-infrastructure-foundation/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（接口契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成，非本命令）
```

### 源码（仓库根目录）

```text
frontend/                       # Vue 3 SPA
├── src/
│   ├── components/
│   │   ├── ui/                 # shadcn-vue 源码（只读，不直接修改）
│   │   └── ...                 # 业务封装组件
│   ├── composables/
│   ├── layouts/                # 全局布局（顶部导航、抽屉、内容区、页脚、登录弹窗）
│   ├── pages/                  # 页面（含 /design、状态页）
│   ├── router/
│   ├── stores/
│   ├── i18n/messages/zh-CN.json
│   ├── styles/tokens.css       # 设计 token（亮/暗两套 CSS 变量）
│   └── main.ts
├── docs/design-tokens.md       # token 清单与用法
├── e2e/                        # Playwright E2E 测试
├── tests/unit/                 # Vitest + @vue/test-utils 单元/组件测试
├── package.json
└── pnpm-lock.yaml

backend/                        # Java 21 多模块 Maven
├── taskify-common/             # 公共库：统一响应、ErrorCode、校验、基础实体、日志/请求 ID
├── taskify-gateway/            # Spring Cloud Gateway：路由、CORS、安全头、认证过滤、限流
├── taskify-auth/               # 认证授权服务：用户、验证码、登录登出、令牌
├── taskify-example/            # 示例服务：示例表 CRUD（参考实现）
├── docker-compose.yml          # 一键拉起中间件（PostgreSQL、RocketMQ、Nacos、Redis）
├── docker-compose.app.yml      # 一键拉起微服务（gateway/auth/example）
└── pom.xml                     # 父 POM（模块：taskify-common/taskify-gateway/taskify-auth/taskify-example）

.github/workflows/              # CI 置于仓库根目录（GitHub 才能识别）：后端矩阵构建 + 前端工作流
```

**结构决策**：采用「前端 SPA + 后端多模块微服务」双目录结构（`/frontend` 与 `/backend`）。后端拆为 `taskify-common`（共享底座）、`taskify-gateway`（网关）、`taskify-auth`（认证授权）、`taskify-example`（示例 CRUD）四个模块（Maven 模块统一前缀 `taskify-`）；数据表统一前缀 `taskify_`。CI 工作流统一放置于仓库根目录 `.github/workflows/`。中间件与微服务分别用两份 docker-compose 管理：`docker-compose.yml` 拉中间件，`docker-compose.app.yml` 拉微服务（各可部署模块提供简单 Dockerfile，先本地 `mvn clean package -Dmaven.test.skip=true` 打包再打镜像）。

### 后端包结构（细分）

后端各模块统一遵循以下 Java 包结构（根包 `com.taskify.<模块>`；`taskify-common` 根包为 `com.taskify.common`）：

```text
taskify-common/src/main/java/com/taskify/common/
├── response/        # 统一响应 ApiResponse、ProblemDetail 封装
├── error/           # ErrorCode 枚举、全局异常处理（@RestControllerAdvice）
├── validation/      # 入参校验封装
├── base/            # 基础实体（BaseEntity：id、时间戳）
├── web/             # 请求 ID 过滤器、结构化日志、CORS/安全响应头
├── mybatis/         # Mybatis-Plus 公共配置
└── cache/           # Redis/Redisson 公共配置

taskify-<service>/src/main/java/com/taskify/<service>/   # 以 taskify-auth 为例
├── <Service>Application.java   # 启动类
├── controller/     # REST 控制器（仅编排，不含业务）
├── service/        # 业务接口 + impl/ 实现
├── mapper/         # Mybatis-Plus Mapper 接口
├── entity/         # 持久化实体（对应 taskify_ 表）
├── dto/            # 请求/响应 DTO
├── vo/             # 视图对象（如需）
├── config/         # 模块配置（Security、验证码等）
├── security/       # 认证/授权相关组件
├── exception/      # 业务异常
└── util/           # 模块内工具
```

**约定**：控制器不写业务逻辑，仅参数接收与响应编排；业务规则在 `service`；数据访问在 `mapper`；对象转换（MapStruct）发生在 `dto`↔`entity` 之间。

## 复杂度追踪

| 违规/复杂度项 | 为何需要 | 更简方案被否定的原因 |
|--------------|---------|---------------------|
| 微服务多模块 + 网关 + Nacos + OpenFeign + RocketMQ（对照原则 V） | 用户明确选定的架构方向：后续业务按服务拆分，需服务发现/网关/消息底座 | 单体/模块化单体更简单，但不符合用户已确定的微服务演进路线；第一阶段即铺设该底座以避免后续重构 |
| 消息队列 RocketMQ 于第一阶段引入 | 作为后续异步通信的底座，第一阶段先接入并验证连通 | 推迟到有实际异步需求时更简；但用户要求基础设施一次性到位 |
