# 实施计划：Taskify 核心协作功能（项目、任务、看板、评论与实时通知）

**分支**：`002-taskify-core` | **日期**：2026-10-21 | **规格**：[spec.md](./spec.md)

**输入**：功能规格来自 `/specs/002-taskify-core/spec.md`；用户补充技术指令：「引入 vue-draggable-plus 实现拖拽交互。」

## 摘要

在 001 阶段铺就的统一界面外壳与后端底座之上，交付 Taskify 的核心业务闭环：预置用户登录后可在项目内创建/分配任务、在看板四列（待办事项、进行中、审查中、已完成）之间移动任务、发表评论，并在任务发布/修改/看板变更时实时收到通知。

前端新增项目列表、看板（默认视图，可切换任务列表）、任务详情侧边抽屉、评论、铃铛下拉通知面板等页面与交互，看板拖拽使用 `vue-draggable-plus`（用户指定），并强制「仅限相邻列」约束、在手机宽度下降级为「单列 + 列切换器 + 移动按钮」。后端新增两个微服务：`taskify-project`（项目/任务/评论/看板）与 `taskify-notification`（通知持久化 + SSE 实时推送），事件经 RocketMQ 流转。沿用统一响应/错误码、页码分页、无状态 JWT、默认拒绝授权、Flyway 迁移与 `taskify_` 表前缀等 001 底座约定。

## 技术上下文

**语言/版本**：
- 前端：Vue 3（Composition API + `<script setup>`）、TypeScript（strict）
- 后端：Java 21（Maven 构建）

**主要依赖**：
- 前端：Tailwind CSS、shadcn-vue、vue-router、vee-validate + zod、vue-i18n、@vueuse/core、lucide-vue-next、**vue-draggable-plus（看板拖拽，用户指定）**、Pinia（状态管理，沿用 001 stores 目录）
- 后端：Spring Boot 3.5.x、Mybatis-Plus、Spring Validation、Spring Cloud Gateway（WebFlux）、Nacos、OpenFeign、MapStruct、springdoc-openapi + Knife4j、RocketMQ（事件流转）、SSE（Spring WebFlux `text/event-stream`）

**存储**：PostgreSQL（Flyway 迁移；UUID v7 应用生成；表前缀 `taskify_`）；Redis（Redisson，可选用作未读计数缓存）

**测试**：前端 Vitest + @vue/test-utils（单元/组件）+ Playwright（E2E）；后端 JUnit 5 + Testcontainers

**目标平台**：Linux 服务器（容器化部署）；前端为响应式 Web（桌面 + 手机宽度，移动端看板无横向滚动）

**项目类型**：Web 应用（前端 SPA + 后端微服务）

**性能目标**：看板移动后新列归属 <1s 反映到其他用户（无需刷新）；通知在事件后 <3s 送达；关键旅程端到端流畅

**约束**：无状态 JWT；授权默认拒绝；CORS 白名单；安全响应头；文档中文（章程 VI）；看板移动仅限相邻列；手机宽度无横向滚动

**规模/范围**：5 名预置用户、3 个示例项目、四列看板、2 个新增微服务、实时通知（SSE）

## 章程符合性检查

*门禁：Phase 0 研究前必须通过；Phase 1 设计后复检。*

- **I. 代码质量优先** — 通过：遵循 001 已确立的 ESLint/Prettier、Maven 规范与包结构约定；拖拽与相邻列校验逻辑封装为可测试的独立单元。
- **II. 测试标准（不可协商）** — 通过：拖拽/移动/评论权限/通知均需单元 + 组件 + 集成 + E2E 覆盖；「相邻列」校验与「非作者不可编辑/删除评论」为关键回归用例。
- **III. 用户体验一致性** — 通过：复用 001 设计 token、共享组件库与状态页；看板/抽屉/通知面板均用现有组件组装；移动端看板采用「单列 + 列切换器」以满足「无横向滚动」。
- **IV. 性能要求** — 通过：成功标准含性能预算（看板同步 <1s、通知送达 <3s），可度量。
- **V. 简洁性与可维护性** — **需说明（记录于复杂度追踪）**：新增 2 个微服务 + RocketMQ 事件 + SSE 实时通道引入复杂度；此为用户明确的微服务方向与实时通知需求。`vue-draggable-plus` 为用户指定依赖。
- **VI. 文档中文化** — 通过：本计划及全部生成文档使用中文。

## 项目结构

### 文档（本功能）

