---
description: "任务清单：表格视图与视图级筛选/分组/排序（第二视图）"
---

# 任务：表格视图与视图级筛选/分组/排序（第二视图）

**输入**：`/specs/005-table-view/` 下的设计文档（plan.md、spec.md、research.md、data-model.md、contracts/、quickstart.md）

**前置**：plan.md（必读）、spec.md（用户故事）、data-model.md、contracts/task-view-api.md、research.md

**测试**：本特性**包含测试任务**——项目章程原则 II「测试标准（不可协商）」要求所有新行为测试先行（红-绿-重构），故每个用户故事均以"先写失败测试"开篇。

**组织方式**：任务按用户故事分组，使每个故事可独立实现与测试。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件、无未完成依赖）
- **[Story]**：所属用户故事（US1–US5）
- 描述含精确文件路径

## 路径约定

- 后端：`backend/taskify-project/src/main/java/com/taskify/project/`（含 `entity/`、`dto/`、`controller/`、`service/`）与 `backend/taskify-project/src/main/resources/db/migration/`
- 前端：`frontend/src/`（`services/`、`stores/`、`composables/`、`components/`、`pages/`）与 `frontend/tests/unit/`、`frontend/e2e/`

---

## Phase 1: Setup（共享基础设施）

**目的**：确认既有组件与环境满足本特性，无需新增依赖

- [X] T001 确认所需 UI 组件（`table`/`checkbox`/`select`/`dropdown-menu`/`badge`/`separator`/`tabs`）已安装于 `frontend/src/components/ui/`；若缺失，先经 MCP 官方 registry 调研并安装，禁止自写基础控件（章程 VII）

---

## Phase 2: Foundational（阻塞前置——优先级字段）

**目的**：为任务模型新增"优先级"字段，是 US1（表格列/筛选/排序）、US2（批量编辑优先级）、US4（优先级泳道）的共同前提

**⚠️ 关键**：本阶段完成前，任何用户故事不得开始

- [X] T002 [P] 后端集成测试（先写、预期失败）：`backend/taskify-project/src/test/java/com/taskify/project/TaskPriorityIntegrationTest.java` —— 创建/修改任务携带 `priority` 可往返，非法值返回 `PARAM_ERROR`，缺省（null）保持未设置
- [X] T003 [P] 新增优先级枚举 `backend/taskify-project/src/main/java/com/taskify/project/entity/Priority.java`：`HIGH`/`MEDIUM`/`LOW`，提供非法值解析（返回 null 或抛 `PARAM_ERROR`）
- [X] T004 [P] `Task` 实体新增 `priority` 字段（`backend/taskify-project/src/main/java/com/taskify/project/entity/Task.java`），并新增 Flyway 迁移 `backend/taskify-project/src/main/resources/db/migration/V4__add_task_priority.sql`（`ALTER TABLE taskify_task ADD COLUMN priority VARCHAR(8);`，可空）
- [X] T005 [P] DTO 扩展 `priority` 字段：`TaskResponse.java`、`UpdateTaskRequest.java`、`CreateTaskRequest.java`（`backend/taskify-project/src/main/java/com/taskify/project/dto/`）；`priority` 可选、可置 null
- [X] T006 `TaskService.create`/`update` 支持 `priority`（`backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java`）：校验取值合法后透传，缺省保持 null（依赖 T003、T004）
- [X] T007 [P] 前端类型扩展：`Task` 接口与 `TaskPayload` 新增 `priority`（`frontend/src/services/taskApi.ts`）

**检查点**：优先级字段端到端可用（后端迁移 + 接口 + 前端类型），可开始用户故事

---

## Phase 3: User Story 1 - 表格视图浏览与筛选/分组/排序（Priority: P1）🎯 MVP

**目标**：项目详情可在看板与表格视图间切换，表格展示全部任务，支持"只看我的任务"筛选、"按负责人"分组、"按截止日"排序

**独立测试**：打开项目→切到表格视图→应用筛选/分组/排序，确认结果正确；无需批量编辑或泳道

### Tests for User Story 1（先写、预期失败）

- [X] T008 [P] [US1] 单测：`frontend/tests/unit/useTaskQuery.spec.ts` —— 筛选（"只看我的任务"=assigneeId==当前用户、状态/负责人/优先级）、分组（按负责人，未分配归"未分配"；按优先级，未设置归"无优先级"）、排序（按截止日升/降，空截止日排末尾）

### Implementation for User Story 1

