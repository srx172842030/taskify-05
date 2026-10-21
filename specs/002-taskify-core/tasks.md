# Tasks: Taskify 核心协作功能（项目、任务、看板、评论与实时通知）

**输入**：设计文档来自 `/specs/002-taskify-core/`（plan.md、spec.md、research.md、data-model.md、contracts/、quickstart.md）

**前置**：plan.md（必需）、spec.md（用户故事）、research.md、data-model.md、contracts/

**测试**：按项目章程「测试标准（不可协商）」纳入测试任务（测试先行，先红后绿）。所有任务需遵循统一响应/错误码、`taskify_` 表前缀、UUID v7、无状态 JWT、默认拒绝授权等 001 底座约定。

**组织**：任务按用户故事分组，使每个故事可独立实现与验证。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件、无未完成依赖）
- **[Story]**：所属用户故事（US1…US6）
- 描述含精确文件路径

---

## Phase 1: Setup（共享基础设施）

**Purpose**：新增后端模块与前端依赖的初始化

- [ ] T001 在 `backend/pom.xml` 的 `<modules>` 注册 `taskify-project` 与 `taskify-notification`，并在 `<dependencyManagement>` 补充两模块坐标
- [ ] T002 [P] 创建 `taskify-project` 模块骨架：`backend/taskify-project/pom.xml`、`backend/taskify-project/src/main/java/com/taskify/project/ProjectApplication.java`、`src/main/resources/application.yml`、`Dockerfile`（参照 `taskify-example` 模板）
- [ ] T003 [P] 创建 `taskify-notification` 模块骨架：`backend/taskify-notification/pom.xml`、`backend/taskify-notification/src/main/java/com/taskify/notification/NotificationApplication.java`、`src/main/resources/application.yml`、`Dockerfile`
- [ ] T004 [P] 前端引入拖拽依赖：在 `frontend/package.json` 添加 `vue-draggable-plus` 并 `pnpm install`（用户指定）
- [ ] T005 在 `backend/docker-compose.app.yml` 新增 `taskify-project`、`taskify-notification` 两个服务定义（依赖 Nacos/RocketMQ/Redis/PostgreSQL）

---

## Phase 2: Foundational（阻塞性前置）

**Purpose**：任何用户故事开始前必须完成的核心底座

**⚠️ CRITICAL**：本阶段完成前，用户故事不得开工

- [ ] T006 网关路由：在 `backend/taskify-gateway` 配置 `/api/v1/projects`、`/api/v1/tasks`、`/api/v1/comments` 路由到 `taskify-project`，`/api/v1/notifications` 路由到 `taskify-notification`，并对 `/api/v1/notifications/stream` 的 SSE 响应放行（不缓冲、不超时截断）
- [ ] T007 迁移：`backend/taskify-project/src/main/resources/db/migration/V1__init_project_tables.sql` 创建 `taskify_project`、`taskify_task`、`taskify_comment` 三表（字段见 data-model.md）
- [ ] T008 迁移：`backend/taskify-notification/src/main/resources/db/migration/V1__init_notification_table.sql` 创建 `taskify_notification` 表（字段见 data-model.md）
- [ ] T009 [P] 预置用户（对应 US6）：`backend/taskify-auth` 新增 Flyway 迁移插入 5 名预置用户（1 产品经理、4 工程师，唯一 `username` + bcrypt `password_hash`，默认密码实现期约定）
- [ ] T010 [P] 预置项目（对应 US6）：`backend/taskify-project` 新增种子/迁移插入 3 个示例项目（`taskify_project` 各含 `name` + 可选 `description` + `created_by`）
- [ ] T011 [P] 前端基座：在 `frontend/src/router/index.ts` 新增 `/projects`、`/projects/:id` 路由；在 `frontend/src/services/` 新增 API 客户端基座（复用 001 的请求封装与统一响应/错误处理）

**Checkpoint**：底座就绪——用户故事可开始并行实现

---

## Phase 3: User Story 1 - 创建项目与项目总览 (Priority: P1) 🎯 MVP

**Goal**：登录用户创建项目、查看项目列表与详情（含 3 个示例项目）

**Independent Test**：登录 → 看到 3 个示例项目 → 新建项目（名称+描述）→ 出现在列表；空名称被一致校验错误拒绝

### Tests for User Story 1 ⚠️

> **NOTE：先写测试，确认其 FAIL 后再实现**