```text
specs/002-taskify-core/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出（拖拽、实时通知、服务拆分等决策）
├── data-model.md        # Phase 1 输出（项目/任务/评论/通知实体）
├── quickstart.md        # Phase 1 输出（端到端验证指南）
├── contracts/           # Phase 1 输出（接口契约）
│   ├── project-api.md       # 项目/任务/评论/看板接口
│   └── notification-api.md  # 通知接口 + SSE 实时流
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成，非本命令）
```

### 源码（仓库根目录）

```text
frontend/                        # Vue 3 SPA（沿用 001 布局）
├── src/
│   ├── components/
│   │   ├── ui/                  # shadcn-vue 源码（只读）
│   │   ├── board/               # 看板列、任务卡片、拖拽容器（vue-draggable-plus）
│   │   ├── task/                # 任务详情抽屉、评论列表/表单、移动控件
│   │   ├── notification/        # 铃铛下拉面板、通知条目、未读角标
│   │   └── project/             # 项目列表、项目创建表单
│   ├── composables/             # useBoard、useNotifications(SSE)、useTaskDetail 等
│   ├── layouts/                 # 复用 001 全局外壳（顶部导航 + 铃铛入口）
│   ├── pages/
│   │   ├── projects/            # 项目列表页、项目详情（看板/列表切换）
│   │   └── ...                  # 状态页复用 001
│   ├── router/                  # 新增 /projects、/projects/:id 路由
│   ├── services/                # project/task/comment/notification API 客户端 + SSE 连接
│   ├── stores/                  # Pinia：projects、tasks、notifications（未读计数）
│   ├── i18n/messages/zh-CN.json # 新增看板/任务/评论/通知文案
│   └── styles/tokens.css        # 复用 001 设计 token
├── e2e/                         # Playwright：看板拖拽、评论、通知 E2E
└── tests/unit/                  # Vitest：相邻列校验、评论权限、通知组件

backend/                         # Java 21 多模块 Maven（沿用 001）
├── taskify-common/              # 复用：统一响应、ErrorCode、校验、基础实体、日志/请求 ID
├── taskify-gateway/             # 复用：路由（新增 project/notification 路由）、SSE 转发
├── taskify-auth/                # 复用：用户、登录、令牌（预置 5 用户数据迁移）
├── taskify-project/             # 新增：项目/任务/评论/看板（含相邻列移动校验、事件发布）
├── taskify-notification/        # 新增：通知持久化、未读计数、SSE 实时推送（消费 RocketMQ 事件）
├── docker-compose.yml           # 复用：中间件（PostgreSQL、RocketMQ、Nacos、Redis）
├── docker-compose.app.yml       # 扩展：新增 project/notification 服务
└── pom.xml                      # 父 POM：新增 taskify-project、taskify-notification 模块
```

**结构决策**：沿用 001 的「前端 SPA + 后端多模块微服务」双目录结构。前端在现有 `src` 下新增 `board/`、`task/`、`notification/`、`project/` 业务组件目录与对应页面/路由/store/service；后端新增 `taskify-project`（项目聚合：项目/任务/评论/看板，聚合根为项目）与 `taskify-notification`（通知）两个模块，均遵循 `com.taskify.<模块>` 包结构（controller/service/mapper/entity/dto/config 等）。`taskify-common`、`taskify-gateway`、`taskify-auth` 复用并扩展（网关新增路由与 SSE 转发、auth 新增预置用户迁移）。数据表统一前缀 `taskify_`。

## 复杂度追踪

| 违规/复杂度项 | 为何需要 | 更简方案被否定的原因 |
|--------------|---------|---------------------|
| 新增 2 个微服务 + RocketMQ 事件 + SSE 实时通道（对照原则 V） | 用户明确微服务方向；实时通知（<3s、无需刷新）需服务间异步事件 + 单向实时通道 | 单体 + 轮询更简单，但不符合已确立的微服务路线与「实时」要求；SSE 比 WebSocket 更简（单向足够） |
| `vue-draggable-plus` 拖拽（对照原则 V） | 用户明确指定；桌面看板拖拽是最自然的交互 | 纯按钮移动更简，但用户体验显著下降；库为 Sortable.js 生态成熟方案 |
| 「相邻列」约束 + 移动端「无拖拽」降级 | 规格澄清要求仅限相邻列；章程要求手机宽度无横向滚动 | 自由拖拽 + 移动端横向滚动更简，但违反规格与章程 |