- [X] T009 [US1] 实现 `useTaskQuery` 组合式函数（`frontend/src/composables/useTaskQuery.ts`）：纯函数式筛选→分组→组内排序，作用于共享 `tasks` 数组（依赖 T008 通过）
- [X] T010 [P] [US1] 实现 `TaskTableView` 表格视图组件（`frontend/src/components/table/TaskTableView.vue`）：桌面完整表格（复用 `table`/`badge`，列含标题/状态/负责人/优先级/截止时间），手机宽度降级为卡片列表、无页面级横向滚动
- [X] T011 [US1] 在 `ProjectDetailPage` 接入表格视图与视图切换（`frontend/src/pages/projects/ProjectDetailPage.vue`）：将既有"列表"入口升级为"表格"，装配 `useTaskQuery` 与筛选/分组/排序控件（依赖 T010）

**检查点**：US1 可独立演示——表格视图的浏览、筛选、分组、排序全部可用

---

## Phase 4: User Story 2 - 批量编辑任务字段（Priority: P1）

**目标**：表格视图多选任务，批量修改负责人/状态/截止时间/优先级，全或无、持久化，仅影响选中任务

**独立测试**：表格多选→批量改负责人/优先级/截止时间，确认全部选中更新；非法批量编辑整批拒绝且数据不变

### Tests for User Story 2（先写、预期失败）