- [ ] T012 [P] [US1] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/ProjectIntegrationTest.java` 覆盖创建成功、空 `name` 返回 `PARAM_ERROR`、列表分页、详情 `NOT_FOUND`
- [ ] T013 [P] [US1] 前端组件测试：`frontend/tests/unit/ProjectList.spec.ts` 覆盖项目列表渲染与创建表单空名称校验

### Implementation for User Story 1

- [ ] T014 [P] [US1] 实体：`backend/taskify-project/src/main/java/com/taskify/project/entity/Project.java`（`id` UUID v7、`name` 非空 1–128、`description` 可空 ≤2000、`created_by` 非空、`created_at`/`updated_at`）
- [ ] T015 [P] [US1] Mapper：`backend/taskify-project/src/main/java/com/taskify/project/mapper/ProjectMapper.java`
- [ ] T016 [P] [US1] DTO：`backend/taskify-project/src/main/java/com/taskify/project/dto/CreateProjectRequest.java` 与 `ProjectResponse.java`
- [ ] T017 [US1] Service：`backend/taskify-project/src/main/java/com/taskify/project/service/ProjectService.java`（+ `impl/`）实现创建（`name` 去空白后非空校验）、分页列表、详情
- [ ] T018 [US1] Controller：`backend/taskify-project/src/main/java/com/taskify/project/controller/ProjectController.java` 暴露 `POST /api/v1/projects`、`GET /api/v1/projects`、`GET /api/v1/projects/{projectId}`
- [ ] T019 [US1] 前端 service：`frontend/src/services/projectApi.ts`（create/list/get）
- [ ] T020 [US1] 前端 store：`frontend/src/stores/projects.ts`
- [ ] T021 [US1] 前端页面：`frontend/src/pages/projects/ProjectListPage.vue`（项目列表 + `ProjectCreateForm.vue` 创建表单）并接入路由

**Checkpoint**：US1 可独立运行与验证

---

## Phase 4: User Story 2 - 创建并分配任务 (Priority: P1)

**Goal**：在项目内创建任务（标题必填、负责人可空）、查看列表/详情、修改任务；新任务默认「待办事项」；点击卡片打开详情抽屉

**Independent Test**：建项目后创建任务（负责人可空）→ 出现在看板「待办事项」列；修改任务生效；空标题被拒

### Tests for User Story 2 ⚠️

- [ ] T022 [P] [US2] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/TaskIntegrationTest.java` 覆盖创建默认 `TODO`、空标题 `PARAM_ERROR`、负责人可空、`assigneeId` 非存在用户报错
- [ ] T023 [P] [US2] 前端组件测试：`frontend/tests/unit/TaskDetail.spec.ts` 覆盖任务创建表单校验与任务详情抽屉渲染

### Implementation for User Story 2

- [ ] T024 [P] [US2] 实体：`backend/taskify-project/src/main/java/com/taskify/project/entity/Task.java`（`id`、`project_id` 非空、`title` 非空 1–200、`description` 可空 ≤5000、`assignee_id` 可空、`status` 非空默认 `TODO`、`created_by` 非空、时间戳）
- [ ] T025 [P] [US2] 状态枚举：`backend/taskify-project/src/main/java/com/taskify/project/entity/TaskStatus.java`（`TODO`/`IN_PROGRESS`/`IN_REVIEW`/`DONE`，含相邻关系）
- [ ] T026 [P] [US2] Mapper：`backend/taskify-project/src/main/java/com/taskify/project/mapper/TaskMapper.java`
- [ ] T027 [P] [US2] DTO：`backend/taskify-project/src/main/java/com/taskify/project/dto/CreateTaskRequest.java`、`UpdateTaskRequest.java`、`TaskResponse.java`
- [ ] T028 [US2] Service：`backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java`（+ `impl/`）实现创建（默认 `TODO`）、按 `status` 过滤列表、详情、修改（标题/描述/负责人，负责人可置空）
- [ ] T029 [US2] Controller：`backend/taskify-project/src/main/java/com/taskify/project/controller/TaskController.java` 暴露 `POST /api/v1/projects/{projectId}/tasks`、`GET /api/v1/projects/{projectId}/tasks`、`GET /api/v1/tasks/{taskId}`、`PATCH /api/v1/tasks/{taskId}`
- [ ] T030 [US2] 前端 service：`frontend/src/services/taskApi.ts`（create/list/get/update）
- [ ] T031 [US2] 前端 store：`frontend/src/stores/tasks.ts`
- [ ] T032 [US2] 前端页面：`frontend/src/pages/projects/ProjectDetailPage.vue`（默认看板视图 + 可切换到任务列表视图，见 FR-008）
- [ ] T033 [US2] 前端组件：`frontend/src/components/task/TaskDetailDrawer.vue`（点击卡片从右侧滑出抽屉，展示/编辑标题、描述、负责人）

