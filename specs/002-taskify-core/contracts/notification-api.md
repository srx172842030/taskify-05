# 接口契约：通知（taskify-notification 服务）

由 `taskify-notification` 服务提供，经网关路由暴露。响应/错误结构遵循 `../001-infrastructure-foundation/contracts/api-conventions.md`。所有接口需登录（`Authorization: Bearer <token>`）。

## 实时通知流（SSE）

`GET /api/v1/notifications/stream`

- 前端用浏览器 `EventSource` 建立连接；服务端以 `text/event-stream` 单向推送。
- 鉴权：JWT（`Authorization` 头或 `token` 查询参数，实现期确认）。
- 每收到一条新通知，推送一个 `event` 消息（`data` 为通知对象 JSON）。
- 连接断开后由 `EventSource` 自动重连；重连期间产生的事件由持久化通知列表兜底（离线不丢，见规格边界情况）。

## 查询通知列表

`GET /api/v1/notifications?page=1&size=20`

成功响应 `data`：分页结构，通知对象数组：

```json
{
  "id": "…",
  "type": "KANBAN_MOVED",
  "taskId": "…",
  "message": "任务「实现登录页」已移动到「进行中」",
  "read": false,
  "createdAt": "2026-10-21T10:05:00Z"
}
```

## 查询未读数量

`GET /api/v1/notifications/unread-count`

成功响应 `data`：

```json
{ "count": 3 }
```

用途：铃铛图标未读角标。

## 标记单条已读

`POST /api/v1/notifications/{notificationId}/read`

成功响应 `data`：

```json
{ "read": true }
```

## 标记全部已读（可选）

`POST /api/v1/notifications/read-all`

成功响应 `data`：

```json
{ "updated": 3 }
```

## 通知类型枚举

| `type` | 触发事件 |
|--------|---------|
| `TASK_CREATED` | 新任务发布 |
| `TASK_UPDATED` | 任务被修改 |
| `KANBAN_MOVED` | 任务看板列变更 |

## 事件流转（内部，非对外契约）

1. `taskify-project` 在任务创建/修改/看板移动时发布领域事件到 RocketMQ topic（`taskify-task-events`）。
2. `taskify-notification` 消费事件，按 `data-model.md` 的接收者规则确定接收者，落库 `taskify_notification`，并向在线接收者 SSE 推送。