- [X] T012 [P] [US2] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/TaskBatchUpdateIntegrationTest.java` —— 批量改字段成功；`taskIds` 空/跨项目/非法优先级/跨列/被阻塞推进 → 整批拒绝、数据不变
- [X] T013 [P] [US2] 单测：`frontend/tests/unit/BatchEditBar.spec.ts` —— 多选计数、批量改负责人/优先级/截止时间/状态、非法目标提示

### Implementation for User Story 2

- [X] T014 [P] [US2] 新增 `BatchUpdateTaskRequest` DTO（`backend/taskify-project/src/main/java/com/taskify/project/dto/BatchUpdateTaskRequest.java`）：`taskIds` 非空、`patch` 至少含一个可编辑字段
- [X] T015 [US2] `TaskService.batchUpdate`（`backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java`）：单事务全或无，逐任务复用 `update`/`move` 既有校验（时间、相邻列、硬阻塞），任一失败整体回滚（依赖 T014）
- [X] T016 [US2] `TaskController` 新增 `POST /api/v1/projects/{projectId}/tasks/batch-update`（`backend/taskify-project/src/main/java/com/taskify/project/controller/TaskController.java`）：返回更新后任务数组，校验失败返回 `PARAM_ERROR`/`TASK_BLOCKED`（依赖 T015）
- [X] T017 [P] [US2] 前端调用：`taskApi.batchUpdate(projectId, { taskIds, patch })`（`frontend/src/services/taskApi.ts`）
- [X] T018 [US2] 实现 `BatchEditBar` 批量编辑工具条（`frontend/src/components/table/BatchEditBar.vue`）：多选计数 + 批量改负责人/优先级/截止时间/状态（复用 `select`/`dropdown-menu`）（依赖 T017、T013 通过）
- [X] T019 [US2] 在 `TaskTableView` 接入行多选（表头全选 + 行复选）与批量编辑工具条（`frontend/src/components/table/TaskTableView.vue`）（依赖 T018）

**检查点**：US1 + US2 均可独立工作，批量编辑与表格视图联动

---

## Phase 5: User Story 3 - 两视图共享数据源与实时同步（Priority: P1）

**目标**：看板与表格共享同一任务数据源，任一视图修改后另一视图立即反映，并实时同步给其他在线用户

**独立测试**：表格改任务→看板立即更新；另一用户改任务→本侧无需刷新即同步

### Tests for User Story 3（先写、预期失败）

- [X] T020 [P] [US3] 单测：扩展 `frontend/tests/unit/notifications.spec.ts` —— 收到 `TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED` 通知时触发 `useTasks().reload()`

### Implementation for User Story 3

- [X] T021 [US3] 扩展 `notifications` store 的 SSE 处理器（`frontend/src/stores/notifications.ts`）：收到 `TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED` 时刷新当前项目任务（复用 `useTasks().reload()`，保留既有 `TASK_BLOCKED`/`TASK_UNBLOCKED` 刷新与通知展示行为）（依赖 T020 通过）
- [X] T022 [US3] 确认 `ProjectDetailPage` 看板与表格视图均读取同一 `useTasks()` 的 `tasks` 数组（`frontend/src/pages/projects/ProjectDetailPage.vue`），无各自独立副本

**检查点**：两视图共享单一数据源，跨用户实时同步生效

---

## Phase 6: User Story 4 - 看板泳道横向分组（Priority: P2）

**目标**：看板支持按负责人或优先级横向分组形成泳道，可关闭

**独立测试**：看板开启"按负责人"/"按优先级"泳道，确认各列内分组正确、未分配/无优先级归对应泳道，可关闭

### Tests for User Story 4（先写、预期失败）

- [X] T023 [P] [US4] 单测：`frontend/tests/unit/useSwimlane.spec.ts` —— 按负责人分组（未分配→"未分配"）、按优先级分组（未设置→"无优先级"）、关闭分组返回平铺

### Implementation for User Story 4

- [X] T024 [US4] 实现泳道分组逻辑（`frontend/src/composables/useSwimlane.ts`，可复用 `useTaskQuery` 的分组纯函数）（依赖 T023 通过）
- [X] T025 [P] [US4] 实现 `KanbanSwimlane` 泳道容器组件（`frontend/src/components/board/KanbanSwimlane.vue`）：展示泳道标题与组内任务卡片
- [X] T026 [US4] 改造 `KanbanBoard`/`KanbanColumn`（`frontend/src/components/board/KanbanBoard.vue`、`KanbanColumn.vue`）：支持按负责人或优先级在列内分组渲染泳道（依赖 T025）
- [X] T027 [US4] `ProjectDetailPage` 看板视图接入泳道切换（负责人/优先级/关闭）（`frontend/src/pages/projects/ProjectDetailPage.vue`）（依赖 T026）

**检查点**：US4 泳道功能独立可用，与表格视图同步

---

## Phase 7: User Story 5 - 视图配置独立与持久化（Priority: P2）

**目标**：每个视图（看板/表格）拥有独立的筛选/分组/排序配置，按用户持久化，可清除且不影响任务数据

**独立测试**：为表格设一套配置、为看板设泳道，确认互不影响，刷新/重进后各自恢复

### Tests for User Story 5（先写、预期失败）

- [X] T028 [P] [US5] 单测：`frontend/tests/unit/useViewConfig.spec.ts` —— 读写 `localStorage`（键 `taskify:view:{projectId}:{view}`）、恢复、清除/重置

### Implementation for User Story 5

- [X] T029 [US5] 实现 `useViewConfig`（`frontend/src/composables/useViewConfig.ts`）：封装筛选/分组/排序的读写、默认值与清除（依赖 T028 通过）
- [X] T030 [US5] 表格视图接入视图配置（`frontend/src/pages/projects/ProjectDetailPage.vue` 与 `TaskTableView.vue`）：筛选/分组/排序状态从 `useViewConfig` 读写
- [X] T031 [US5] 看板视图接入视图配置（`frontend/src/pages/projects/ProjectDetailPage.vue`）：泳道与筛选/排序状态从 `useViewConfig` 读写

**检查点**：两个视图的配置相互独立且可持久化，清除不影响任务数据

---

## Phase 8: Polish & 跨切面收尾

**目的**：跨用户故事的验证与质量门禁

- [X] T032 [P] 端到端测试：`frontend/e2e/table-view.spec.ts` —— "表格批量编辑→看板同步"主流程
- [X] T033 [P] 运行 `specs/005-table-view/quickstart.md` 全部验证场景并记录结果
- [X] T034 [P] 可访问性与响应式检查：表格视图与泳道视图满足 WCAG AA（语义化/键盘/对比度/读屏），手机宽度无页面级横向滚动（`frontend/src/components/table/TaskTableView.vue`、`frontend/src/components/board/KanbanSwimlane.vue`）
- [X] T035 [P] 在单任务创建/编辑入口暴露"优先级"字段：`frontend/src/components/task/TaskDetailDrawer.vue` 与 `frontend/src/pages/projects/ProjectDetailPage.vue` 的新建任务表单，使创建与单任务详情可设置优先级（补齐 FR-013 的单任务编辑入口；含组件测试，章程 II）
- [X] T036 性能度量：对照 `plan.md` 声明的性能预算（≤100 任务筛选/分组/排序 <100ms、批量编辑单请求完成），在变更前后度量并记录，确认无回归（章程 IV）
- [X] T037 质量门禁：`frontend` 下 `pnpm lint`、`pnpm build` 与 `backend` 下 `./mvnw -pl taskify-project test` 全部通过，无新增告警/错误（对应 SC-006）

---

## 依赖与执行顺序

### 阶段依赖

- **Setup（Phase 1）**：无依赖，立即开始
- **Foundational（Phase 2）**：依赖 Setup，**阻塞所有用户故事**（优先级字段）
- **用户故事（Phase 3–7）**：依赖 Foundational 完成
- **Polish（Phase 8）**：依赖所有目标故事完成

### 用户故事依赖

- **US1（P1）**：Foundational 后可开始，仅依赖优先级字段，不依赖其他故事
- **US2（P1）**：Foundational 后可开始；复用 US1 的表格视图（`TaskTableView`），建议在 US1 之后实现（T019 依赖 T010）
- **US3（P1）**：Foundational 后可开始；依赖既有 `useTasks`/`notifications` 结构
- **US4（P2）**：Foundational 后可开始；复用 `KanbanBoard`/`KanbanColumn`
- **US5（P2）**：依赖 US1（表格控件）与 US4（泳道）均已就绪后接入配置

> 注意：`ProjectDetailPage.vue` 被 US1/US3/US4/US5 共同修改，属跨故事文件；实现时按阶段顺序合并，避免并行冲突。

### 各故事内部顺序

- 测试（先写、失败）→ 模型/类型 → 服务/组合式函数 → 端点/组件 → 装配集成

### 并行机会

- Phase 2：T002/T003/T004/T005/T007 可并行（不同文件）
- Phase 4：T012/T013/T017 可并行；T014→T015→T016 串行
- Phase 6：T023/T025 可并行
- Phase 8：T032/T033/T034/T035 可并行；T036（性能度量）依赖 T035；T037（质量门禁）最后执行
- 各故事内的测试任务（T008/T012+T017/T020/T023/T028）可并行

---

## 并行示例：User Story 2

```bash
# 后端与前端测试先行（并行）：
Task: "后端集成测试 TaskBatchUpdateIntegrationTest.java"
Task: "前端单测 BatchEditBar.spec.ts"

