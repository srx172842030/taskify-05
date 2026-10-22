# Tasks: Taskify 任务时间与依赖关系（开始/截止时间、多提醒与阻塞关系）

**输入**：设计文档来自 `/specs/003-task-scheduling-dependencies/`（plan.md、spec.md、research.md、data-model.md、contracts/、quickstart.md）

**前置**：plan.md（必需）、spec.md（用户故事）、research.md、data-model.md、contracts/

**测试**：按项目章程「测试标准（不可协商）」纳入测试任务（测试先行，先红后绿）。所有任务需遵循统一响应/错误码、`taskify_` 表前缀、UUID v7、无状态 JWT、默认拒绝授权等 001 底座约定，及 002 的「仅相邻列」看板约定。

**组织**：任务按用户故事分组，使每个故事可独立实现与验证。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件、无未完成依赖）
- **[Story]**：所属用户故事（US1…US4）
- 描述含精确文件路径

---

## Phase 1: Setup（共享基础设施）

**Purpose**：所有故事共用的错误码与通知类型扩展

- [X] T001 在 `backend/taskify-common/src/main/java/com/taskify/common/error/ErrorCode.java` 新增错误码 `TASK_BLOCKED`（语义：尝试开始一个被阻塞的任务）
- [X] T002 [P] 在 `backend/taskify-notification/src/main/java/com/taskify/notification/entity/NotificationType.java` 新增 `REMINDER`、`TASK_BLOCKED`、`TASK_UNBLOCKED` 三个枚举值

---

## Phase 2: Foundational（阻塞性前置）

**Purpose**：数据模型（迁移 + 实体 + Mapper），任何用户故事开始前必须完成

**⚠️ CRITICAL**：本阶段完成前，用户故事不得开工

- [X] T003 迁移：`backend/taskify-project/src/main/resources/db/migration/V3__add_task_scheduling.sql` —— ALTER `taskify_task` 增加 `start_at TIMESTAMPTZ NULL`、`start_all_day BOOLEAN NOT NULL DEFAULT false`、`due_at TIMESTAMPTZ NULL`、`due_all_day BOOLEAN NOT NULL DEFAULT false`；新建 `taskify_reminder` 与 `taskify_task_dependency` 两表（字段与约束见 data-model.md）
- [X] T004 [P] 实体：在 `backend/taskify-project/src/main/java/com/taskify/project/entity/Task.java` 增加 `startAt`、`startAllDay`、`dueAt`、`dueAllDay` 字段（`OffsetDateTime`/`Boolean`）
- [X] T005 [P] 实体：新建 `backend/taskify-project/src/main/java/com/taskify/project/entity/Reminder.java`（`taskId`、`mode`、`triggerAt`、`offsetMinutes`、`anchor`、`fired`）与 `backend/taskify-project/src/main/java/com/taskify/project/entity/ReminderMode.java`（`ABSOLUTE`/`RELATIVE`）
- [X] T006 [P] 实体：新建 `backend/taskify-project/src/main/java/com/taskify/project/entity/TaskDependency.java`（`blockerId` 被依赖/阻塞者、`dependentId` 依赖/被阻塞者）
- [X] T007 [P] Mapper：新建 `backend/taskify-project/src/main/java/com/taskify/project/mapper/ReminderMapper.java`
- [X] T008 [P] Mapper：新建 `backend/taskify-project/src/main/java/com/taskify/project/mapper/TaskDependencyMapper.java`

**Checkpoint**：底座就绪——用户故事可开始实现

---

## Phase 3: User Story 1 - 设置开始时间与截止时间 (Priority: P1) 🎯 MVP

**Goal**：任务可独立设置开始/截止时间，两者均可「全天」或「具体时刻」，可留空；截止不早于开始；在列表/看板/详情一致展示。

**Independent Test**：编辑任务设置开始/截止（含全天切换、只设其一）→ 保存 → 三处一致展示；截止早于开始被 `PARAM_ERROR` 拒绝且数据不变。

### Tests for User Story 1 ⚠️

> **NOTE：先写测试，确认其 FAIL 后再实现**

