# 接口契约：任务优先级与批量编辑（taskify-project 服务）

由 `taskify-project` 服务提供，经网关路由暴露。本文档为 `../003-task-scheduling-dependencies/contracts/task-scheduling-api.md`（及 002/003 任务契约）的**增量扩展**；响应/错误结构、鉴权与分页约定沿用 `../001-infrastructure-foundation/contracts/api-conventions.md`。所有接口需登录（`Authorization: Bearer <token>`）。

## 任务对象（扩展字段）

任务对象在 003 基础上新增：

```json
{
  "id": "…", "projectId": "…", "title": "…", "description": "…",
  "assigneeId": "…", "status": "TODO", "createdBy": "…",
  "priority": "HIGH",
  "startAt": "…", "startAllDay": false, "dueAt": "…", "dueAllDay": false,
  "blocked": false, "blockerIds": [],
  "createdAt": "…", "updatedAt": "…"
}
```

- `priority`：可空，取值 `HIGH`/`MEDIUM`/`LOW`，`null`＝未设置。

## 创建/修改任务（扩展优先级字段）

- `POST /api/v1/projects/{projectId}/tasks` 与 `PATCH /api/v1/tasks/{taskId}` 请求体新增可选字段：

```json
{ "priority": "HIGH" }
```

- `priority` 可传 `null` 清空；非法值返回 `PARAM_ERROR`。修改仍触发 `TASK_UPDATED` 事件。

## 批量编辑任务

`POST /api/v1/projects/{projectId}/tasks/batch-update`

**语义**：对同一项目内的若干任务一次性应用同一变更，**全或无**——任一任务校验失败则整批拒绝，不产生部分生效。

请求体：

```json
{
  "taskIds": ["…", "…"],
  "patch": {
    "assigneeId": "…",
    "priority": "HIGH",
    "startAt": "2026-10-25T09:00:00Z", "startAllDay": false,
    "dueAt": "2026-10-28T18:00:00Z", "dueAllDay": false,
    "status": "IN_PROGRESS"
  }
}
```

- `taskIds`：非空数组；所有任务必须属于 `projectId`（否则 `PARAM_ERROR`）。
- `patch`：至少包含一个可编辑字段；字段语义与单任务编辑一致（可空字段传 `null` 清空）。
- `patch.status`：视为"移动每个选中任务到目标列"，逐任务校验相邻列与硬阻塞；任一任务跨列或被阻塞则整批返回 `TASK_BLOCKED`（或 `PARAM_ERROR`），数据不变。

成功响应 `data`：更新后的任务对象数组：

```json
[ { "id": "…", "priority": "HIGH", "status": "IN_PROGRESS", "…" }, { "…": "…" } ]
```

**校验汇总**：

| 情形 | 错误码 |
|------|--------|
| `taskIds` 为空 | `PARAM_ERROR` |
| 任一任务不属于 `projectId` / 不存在 | `PARAM_ERROR` / `NOT_FOUND` |
| `patch` 不含任何可编辑字段 | `PARAM_ERROR` |
| `priority` 非法值 | `PARAM_ERROR` |
| 截止早于开始 | `PARAM_ERROR` |
| `status` 非相邻列 | `PARAM_ERROR` |
| `status` 目标为 `IN_PROGRESS` 且任务被阻塞 | `TASK_BLOCKED` |

**副作用**：每个受影响任务按既有规则发布对应事件（字段变更 → `TASK_UPDATED`；状态变更 → `KANBAN_MOVED`），阻塞状态随之重算（进/出 `DONE` 或状态变更时）。

## 错误码（新增）

无新增错误码；批量编辑复用既有 `PARAM_ERROR` 与 `TASK_BLOCKED`。

## 事件流转（内部，非对外契约）

批量编辑沿用既有任务事件主题 `taskify-task-events`：字段变更发 `TASK_UPDATED`，状态变更发 `KANBAN_MOVED`（及进/出 `DONE` 引发的 `TASK_BLOCKED`/`TASK_UNBLOCKED`）。前端在收到任务类事件时刷新当前项目任务，实现跨用户视图同步（见 `research.md` 决策 3）。
