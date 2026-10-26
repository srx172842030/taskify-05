# 实施计划：表格视图与视图级筛选/分组/排序（第二视图）

**分支**：`005-table-view` | **日期**：2026-10-26 | **规格**：[spec.md](./spec.md)

**输入**：来自 `/specs/005-table-view/spec.md` 的功能规格说明

## 摘要

为 Taskify 在既有看板视图之外新增**表格视图**：以表格呈现项目全部任务，支持按字段筛选（"只看我的任务"）、分组（"按负责人"）与排序（"按截止日"），支持多选任务批量编辑（负责人/状态/截止时间/优先级）。看板与表格共享同一任务数据源，任一视图修改后另一视图实时同步。看板支持按负责人或优先级横向分组形成泳道。为支撑"按优先级"泳道，任务模型新增一个可空的"优先级"字段（经 `/speckit-specify` 澄清确认的唯一模型变更）。

技术方案要点（详见 [research.md](./research.md)）：
- **优先级**：任务表新增可空 `priority` 列（`HIGH`/`MEDIUM`/`LOW`，空值＝未设置），贯穿实体/DTO/前端类型与迁移。
- **批量编辑**：新增单个原子批量接口（全量校验、全或无），复用既有单任务校验规则（相邻列 + 硬阻塞）。
- **跨用户实时同步**：扩展现有 SSE 通知通道——收到 `TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED` 时刷新当前项目任务（现有仅对阻塞事件刷新）。
- **视图配置持久化**：浏览器 `localStorage`（个人级、按项目、按视图），不新增后端存储。
- **UI**：复用已安装的 shadcn-vue `table`/`checkbox`/`select`/`dropdown-menu`/`badge`/`separator` 等组件，无新增自定义控件。

## 技术上下文

**语言/版本**：前端 TypeScript 5.7 + Vue 3.5；后端 Java 17 + Spring Boot 3.x

**主要依赖**：Vue 3、Pinia、Vue Router、Vite、Tailwind CSS、shadcn-vue（reka-ui）、vue-draggable-plus、vue-i18n、vee-validate + zod；Spring Boot Web/Security/Validation、MyBatis Plus、Flyway、PostgreSQL、RocketMQ

**存储**：PostgreSQL（`taskify_task` 新增 `priority` 列，Flyway 迁移）；视图配置存于浏览器 `localStorage`

**测试**：Vitest + @vue/test-utils（前端单测）、Playwright（前端端到端）；JUnit 5 + Testcontainers PostgreSQL（后端集成测试）

**目标平台**：Web（桌面 + 手机响应式，手机宽度无页面级横向滚动）

**项目类型**：Web 应用（前端 SPA + 微服务后端）

**性能目标**：≤100 任务的筛选/分组/排序与视图切换在 100ms 内完成；批量编辑以单请求原子完成（避免 N 次逐条请求）

**约束**：WCAG AA 可访问性；手机宽度无页面级横向滚动；UI 组件复用（章程 VII）；测试先行、80% 覆盖率（章程 II）；文档中文化（章程 VI）

**规模/范围**：5 名用户、3 个项目、每项目 ≤100 任务（沿用分页 `size=100`）

## 章程核对（Constitution Check）

*门禁：Phase 0 研究前必须通过；Phase 1 设计后复检。*

| 原则 | 核对结论 |
|------|---------|
| I. 代码质量优先 | 通过。批量校验/移动逻辑复用既有 `TaskService` 规则，避免复制；筛选/分组/排序作为纯函数集中一处。类型检查（`vue-tsc`）与 lint 作为门禁。 |
| II. 测试标准（不可协商） | 通过（需在实现中落实）。新增优先级、批量编辑、筛选/分组/排序、泳道、配置持久化均需单测；批量接口与优先级列需集成测试；端到端覆盖"表格编辑→看板同步"。 |
| III. 用户体验一致性 | 通过。复用既有设计系统与 shadcn-vue 组件；手机宽度表格降级为卡片/精简列，无横向滚动；遵循 WCAG AA。 |
| IV. 性能要求 | 通过。声明预算：≤100 任务下筛选/分组/排序 <100ms；批量编辑单请求完成；避免逐条请求。 |
| V. 简洁性与可维护性 | 通过。视图配置用 `localStorage`（不新增后端服务）；批量接口收敛为一个端点；优先级为简单枚举列。 |
| VI. 文档中文化 | 通过。本计划与全部设计产物、代码注释、提交信息均使用中文。 |
| VII. UI 组件复用（不可复制实现） | 通过。所需 `table`/`checkbox`/`select`/`dropdown-menu`/`badge`/`separator`/`tabs` 等组件均已安装（来自官方 registry，见 004）；本特性不新增任何自定义基础控件。 |

**门禁结果**：全部通过，无需要论证的违规项。

## 项目结构

### 文档（本特性）

```text
specs/005-table-view/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出（技术决策）
├── data-model.md        # Phase 1 输出（数据模型）
├── quickstart.md        # Phase 1 输出（端到端验证指南）
├── contracts/           # Phase 1 输出（接口契约）
│   └── task-view-api.md
└── tasks.md             # Phase 2 输出（/speckit-tasks，非本命令产出）
```

### 源码（仓库根）

```text
backend/taskify-project/
├── src/main/java/com/taskify/project/
│   ├── entity/Task.java            # 新增 priority 字段
│   ├── entity/Priority.java        # 新增：优先级枚举
│   ├── dto/TaskResponse.java       # 响应新增 priority
│   ├── dto/UpdateTaskRequest.java  # 请求新增 priority
│   ├── dto/BatchUpdateTaskRequest.java  # 新增：批量编辑请求
│   ├── controller/TaskController.java   # 新增批量编辑端点
│   └── service/TaskService.java    # 新增 batchUpdate，复用既有校验
└── src/main/resources/db/migration/V4__add_task_priority.sql  # 新增

frontend/src/
├── services/taskApi.ts             # Task/TaskPayload 新增 priority；批量编辑调用
├── stores/tasks.ts                 # 复用共享数据源（不变）
├── composables/useViewConfig.ts    # 新增：视图配置持久化
├── composables/useTaskQuery.ts     # 新增：筛选/分组/排序纯函数
├── components/board/KanbanBoard.vue      # 支持泳道分组
├── components/board/KanbanSwimlane.vue   # 新增：泳道容器
├── components/table/TaskTableView.vue    # 新增：表格视图
├── components/table/BatchEditBar.vue     # 新增：批量编辑工具条
└── pages/projects/ProjectDetailPage.vue  # 视图切换、配置装配、SSE 同步
```

**结构决策**：沿用既有单体 SPA + `taskify-project` 微服务结构；本特性在既有目录内新增文件，不引入新服务或新项目。

## 复杂度跟踪

> 仅在章程核对存在需论证的违规时填写。本特性无违规项，以下仅记录经确认的取舍。

| 取舍 | 为何需要 | 更简替代方案被否的原因 |
|------|---------|----------------------|
| 任务模型新增 `priority` 列 | 支持"按优先级"泳道与筛选/排序（用户确认） | 不引入优先级则无法满足已确认需求；仅按负责人泳道不足以满足"或优先级" |
| 新增单个批量编辑端点 | 保证批量编辑"全或无"原子性与单请求性能（FR-008/FR-009/性能预算） | 前端逐条 PATCH 无法保证原子回滚，且违反"不产生部分生效" |
| 视图配置存 `localStorage` | 个人偏好、轻量、无需后端协调（FR-016） | 后端存储需新增表/接口/迁移，超出个人偏好所需，违背简洁性（YAGNI） |
