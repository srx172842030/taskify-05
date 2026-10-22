# Bug Assessment: 排期/阻塞功能四类缺陷（实时刷新、拖拽回滚、提醒删除 404、阻塞任务继续流转）

- **Slug**: scheduling-dependency-bugs
- **Created**: 2026-10-22
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high（其中 F3 阻断「删除提醒」功能，F1/F2/F4 为功能性缺陷）

## Report（原文）

1. 任务阻塞状态在变化时页面没有实时刷新。
2. 阻塞任务被移动时，拖拽位置没有恢复。
3. 删除任务提醒时接口报错404。
4. 已经在进行中的阻塞任务仍然可以自由移动。

## Symptom

上述四项均源自 `003-task-scheduling-dependencies`（任务时间与依赖）新实现的缺陷：

- **F1**：当阻塞关系/阻塞状态变化（如阻塞任务移到「已完成」使依赖任务解除阻塞）时，前端看板/列表中的「被阻塞」标记不实时更新，需手动刷新。
- **F2**：将「被阻塞」任务拖入「进行中」时，移动被拒绝并弹出提示，但卡片仍停留在目标列，未回弹到原列。
- **F3**：在任务详情抽屉中删除提醒节点时，`DELETE /api/v1/reminders/{id}` 返回 404，删除无法完成。
- **F4**：一个已经处于「进行中」的任务若因新建阻塞关系而变为「被阻塞」，仍可继续向前移动到「审查中」等列。

## Reproduction

1. F1：项目内建 A、B；建立「A 阻塞 B」；将 A 移至「已完成」。观察 B 卡片/列表的「被阻塞」标记——预期实时消失，实际仍显示直到刷新。
2. F2：将「被阻塞」任务 B 从「待办事项」拖到「进行中」。预期弹提示且卡片回弹到「待办事项」；实际弹提示但卡片停留在「进行中」。
3. F3：打开任务详情抽屉 → 「提醒」→ 添加一个提醒 → 点击「删除」。预期删除成功；实际控制台报 `DELETE /api/v1/reminders/{id}` 404。
4. F4：建 A、B，先把 B 移到「进行中」，再建立「A 阻塞 B」（A 未完成），此时 B 仍可移动到「审查中」。

## Suspected Code Paths

**F1（阻塞状态不实时刷新）**
- `frontend/src/stores/notifications.ts` — `connectStream()` 收到 SSE 事件只调用 `prepend()` 追加到通知面板，未刷新 `tasks` store；`useNotifications` 亦未联动看板数据。
- `frontend/src/composables/useBoard.ts` — `moveTask()` 移动成功后仅 `upsert(updated)`（更新被移动任务自身），未刷新受阻塞状态变化影响的依赖任务。

**F2（拖拽不回弹）**
- `frontend/src/components/board/KanbanColumn.vue` — `VueDraggable` 的 `v-model="items"` 已在 `@add` 触发前把卡片移入目标列；`onAdd()` 调用 `moveTask()`，若其返回 false（被阻塞），`items` 未被还原。
- `frontend/src/composables/useBoard.ts` — `moveTask()` 对被阻塞任务在 `upsert` 之前 `return false`，未触发任何 store 变更，故列的 `watch(props.tasks)` 不重算，本地 `items` 保持错误状态。

**F3（提醒删除 404）**
- `backend/taskify-gateway/src/main/resources/application.yml:30` — 网关仅路由 `/api/v1/projects/**`、`/api/v1/tasks/**`、`/api/v1/comments/**`，**缺少 `/api/v1/reminders/**`**，故 `PATCH/DELETE /api/v1/reminders/{id}` 在网关层 404。
- `backend/taskify-project/src/main/java/com/taskify/project/controller/ReminderController.java` — `PATCH/DELETE /api/v1/reminders/{reminderId}` 为应用内路由，未被网关转发。（`GET/POST /api/v1/tasks/{id}/reminders` 因在 `/tasks/**` 下可正常访问。）

**F4（进行中的阻塞任务继续流转）**
- `backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java` — `move()` 仅在 `target == TaskStatus.IN_PROGRESS` 时判阻塞，故「进行中 → 审查中」（target=IN_REVIEW）不被拦截。