**Checkpoint**：US1 + US2 可独立运行与验证

---

## Phase 5: User Story 3 - Kanban 看板与任务流转 (Priority: P1)

**Goal**：四列看板；桌面用 `vue-draggable-plus` 拖拽移动；仅限相邻列；移动端单列 + 列切换器 + 抽屉移动按钮（无横向滚动）

**Independent Test**：拖拽任务到相邻列成功、跨列被拒（回弹）；移动端单列无横向滚动、可在四列间切换

### Tests for User Story 3 ⚠️

- [ ] T034 [P] [US3] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/TaskMoveIntegrationTest.java` 覆盖相邻列移动成功、跨列（如 `TODO`→`DONE`）返回 `PARAM_ERROR`
- [ ] T035 [P] [US3] 前端组件测试：`frontend/tests/unit/board.spec.ts` 覆盖相邻列校验逻辑与移动控件（上一列/下一列按钮）
- [ ] T036 [P] [US3] E2E：`frontend/e2e/kanban.spec.ts` 覆盖桌面拖拽相邻列成功、跨列回弹

### Implementation for User Story 3

- [ ] T037 [US3] Service：在 `backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java` 增加 `move(taskId, toStatus)`，校验 `toStatus` 与当前 `status` 相邻（`TODO ↔ IN_PROGRESS ↔ IN_REVIEW ↔ DONE`），否则 `PARAM_ERROR`；成功后发布 `KANBAN_MOVED` 事件
- [ ] T038 [US3] Controller：在 `TaskController.java` 增加 `POST /api/v1/tasks/{taskId}/move`（请求体 `{ "toStatus": "..." }`）
- [ ] T039 [US3] 前端看板组件：`frontend/src/components/board/KanbanBoard.vue`、`KanbanColumn.vue`、`TaskCard.vue`，用 `vue-draggable-plus` 的 `VueDraggable` 实现四列间拖拽
- [ ] T040 [US3] 前端 composable：`frontend/src/composables/useBoard.ts`（拖拽落下 → 调 `move` 接口 → 相邻列校验 → 失败回弹提示）
- [ ] T041 [US3] 前端乐观更新：移动后本地乐观更新 `tasks` store，后端拒绝时回滚到原列（跨用户实时同步见 US5 T064）
- [ ] T042 [US3] 前端移动端：手机宽度下单列展示 + 顶部列切换器（Tailwind 断点，无横向滚动）；任务抽屉内提供「移动到上一列/下一列」按钮

**Checkpoint**：US1–US3 可独立运行与验证

---

## Phase 6: User Story 4 - 任务评论与协作 (Priority: P2)

**Goal**：在任务详情抽屉发表评论、查看按时间排序的评论；仅作者可编辑/删除自己的评论

**Independent Test**：发表评论成功、空评论被拒、作者编辑/删除成功、非作者操作被拒（`FORBIDDEN`）

### Tests for User Story 4 ⚠️

- [ ] T043 [P] [US4] 后端集成测试：`backend/taskify-project/src/test/java/com/taskify/project/CommentIntegrationTest.java` 覆盖发表、空 `content` `PARAM_ERROR`、仅作者可编辑/删除（非作者 `FORBIDDEN`）
- [ ] T044 [P] [US4] 前端组件测试：`frontend/tests/unit/CommentList.spec.ts` 覆盖评论列表渲染与编辑/删除按钮仅对作者可见

### Implementation for User Story 4

- [ ] T045 [P] [US4] 实体：`backend/taskify-project/src/main/java/com/taskify/project/entity/Comment.java`（`id`、`task_id` 非空、`author_id` 非空、`content` 非空 ≤5000、时间戳）
- [ ] T046 [P] [US4] Mapper：`backend/taskify-project/src/main/java/com/taskify/project/mapper/CommentMapper.java`
- [ ] T047 [P] [US4] DTO：`backend/taskify-project/src/main/java/com/taskify/project/dto/CreateCommentRequest.java`、`UpdateCommentRequest.java`、`CommentResponse.java`
- [ ] T048 [US4] Service：`backend/taskify-project/src/main/java/com/taskify/project/service/CommentService.java`（+ `impl/`）实现发表、按时间升序列表、编辑/删除（校验 `author_id == 当前用户`，否则 `FORBIDDEN`）
- [ ] T049 [US4] Controller：`backend/taskify-project/src/main/java/com/taskify/project/controller/CommentController.java` 暴露 `POST /api/v1/tasks/{taskId}/comments`、`GET /api/v1/tasks/{taskId}/comments`、`PATCH /api/v1/comments/{commentId}`、`DELETE /api/v1/comments/{commentId}`
- [ ] T050 [US4] 前端 service：`frontend/src/services/commentApi.ts`（create/list/update/delete）
- [ ] T051 [US4] 前端组件：`frontend/src/components/task/CommentList.vue` 与 `CommentForm.vue`（编辑/删除仅作者可见），接入 `TaskDetailDrawer.vue`

**Checkpoint**：US1–US4 可独立运行与验证

---

## Phase 7: User Story 5 - 实时通知 (Priority: P2)

**Goal**：任务发布/修改/看板变更时，负责人（及创建者）实时收到通知；铃铛下拉面板 + 未读角标 + 已读标记；看板跨用户实时同步

**Independent Test**：用户 A 建任务指派给 B，B 无需刷新即收到通知；A 修改/移动任务，B 继续收到；未读角标与已读状态正确；另一用户查看看板时移动同步更新

### Tests for User Story 5 ⚠️

- [ ] T052 [P] [US5] 后端集成测试：`backend/taskify-notification/src/test/java/com/taskify/notification/NotificationIntegrationTest.java` 覆盖事件→通知落库的接收者规则（负责人/创建者/去重/负责人留空）
- [ ] T053 [P] [US5] 前端组件测试：`frontend/tests/unit/NotificationBell.spec.ts` 覆盖铃铛下拉面板与未读角标
- [ ] T054 [P] [US5] E2E：`frontend/e2e/notification.spec.ts` 覆盖通知实时送达（无需刷新）与已读标记

### Implementation for User Story 5

- [ ] T055 [P] [US5] 实体：`backend/taskify-notification/src/main/java/com/taskify/notification/entity/Notification.java`（`id`、`recipient_id` 非空、`type` 枚举 `TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED`、`task_id` 可空、`message` 非空 ≤500、`read` 默认 false、`created_at`）
- [ ] T056 [P] [US5] Mapper：`backend/taskify-notification/src/main/java/com/taskify/notification/mapper/NotificationMapper.java`
- [ ] T057 [US5] 事件发布（`taskify-project`）：在 `TaskService` 创建/修改/移动处发布 RocketMQ 事件（`TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED`），topic 如 `taskify-task-events`
- [ ] T058 [US5] 事件消费（`taskify-notification`）：`.../consumer/TaskEventConsumer.java` 消费事件 → 按 data-model.md 规则计算接收者（负责人/创建者/去重/留空）→ 落库 `taskify_notification`
- [ ] T059 [US5] SSE 基础设施（`taskify-notification`）：在线连接注册表（`userId → 连接`）+ Spring WebFlux `text/event-stream` 推送
- [ ] T060 [US5] Controller：`backend/taskify-notification/src/main/java/com/taskify/notification/controller/NotificationController.java` 暴露 `GET /api/v1/notifications/stream`（SSE）、`GET /api/v1/notifications`、`GET /api/v1/notifications/unread-count`、`POST /api/v1/notifications/{id}/read`、`POST /api/v1/notifications/read-all`
- [ ] T061 [US5] 前端 service + composable：`frontend/src/services/notificationApi.ts` 与 `frontend/src/composables/useNotifications.ts`（`EventSource` 连接 + 自动重连）
- [ ] T062 [US5] 前端 store：`frontend/src/stores/notifications.ts`（列表 + 未读计数）
- [ ] T063 [US5] 前端组件：`frontend/src/components/notification/NotificationBell.vue` 与 `NotificationPanel.vue`（铃铛 + 未读角标 + 下拉列表 + 标记已读），接入 001 全局外壳顶栏
- [ ] T064 [US5] 看板实时同步：将 `KANBAN_MOVED` 事件经 SSE 推送给查看看板的用户，使其他用户视图无需刷新即同步（满足 SC-002）

**Checkpoint**：US1–US5 可独立运行与验证

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**：跨故事的质量收尾

- [ ] T065 [P] 运行 `quickstart.md` 全量端到端验证并修复发现的问题（6 个验证场景逐一通过）
- [ ] T066 [P] i18n 文案：在 `frontend/src/i18n/messages/zh-CN.json` 补齐看板、任务、评论、通知文案（中文）
- [ ] T067 [P] 补充边界用例测试：非相邻移动拒绝、非作者评论编辑/删除、负责人留空时通知规则（后端 + 前端）
- [ ] T068 文档与 CI：更新 `README.md` 与 `backend/docker-compose.app.yml` 说明；`.github/workflows/` 将 `taskify-project`、`taskify-notification` 纳入后端矩阵构建与测试

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**：无依赖，可立即开始
- **Foundational (Phase 2)**：依赖 Setup；**阻塞所有用户故事**
- **用户故事 (Phase 3–7)**：均依赖 Foundational 完成
  - US1（P1）→ US2（P1）→ US3（P1）→ US4（P2）→ US5（P2）按优先级顺序推进（任务依赖任务，评论依赖任务，通知依赖任务+移动）
- **Polish (Phase 8)**：依赖所有目标故事完成

### User Story Dependencies

- **US1（P1）**：Foundational 后可开始；无故事依赖
- **US2（P1）**：依赖 US1（需要项目存在）；可独立测试（给定一个项目）
- **US3（P1）**：依赖 US2（需要任务存在）；可独立测试（给定一个任务）
- **US4（P2）**：依赖 US2（需要任务存在）；可独立测试（给定一个任务）
- **US5（P2）**：依赖 US2/US3（需要任务创建/修改/移动事件）；引入 SSE 通道
- **US6（预置数据）**：已并入 Foundational（T009 预置用户、T010 预置项目），作为演示前置而非独立故事阶段

### Within Each User Story

- 测试（TDD）先写并确认 FAIL，再实现
- 实体/Mapper/DTO（可 [P] 并行）→ Service → Controller → 前端 service/store → 前端页面/组件
- 核心实现先于集成；故事完成后再进入下一优先级

### Parallel Opportunities

- Phase 1 与 Phase 2 中标记 [P] 的任务可并行（不同文件）
- 每个故事内：实体/Mapper/DTO 可并行；测试任务可并行
- Foundational 完成后，若人手充足，US1/US2/US3 的**后端与前端**可由不同成员并行（但注意 US2 依赖 US1 的实体、US3 依赖 US2 的 Task）

---

## Parallel Example: User Story 2

```bash
# 先并行启动测试（确认 FAIL）：
Task: "T022 [P] [US2] 后端集成测试 TaskIntegrationTest.java"
Task: "T023 [P] [US2] 前端组件测试 TaskDetail.spec.ts"

