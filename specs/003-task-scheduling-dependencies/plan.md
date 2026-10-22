# 实施计划：Taskify 任务时间与依赖关系（开始/截止时间、多提醒与阻塞关系）

**分支**：`003-task-scheduling-dependencies` | **日期**：2026-10-22 | **规格**：[spec.md](./spec.md)

**输入**：功能规格来自 `/specs/003-task-scheduling-dependencies/spec.md`（含澄清：阻塞为硬阻塞、提醒支持绝对+相对、开始时间不门槛、阻塞仅同项目、阻塞状态变化通知）。

## 摘要

在 002（项目/任务/看板/评论/通知）之上，为任务补齐「时间表达」能力：任务支持独立的开始时间与截止时间（均可全天或具体时刻）、可配置多个提醒节点（绝对时间点或相对提前量）、任务间可建立显式的硬阻塞关系（被依赖任务未完成时，依赖任务被标记「不可开始」、阻止其开始，并在阻塞/解除时通知负责人）。全程不引入日历视图，排期与依赖信息在看板卡片、任务列表、详情抽屉中一致表达。

后端在 `taskify-project` 扩展任务时间字段、新增提醒节点与阻塞关系两张表与调度器/硬阻塞校验，并扩展 RocketMQ 事件；`taskify-notification` 新增 `REMINDER`/`TASK_BLOCKED`/`TASK_UNBLOCKED` 通知类型。前端扩展任务表单与详情抽屉、任务卡片与列表的排期/阻塞展示、提醒编辑与阻塞编辑交互，并强制「不可开始」的移动阻断。沿用 001/002 的统一响应/错误码、无状态 JWT、Flyway、`taskify_` 表前缀、SSE 通知链路。

## 技术上下文

**语言/版本**：
- 前端：Vue 3（Composition API + `<script setup>`）、TypeScript（strict）
- 后端：Java 21（Maven 构建）

**主要依赖**：
- 前端：Tailwind CSS、shadcn-vue、vue-router、vee-validate + zod、vue-i18n、@vueuse/core、lucide-vue-next、vue-draggable-plus、Pinia
- 后端：Spring Boot 3.5.x、Mybatis-Plus、Spring Validation、Nacos、OpenFeign、MapStruct、springdoc-openapi + Knife4j、RocketMQ、Spring `@Scheduled`（提醒调度）、SSE

**存储**：PostgreSQL（Flyway 迁移；UUID v7 应用生成；表前缀 `taskify_`；`TIMESTAMPTZ`/`OffsetDateTime`）

**测试**：前端 Vitest + @vue/test-utils（单元/组件）+ Playwright（E2E）；后端 JUnit 5 + Testcontainers

**目标平台**：Linux 服务器（容器化部署）；前端响应式 Web（桌面 + 手机宽度）

**项目类型**：Web 应用（前端 SPA + 后端微服务）

**性能目标**：提醒/阻塞通知在事件后 <3s 送达（无需刷新）；看板移动结果 <1s 反映；关键旅程端到端流畅

**约束**：无状态 JWT；授权默认拒绝；文档中文（章程 VI）；无日历/时间轴视图；阻塞仅同项目；移动仅相邻列 + 硬阻塞；手机宽度无横向滚动

**规模/范围**：5 名预置用户、3 个示例项目；`taskify-project` 扩展 + `taskify-notification` 枚举扩展；新增 2 张表（提醒、阻塞关系）

## 章程符合性检查

*门禁：Phase 0 研究前必须通过；Phase 1 设计后复检。*