## Root Cause Hypothesis

- **F1**（confidence: high）：前端缺少「阻塞状态变化 → 刷新任务数据」的联动；SSE 只驱动通知面板，移动操作也不重取依赖任务。
- **F2**（confidence: high）：被阻塞任务在客户端提前 `return false`，未写 store，导致 `VueDraggable` 通过 `v-model` 对 `items` 的本地变更不被还原。
- **F3**（confidence: high）：网关漏配 `/api/v1/reminders/**` 路由，`/reminders/{id}` 的 `PATCH`/`DELETE` 被网关 404 拦截。
- **F4**（confidence: medium）：硬阻塞语义只覆盖「进入进行中」；规格仅要求「阻止开始」，对「已在进行中后变为阻塞」的继续流转未约束（属规格语义缺口，见 Open Questions）。

## Proposed Remediation

**F1 — 阻塞状态实时刷新**
- **Preferred**：`moveTask()` 成功后，若目标/当前列涉及 `DONE`（进/出完成），重新加载当前项目任务列表（`useTasks().load(projectId)`），使依赖任务的 `blocked`/`blockerIds` 刷新。同时，在 SSE 收到 `TASK_BLOCKED`/`TASK_UNBLOCKED` 通知时也触发一次任务列表刷新。
- **Alternatives**：让后端 move 响应返回受影响的依赖任务增量，前端按 ID `upsert`——更精细但需扩展契约。

**F2 — 拖拽回弹**
- **Preferred**：在 `KanbanColumn.onAdd()` 中 `await moveTask(...)`，返回 false 时以 `items.value = props.tasks.filter(t => t.status === props.status)` 还原本地列表（同时覆盖服务端拒绝的相邻列违规回滚一致性）。
- **Alternatives**：在 `useBoard.moveTask()` 被阻塞分支内仍触发一次「空 upsert」以驱动 watch 重算——不够直观，不建议。

**F3 — 提醒删除 404**
- **Preferred**：在 `backend/taskify-gateway/src/main/resources/application.yml` 的 project 路由 `Path` 中追加 `/api/v1/reminders/**`（与 `/api/v1/tasks/**` 同目标服务）。

**F4 — 进行中的阻塞任务禁止向前流转**
- **Preferred**：将 `move()` 的硬阻塞判断由「`target == IN_PROGRESS`」改为「`isBlocked(id) && target.order() > current.order()`」——被阻塞任务不得向更靠后的列推进，但允许回退（撤销开始）。
- **Alternatives**：保持现状并在 spec 中显式声明「仅拦截进入进行中」；或对「进行中变阻塞」任务自动回退到 TODO（影响更大，需产品确认）。

**Files likely to change**:
- `frontend/src/composables/useBoard.ts`
- `frontend/src/components/board/KanbanColumn.vue`
- `frontend/src/stores/notifications.ts`（或 `useNotifications.ts`）
- `backend/taskify-gateway/src/main/resources/application.yml`
- `backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java`

**Tests to add or update**:
- 前端：`useBoard` 被阻塞移动返回 false 后 `KanbanColumn.items` 回弹（组件测试）；SSE `TASK_UNBLOCKED` 触发任务刷新。
- 后端：`TaskService.move` 对「进行中且被阻塞 → 审查中」返回 `TASK_BLOCKED` 的集成用例；网关路由覆盖 `/api/v1/reminders/**`。

## Risks & Considerations

- F1 的「移动后重载列表」在任务多时引入一次全量请求，需注意性能（项目规模小，可接受）。
- F4 若改为「禁止向前流转」，需重新审视 spec 中「仅阻止开始」的表述是否同步更新（避免规格与实现漂移）。
- F3 的网关路由改动需重启网关生效；需确认 `/api/v1/reminders/**` 不应被误路由到其他服务。

## Open Questions

- [NEEDS CLARIFICATION: F4 的期望语义——「被阻塞任务」仅禁止进入「进行中」，还是禁止一切向前流转（含「进行中→审查中」「审查中→已完成」）？]
- [NEEDS CLARIFICATION: F1 的实时刷新期望粒度——移动操作后主动重载列表，还是依赖 SSE 通知驱动刷新？]