- [X] T009 [P] [US1] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/TaskTimeIntegrationTest.java` 覆盖创建/修改带时间字段、`dueAt` 早于 `startAt` 返回 `PARAM_ERROR`、全天标记一致、只设其一
- [X] T010 [P] [US1] 前端组件测试：`frontend/tests/unit/TaskTimeFields.spec.ts` 覆盖时间输入、全天切换、截止早于开始的前端校验提示

### Implementation for User Story 1

- [X] T011 [US1] DTO：在 `backend/taskify-project/src/main/java/com/taskify/project/dto/CreateTaskRequest.java` 增加 `startAt`、`startAllDay`、`dueAt`、`dueAllDay`（均可空）
- [X] T012 [US1] DTO：在 `backend/taskify-project/src/main/java/com/taskify/project/dto/UpdateTaskRequest.java` 增加同 4 个时间字段（可单独置 null 清空）
- [X] T013 [P] [US1] DTO：在 `backend/taskify-project/src/main/java/com/taskify/project/dto/TaskResponse.java` 增加 `startAt`、`startAllDay`、`dueAt`、`dueAllDay` 并从 `Task` 映射
- [X] T014 [US1] Service：在 `backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java` 的 create/update 增加时间校验（截止不早于开始，全天按日期比较）与持久化
- [X] T015 [US1] Controller：在 `backend/taskify-project/src/main/java/com/taskify/project/controller/TaskController.java` 的 create/update 传递时间字段
- [X] T016 [US1] 前端 service：在 `frontend/src/services/taskApi.ts` 增加时间字段类型与请求载荷
- [X] T017 [US1] 前端表单：在 `frontend/src/components/task/TaskDetailDrawer.vue` 增加开始/截止时间输入（具体时刻 + 全天切换）
- [X] T018 [US1] 前端展示：在 `frontend/src/components/board/TaskCard.vue` 与任务列表（`frontend/src/pages/projects/ProjectDetailPage.vue` 列表视图）展示时间字段

**Checkpoint**：US1 可独立运行与验证

---

## Phase 4: User Story 2 - 建立任务阻塞关系 (Priority: P1)

**Goal**：同项目任务间建立显式阻塞关系（硬阻塞）；被依赖未完成 → 依赖任务标记「不可开始」、阻止开始（move→IN_PROGRESS 拒绝）并通知；完成 → 解除并通知。

**Independent Test**：建立「A 阻塞 B」→ B 标记不可开始并列出 A → 尝试把 B 移到「进行中」被 `TASK_BLOCKED` 拒绝 → A 完成 → B 解除标记且负责人收到通知；跨项目/自/环依赖被 `PARAM_ERROR` 拒绝。

### Tests for User Story 2 ⚠️

- [X] T019 [P] [US2] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/TaskDependencyIntegrationTest.java` 覆盖建立关系、跨项目/自/环/重复拒绝、硬阻塞 move 返回 `TASK_BLOCKED`、完成解除、移回未完成重新阻塞
- [X] T020 [P] [US2] 后端集成测试：扩展 `backend/taskify-notification/src/test/java/com/taskify/notification/NotificationIntegrationTest.java` 覆盖 `TASK_BLOCKED`/`TASK_UNBLOCKED` 消费与负责人为空不产生通知

### Implementation for User Story 2

- [X] T021 [US2] DTO：新建 `backend/taskify-project/src/main/java/com/taskify/project/dto/CreateDependencyRequest.java`（`blockerTaskId`）与 `DependencyResponse.java`（`blockers`/`dependents`）
- [X] T022 [US2] Service：新建 `backend/taskify-project/src/main/java/com/taskify/project/service/DependencyService.java`（建立/删除关系；校验同项目、非自阻塞、无环、不重复；读时计算「被阻塞」）
- [X] T023 [US2] Controller：新建 `backend/taskify-project/src/main/java/com/taskify/project/controller/DependencyController.java` 暴露 `GET/POST/DELETE /api/v1/tasks/{taskId}/dependencies`
- [X] T024 [US2] Service：在 `backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java` 的 move 增加硬阻塞校验——目标 `IN_PROGRESS` 且任务「被阻塞」时抛 `TASK_BLOCKED`
- [X] T025 [US2] 事件：在 `backend/taskify-project/src/main/java/com/taskify/project/mq/TaskEvent.java` 与 `TaskEventProducer.java` 增加 `TASK_BLOCKED`/`TASK_UNBLOCKED`；在任务移动（进/出 `DONE`）与依赖建立/删除时计算受影响任务并发布
- [X] T026 [US2] 通知消费：在 `backend/taskify-notification/src/main/java/com/taskify/notification/service/NotificationService.java` 与 `mq/TaskEventConsumer.java` 处理 `TASK_BLOCKED`/`TASK_UNBLOCKED`（负责人非空时落库 + SSE）
- [X] T027 [US2] 前端 service/store：新增 `frontend/src/services/dependencyApi.ts`；扩展 `frontend/src/stores/tasks.ts` 维护任务的 `blocked`/`blockerIds`
- [X] T028 [US2] 前端 UI：在 `frontend/src/components/task/TaskDetailDrawer.vue` 增加阻塞关系编辑；在 `frontend/src/components/board/TaskCard.vue` 显示「不可开始」徽标与阻塞警示（列出阻塞任务）
- [X] T029 [US2] 前端交互：在 `frontend/src/composables/useBoard.ts` 中，被阻塞任务拖到「进行中」时被阻止并提示（对接 `TASK_BLOCKED` 错误）

