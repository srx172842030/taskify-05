# 接口契约：通知（taskify-notification 服务）— 003 扩展

本文档为 `../002-taskify-core/contracts/notification-api.md` 的**增量扩展**。SSE 流、查询列表、未读计数、标记已读等接口不变，仅扩展通知类型与事件流转。

## 通知类型枚举（新增）

在 002 既有 `TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED` 之外新增：

| `type` | 触发事件 | 接收者 |
|--------|---------|--------|
| `REMINDER` | 提醒节点到达 | 任务负责人（非空时） |
| `TASK_BLOCKED` | 任务被阻塞 | 任务负责人（非空时） |
| `TASK_UNBLOCKED` | 任务解除阻塞 | 任务负责人（非空时） |

通知对象示例（`GET /api/v1/notifications` 与 SSE `data` 同构）：

```json
{
  "id": "…",
  "type": "TASK_UNBLOCKED",
  "taskId": "…",
  "message": "任务「实现登录页」的阻塞已解除，可以开始",
  "read": false,
  "createdAt": "2026-10-25T09:00:00Z"
}
```

## 事件流转（内部，非对外契约）

1. `taskify-project` 发布三类新事件到 RocketMQ：
   - `REMINDER_DUE`：提醒调度器扫描到到期提醒（携带 `taskId`、`assigneeId`、`title`）。
   - `TASK_BLOCKED` / `TASK_UNBLOCKED`：任务移动（进/出 `DONE`）或阻塞关系创建/删除导致阻塞状态变化（携带被阻塞/解除的任务 `taskId`、`assigneeId`、`title`）。
2. `taskify-notification` 消费事件，按接收者规则（负责人非空）落库 `taskify_notification`，并向在线接收者 SSE 推送。
3. 去重与负责人为空规则与 002 一致：负责人为空时不产生上述通知；同一事件若负责人与创建者相同仅一条。