# 后端 DTO 与前端调用层（并行，不同文件）：
Task: "BatchUpdateTaskRequest DTO"
Task: "taskApi.batchUpdate 调用"
```

---

## 实施策略

### MVP 优先（仅 User Story 1）

1. 完成 Phase 1（Setup）
2. 完成 Phase 2（Foundational：优先级字段）——**关键，阻塞一切**
3. 完成 Phase 3（US1 表格视图）
4. **停止并验证**：独立测试 US1
5. 可演示/交付

### 增量交付

1. Setup + Foundational → 基础就绪
2. + US1 → 独立测试 → 交付（MVP！）
3. + US2 → 独立测试 → 交付（批量编辑）
4. + US3 → 独立测试 → 交付（实时同步）
5. + US4 → 独立测试 → 交付（泳道）
6. + US5 → 独立测试 → 交付（配置持久化）

### 并行团队策略

- Foundational 完成前：单人/双人协作完成优先级字段
- Foundational 完成后：
  - 开发者 A：US1（表格视图）
  - 开发者 B：US2 后端（批量编辑端点）
  - 开发者 C：US4（泳道，先于 US2 前端装配）
- 注意 `ProjectDetailPage.vue` 的合并顺序，避免冲突

---

## 备注

- [P] 任务 = 不同文件、无未完成依赖
- [Story] 标签将任务映射到用户故事以便追溯
- 每个用户故事应可独立完成与测试
- 测试须先失败再实现（章程 II）
- 每个任务或逻辑组后提交一次
- 可在任一检查点停下验证故事
- 避免：模糊任务、同文件冲突、破坏独立性的跨故事依赖

---

## Phase 9: Convergence

- [X] T038 在 `TaskService.batchUpdate` 的状态变更分支中，当目标或当前列为 `DONE` 时，重算受影响任务的依赖任务阻塞状态并发出 `TASK_BLOCKED`/`TASK_UNBLOCKED` 事件（与单任务 `move` 行为一致）per FR-009/plan 决策 2 (partial)
- [X] T039 在 `TaskTableView.vue` 表格增加「开始时间」列（含手机卡片展示），使列覆盖 FR-002 明确列出的「开始时间」字段 per FR-002 (partial)
- [X] T040 为看板视图补充独立的筛选与排序配置（当前仅泳道分组），或经评审确认看板配置维度仅含泳道分组 per FR-016 (partial)