**Checkpoint**：US1 + US2 均可独立工作

---

## Phase 5: User Story 3 - 配置多个提醒节点 (Priority: P2)

**Goal**：任务可配置多个提醒节点（绝对时间点或相对提前量），到期向负责人发送提醒通知；可增删改；已触发不重复、已过期不补发。

**Independent Test**：配置一个绝对 + 一个相对提醒 → 保存 → 到期后负责人收到 `REMINDER` 通知且不重复；相对提醒锚定字段缺失时创建被 `PARAM_ERROR` 拒绝。

### Tests for User Story 3 ⚠️

- [X] T030 [P] [US3] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/ReminderIntegrationTest.java` 覆盖绝对/相对创建、`ABSOLUTE` 缺 `triggerAt` / `RELATIVE` 缺 `offsetMinutes>0`+`anchor` 返回 `PARAM_ERROR`、锚定字段缺失拒绝、调度触发置 `fired`
- [X] T031 [P] [US3] 后端集成测试：扩展 `backend/taskify-notification/src/test/java/com/taskify/notification/NotificationIntegrationTest.java` 覆盖 `REMINDER` 消费

### Implementation for User Story 3

- [X] T032 [US3] DTO：新建 `backend/taskify-project/src/main/java/com/taskify/project/dto/ReminderRequest.java` 与 `ReminderResponse.java`
- [X] T033 [US3] Service：新建 `backend/taskify-project/src/main/java/com/taskify/project/service/ReminderService.java`（创建/修改/删除；`ABSOLUTE`/`RELATIVE` 校验；相对解析 `anchor=START→start_at-offset`、`anchor=DUE→due_at-offset`）
- [X] T034 [US3] Controller：新建 `backend/taskify-project/src/main/java/com/taskify/project/controller/ReminderController.java` 暴露 `GET/POST /api/v1/tasks/{taskId}/reminders`、`PATCH/DELETE /api/v1/reminders/{reminderId}`
- [X] T035 [US3] 调度器：新建 `backend/taskify-project/src/main/java/com/taskify/project/scheduler/ReminderScheduler.java`（Spring `@Scheduled` 周期扫描 `fired=false` 且解析触发时间 ≤ now，置 `fired=true` 后发布事件；扫描周期 ≤ 1s，满足 SC-003 的 3s 送达预算）
- [X] T036 [US3] 事件：在 `backend/taskify-project/src/main/java/com/taskify/project/mq/TaskEvent.java` 与 `TaskEventProducer.java` 增加 `REMINDER_DUE` 事件（携带 `taskId`、`assigneeId`、`title`）
- [X] T037 [US3] 通知消费：在 `backend/taskify-notification/src/main/java/com/taskify/notification/service/NotificationService.java` 与 `mq/TaskEventConsumer.java` 处理 `REMINDER`（负责人非空时落库 + SSE）
- [X] T038 [US3] 前端：新建 `frontend/src/components/task/ReminderEditor.vue`、`frontend/src/composables/useReminders.ts`、`frontend/src/services/reminderApi.ts`；在 `TaskDetailDrawer.vue` 集成多提醒增删改（绝对/相对）

**Checkpoint**：US1 + US2 + US3 均可独立工作

---

## Phase 6: User Story 4 - 现有视图内一致展示（无日历视图）(Priority: P2)

**Goal**：时间字段与阻塞状态在看板卡片、任务列表、详情抽屉三处一致展示；不引入日历/时间轴视图；手机宽度可读、无横向滚动。

**Independent Test**：设置时间与阻塞关系后逐一查看看板卡片/列表/详情，三处一致；系统中无日历/时间轴视图；手机宽度下信息可读且无横向滚动。

### Implementation for User Story 4

- [X] T039 [US4] 前端一致性：在 `frontend/src/components/board/TaskCard.vue`、`frontend/src/components/task/TaskDetailDrawer.vue`、任务列表三处复用同一时间/阻塞展示组件或工具函数（避免漂移）
- [X] T040 [US4] 无日历视图 + 手机宽度：确认不新增日历/时间轴视图；对齐 `frontend/src/styles/tokens.css` 与响应式规范，保证手机宽度下排期与阻塞信息可读、无横向滚动
- [X] T041 [US4] E2E：`frontend/e2e/` 新增 Playwright 用例覆盖「被阻塞任务拖拽被阻止 + 警示」「A 完成后 B 解除阻塞并收到通知」「手机宽度无横向滚动」
- [X] T042 [US4] i18n：在 `frontend/src/i18n/messages/zh-CN.json` 新增时间/全天/提醒/阻塞/通知相关文案

**Checkpoint**：全部用户故事可独立工作

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**：横切多故事的收尾工作

- [X] T043 [P] 补充单元测试：时间校验（截止不早于开始、全天按日期比较）、阻塞状态读时计算、提醒相对解析的纯逻辑单元测试（`backend/taskify-project/src/test/java/com/taskify/project/` 下新增对应单元测试类）
- [X] T044 运行 `specs/003-task-scheduling-dependencies/quickstart.md` 全部场景验证（curl 校验 + 前端手动/自动化验证）
- [X] T045 更新文档：在 `README.md` 与相关文档补充本功能说明（中文，章程 VI）

---

## 依赖与执行顺序

### 阶段依赖

- **Setup（Phase 1）**：无依赖，可立即开始
- **Foundational（Phase 2）**：依赖 Setup，阻塞所有用户故事
- **用户故事（Phase 3+）**：全部依赖 Foundational 完成；故事间可并行或按优先级顺序（US1 → US2 → US3 → US4）
- **Polish（Phase 7）**：依赖所需用户故事完成

### 用户故事依赖

- **US1（P1）**：Foundational 后可开始，不依赖其他故事
- **US2（P1）**：Foundational 后可开始；不依赖 US1（仅需任务实体，已由 002 提供）
- **US3（P2）**：Foundational 后可开始；不依赖 US1/US2
- **US4（P2）**：建议在 US1/US2 前端完成后进行（其校验依赖时间与阻塞展示已就绪）

### 共享文件协调（跨故事）

- `TaskEvent.java` / `TaskEventProducer.java`：US2（T025）与 US3（T036）都扩展，并行时需协调或顺序提交
- `NotificationService.java` / `TaskEventConsumer.java`：US2（T026）与 US3（T037）都扩展，同理
- 并行开发时建议：US2 与 US3 由同一人负责或串行这两处小改动

### 故事内顺序

- 测试（T009/T010 等）先写并确认 FAIL，再实现
- 实体/Mapper（Foundational）→ DTO → Service → Controller → 前端
- 故事完成后再进入下一优先级

### 并行机会

- Phase 2 的实体/Mapper 任务（T004–T008）可并行
- 每个故事的测试任务（如 T009/T010）可并行
- Foundational 完成后，US1/US2/US3 可由不同人并行（注意上述共享文件协调）

---

## 并行示例：User Story 1

```bash
# 并行启动 US1 的两个测试任务：
Task: "T009 后端集成测试 TaskTimeIntegrationTest.java"
Task: "T010 前端组件测试 TaskTimeFields.spec.ts"