- **I. 代码质量优先** — 通过：沿用 001/002 包结构与 ESLint/Prettier、Maven 规范；时间校验、提醒解析、阻塞状态计算封装为独立可测试单元。
- **II. 测试标准（不可协商）** — 通过：时间校验、提醒两种形态解析、硬阻塞 move 拒绝、循环/自/跨项目依赖拒绝、阻塞状态通知均需单元 + 集成 + E2E 覆盖；默认测试先行。
- **III. 用户体验一致性** — 通过：复用 002 任务卡片/列表/详情抽屉与通知面板，在其上增量展示排期与阻塞信息；警示徽标/文案走共享组件与设计 token；手机宽度无横向滚动。
- **IV. 性能要求** — 通过：成功标准含 <3s 通知送达、<1s 看板同步的可度量预算；提醒调度周期在实现期按预算校准。
- **V. 简洁性与可维护性** — **需说明（记录于复杂度追踪）**：新增提醒调度（周期扫描）+ 阻塞状态变化事件引入一定复杂度；「被阻塞」读时计算、不落布尔列，是满足一致性下的最简方案。
- **VI. 文档中文化** — 通过：本计划及全部生成文档使用中文。

## 项目结构

### 文档（本功能）

```text
specs/003-task-scheduling-dependencies/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出（时间存储、提醒调度、阻塞建模等决策）
├── data-model.md        # Phase 1 输出（任务扩展、提醒、阻塞关系实体）
├── quickstart.md        # Phase 1 输出（端到端验证指南）
├── contracts/           # Phase 1 输出（接口契约）
│   ├── task-scheduling-api.md  # 任务时间/提醒/阻塞关系接口（增量）
│   └── notification-api.md     # 通知类型扩展（增量）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成，非本命令）
```

### 源码（仓库根目录）

```text
frontend/                        # Vue 3 SPA（沿用 001/002）
├── src/
│   ├── components/
│   │   ├── board/               # 扩展：任务卡片展示排期/阻塞徽标
│   │   ├── task/                # 扩展：详情抽屉时间字段、提醒编辑、阻塞编辑、移动阻断提示
│   │   └── notification/        # 复用：新增通知类型展示
│   ├── composables/             # 新增 useReminders、useDependencies；扩展 useTaskDetail/useBoard
│   ├── pages/projects/          # 扩展：任务列表排期列
│   ├── services/                # 扩展 task/reminder/dependency API 客户端
│   ├── stores/                  # 扩展 tasks store（时间/阻塞状态）
│   └── i18n/messages/zh-CN.json # 新增时间/提醒/阻塞文案
├── e2e/                         # Playwright：硬阻塞拖拽被阻止、提醒通知、手机宽度
└── tests/unit/                  # Vitest：时间校验、阻塞徽标、提醒表单

backend/                         # Java 21 多模块 Maven（沿用 001/002）
├── taskify-common/              # 复用：新增 TASK_BLOCKED 错误码
├── taskify-project/             # 扩展：任务时间字段、提醒节点、阻塞关系、硬阻塞校验、提醒调度
├── taskify-notification/        # 扩展：NotificationType 新增 3 类、消费新事件
├── docker-compose.app.yml       # 无新服务（复用 project/notification）
└── pom.xml                      # 无新模块（复用）
```

**结构决策**：不新增微服务，在既有 `taskify-project`（任务聚合）内扩展时间/提醒/阻塞领域，`taskify-notification` 仅扩展通知类型与事件消费。前端在既有组件目录内增量扩展，新增 `useReminders`/`useDependencies` 组合式函数。数据表统一前缀 `taskify_`，由 `taskify-project` 的 Flyway 迁移管理。

## 复杂度追踪

| 违规/复杂度项 | 为何需要 | 更简方案被否定的原因 |
|--------------|---------|---------------------|
| 提醒调度（周期扫描 + 事件）引入后台任务 | 相对提醒需随任务时间动态解析，任意时刻触发需后台调度 | RocketMQ 延迟消息延迟等级粗粒度；Quartz 动态任务生命周期复杂；周期扫描最简且满足 <3s 预算 |
| 阻塞状态变化需计算受影响任务并发布事件 | 硬阻塞 + 状态通知要求「阻塞/解除」可感知并送达负责人 | 仅界面读时标记（不通知）更简，但澄清已明确要求通知 |
| 「被阻塞」读时计算而非落布尔列 | 保证与阻塞关系/状态实时一致，避免双写漂移 | 落布尔列需额外事务维护，违反简洁性（章程 V） |