# 再并行创建实体/Mapper/DTO（不同文件）：
Task: "T024 [P] [US2] 实体 Task.java"
Task: "T025 [P] [US2] 状态枚举 TaskStatus.java"
Task: "T026 [P] [US2] TaskMapper.java"
Task: "T027 [P] [US2] DTO CreateTaskRequest/UpdateTaskRequest/TaskResponse"
```

---

## Implementation Strategy

### MVP First（仅 US1）

1. 完成 Phase 1 Setup + Phase 2 Foundational
2. 完成 Phase 3 US1（项目）
3. **STOP & VALIDATE**：独立测试 US1
4. 可部署/演示（已含预置用户与示例项目）

### Incremental Delivery

1. Setup + Foundational → 底座就绪
2. + US1（项目）→ 独立验证 → 演示（MVP）
3. + US2（任务）→ 独立验证 → 演示
4. + US3（看板拖拽）→ 独立验证 → 演示
5. + US4（评论）→ 独立验证 → 演示
6. + US5（实时通知 + 看板同步）→ 独立验证 → 演示
7. 每步新增价值，不破坏既有故事

### Parallel Team Strategy

- 团队共同完成 Setup + Foundational
- Foundational 完成后：
  - 成员 A：US1（项目）
  - 成员 B：US2（任务，依赖 A 的项目实体）
  - 成员 C：US4（评论，可与 B 并行，依赖 Task 实体定义）
  - US3 与 US5 待 B 的 Task + move 就绪后推进

---

## Notes

- [P] 任务 = 不同文件、无依赖；[Story] 标签用于追踪归属
- 每个用户故事应可独立完成与验证；跨故事依赖（US2 依赖 US1 等）已在依赖章节标注
- 测试先写并确认失败再实现（章程原则 II）
- 每个任务或逻辑组完成后提交
- 任何 Checkpoint 可停下来独立验证该故事
- 看板列 = 任务 `status` 枚举（`TODO`/`IN_PROGRESS`/`IN_REVIEW`/`DONE`）；「相邻列」= 枚举顺序相邻（详见 contracts/project-api.md）
- 评论不触发通知（规格未要求）；通知仅由任务创建/修改/看板变更触发