# 并行创建 DTO 与响应（不同文件）：
Task: "T011 CreateTaskRequest.java 增加时间字段"
Task: "T012 UpdateTaskRequest.java 增加时间字段"
Task: "T013 TaskResponse.java 增加时间字段"
```

---

## 实施策略

### MVP 优先（仅 User Story 1）

1. 完成 Phase 1 Setup + Phase 2 Foundational
2. 完成 Phase 3 US1（任务开始/截止时间）
3. **停止并验证**：US1 独立测试通过
4. 可先演示/交付

### 增量交付

1. Setup + Foundational → 底座就绪
2. 加 US1 → 独立验证 → 交付（MVP：时间字段）
3. 加 US2 → 独立验证 → 交付（硬阻塞依赖）
4. 加 US3 → 独立验证 → 交付（多提醒）
5. 加 US4 → 独立验证 → 交付（一致展示 + 无日历视图）
6. 每个故事在不破坏前序故事的前提下增值

### 多人协作策略

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后：
   - 开发者 A：US1（时间字段）
   - 开发者 B：US2（阻塞关系，注意与 US3 的共享文件协调）
   - 开发者 C：US3（提醒，注意共享文件）
3. US4 由任一人在前端就绪后进行

---

## 备注

- `[P]` 任务 = 不同文件、无未完成依赖
- `[Story]` 标签将任务映射到用户故事以便追踪
- 每个用户故事应可独立完成与验证
- 实现前先确认测试 FAIL（红-绿-重构）
- 每个任务或逻辑组完成后提交
- 可在任一 Checkpoint 停止以独立验证故事
- 避免：模糊任务、同文件冲突、破坏故事独立性的跨故事依赖
