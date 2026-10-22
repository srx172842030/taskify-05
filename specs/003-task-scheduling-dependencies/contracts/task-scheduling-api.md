# 接口契约：任务时间、提醒与阻塞关系（taskify-project 服务）

由 `taskify-project` 服务提供，经网关路由暴露。本文档为 `../002-taskify-core/contracts/project-api.md` 的**增量扩展**；响应/错误结构、鉴权与分页约定沿用 `../001-infrastructure-foundation/contracts/api-conventions.md`。所有接口需登录（`Authorization: Bearer <token>`）。

## 任务对象（扩展字段）

任务对象在 002 基础上新增：

```json
{
  "id": "…", "projectId": "…", "title": "…", "description": "…",
  "assigneeId": "…", "status": "TODO", "createdBy": "…",
  "startAt": "2026-10-25T09:00:00Z", "startAllDay": false,
  "dueAt": "2026-10-28T18:00:00Z", "dueAllDay": false,
  "blocked": true,
  "blockerIds": ["…", "…"],
  "createdAt": "…", "updatedAt": "…"
}
```

- `startAt`/`dueAt`：可空；全天时分别为当日 00:00 / 23:59:59，配合 `*AllDay` 标记展示。
- `blocked`：读时计算的「不可开始」状态（存在未完成 blocker 时为 true）。
- `blockerIds`：阻塞该任务的任务 ID 列表（被依赖任务）。

## 创建/修改任务（扩展时间字段）

### 创建任务

`POST /api/v1/projects/{projectId}/tasks`

请求体在 002 基础上新增可选字段：

```json
{ "title": "实现登录页", "description": "…", "assigneeId": "3f2a...",
  "startAt": "2026-10-25T09:00:00Z", "startAllDay": false,
  "dueAt": "2026-10-28T18:00:00Z", "dueAllDay": false }
```

### 修改任务

`PATCH /api/v1/tasks/{taskId}`

请求体新增可选时间字段（同上；均可单独置 null 清空）。仍触发 `TASK_UPDATED` 事件。

**校验**：截止不早于开始（全天按日期比较）；`*AllDay` 与时间值形态一致。违反返回 `PARAM_ERROR`。

## 移动任务（硬阻塞）

`POST /api/v1/tasks/{taskId}/move`

请求体（同 002）：

```json
{ "toStatus": "IN_PROGRESS" }
```

规则在 002「仅相邻列」之上新增：当 `toStatus = IN_PROGRESS` 且任务「被阻塞」（存在未完成 blocker）时，返回错误码 `TASK_BLOCKED`（数据不变）；否则成功返回更新后的任务对象，并触发 `KANBAN_MOVED` 事件。

## 提醒节点

### 查询任务提醒列表

`GET /api/v1/tasks/{taskId}/reminders`

成功响应 `data`：提醒对象数组：

```json
{ "id": "…", "taskId": "…", "mode": "RELATIVE", "triggerAt": null,
  "offsetMinutes": 1440, "anchor": "DUE", "fired": false, "createdAt": "…" }
```

### 创建提醒

`POST /api/v1/tasks/{taskId}/reminders`

绝对时间点：

```json
{ "mode": "ABSOLUTE", "triggerAt": "2026-10-27T09:00:00Z" }
```

相对提前量：

```json
{ "mode": "RELATIVE", "offsetMinutes": 1440, "anchor": "DUE" }
```

**校验**：`ABSOLUTE` 要求 `triggerAt` 必填；`RELATIVE` 要求 `offsetMinutes > 0` 且 `anchor ∈ {START, DUE}`；相对提醒所锚定时间字段未设置时返回 `PARAM_ERROR`。成功响应 `data`：提醒对象。

### 修改提醒

`PATCH /api/v1/reminders/{reminderId}`

请求体同创建（整体替换形态/取值）。成功响应 `data`：更新后的提醒对象。

### 删除提醒

`DELETE /api/v1/reminders/{reminderId}`

成功响应 `data`：`{ "deleted": true }`。

## 阻塞关系

### 查询任务依赖

`GET /api/v1/tasks/{taskId}/dependencies`

成功响应 `data`：

```json
{ "blockers": [ { "taskId": "…", "title": "…", "status": "DONE" } ],
  "dependents": [ { "taskId": "…", "title": "…", "status": "TODO" } ] }
```

- `blockers`：阻塞本任务的任务（被依赖任务）。
- `dependents`：被本任务阻塞的任务（依赖任务）。

### 建立阻塞关系

`POST /api/v1/tasks/{dependentId}/dependencies`

请求体：

```json
{ "blockerTaskId": "…" }
```

语义：`blockerTaskId` 阻塞 `dependentId`（`dependentId` 依赖 `blockerTaskId`）。

**校验**：两任务同项目；`blockerTaskId != dependentId`；不形成环；不重复。违反返回 `PARAM_ERROR`。成功响应 `data`：`{ "created": true }`。

### 移除阻塞关系

`DELETE /api/v1/tasks/{dependentId}/dependencies/{blockerTaskId}`

成功响应 `data`：`{ "deleted": true }`。

## 错误码（新增）

| 错误码 | 含义 |
|--------|------|
| `TASK_BLOCKED` | 尝试开始一个被阻塞（存在未完成依赖）的任务 |

## 事件流转（内部，非对外契约）

1. `taskify-project` 在任务移动（进/出 `DONE`）、阻塞关系创建/删除时，计算受影响任务的阻塞状态变化，发布 `TASK_BLOCKED`/`TASK_UNBLOCKED` 事件到 RocketMQ。
2. 提醒调度器扫描到期提醒，发布 `REMINDER_DUE` 事件。
3. `taskify-notification` 消费上述事件，生成 `TASK_BLOCKED`/`TASK_UNBLOCKED`/`REMINDER` 通知并 SSE 推送（见 `notification-api.md`）。
