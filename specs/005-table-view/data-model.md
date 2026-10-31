# 数据模型：表格视图与视图级筛选/分组/排序（第二视图）

本文档仅列 005 的新增/变更数据；未列出的实体与约定沿用 `../002-taskify-core/data-model.md` 与 `../003-task-scheduling-dependencies/data-model.md`（表前缀 `taskify_`、主键 UUID v7 应用生成、Flyway 迁移、时间戳 `TIMESTAMPTZ`/`OffsetDateTime`）。

## 变更总览

| 实体 | 存储位置 | 变更 |
|------|---------|------|
| 任务 `taskify_task` | taskify-project（PostgreSQL） | 新增可空 `priority` 列 |
| 视图配置 | 前端 `localStorage` | 新增（非数据库实体） |

> 看板泳道、筛选/分组/排序均为前端派生视图，不产生持久化数据；批量编辑复用任务表，不新增表。

## 任务（taskify_task，新增列）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `priority` | VARCHAR(8) | 可空 | 优先级，取值 `HIGH`/`MEDIUM`/`LOW`；`NULL`＝未设置（默认） |

**取值与展示**：

| 存储值 | 前端标签 |
|--------|---------|
| `HIGH` | 高 |
| `MEDIUM` | 中 |
| `LOW` | 低 |
| `NULL` | 无优先级 |

**校验规则**：

- 仅接受 `HIGH`/`MEDIUM`/`LOW`/`null` 之一；非法值返回 `PARAM_ERROR`。
- 空值含义＝"未设置"，在泳道分组中归入「无优先级」泳道，在排序中可排末尾（可选规则）。

**兼容性**：新字段可空、无默认值语义负担；既有创建/修改请求不携带 `priority` 时保持 `NULL`，不影响既有接口与数据。

## 视图配置（前端 `localStorage`，非数据库）

为每个视图（看板/表格）、每个项目、每个用户（浏览器）保存一份配置，结构：

```json
{
  "filters": { "assignee": null, "status": null, "priority": null, "mineOnly": false },
  "groupBy": null | "assignee" | "priority",
  "sortBy": "dueAt" | "createdAt" | "title" | "priority",
  "sortDir": "asc" | "desc"
}
```

- 键：`taskify:view:{projectId}:{view}`，`view ∈ {board, table}`。
- **筛选**：`mineOnly`＝"只看我的任务"；`assignee`/`status`/`priority` 为具体条件。
- **分组**：`groupBy` 为空时平铺，否则按对应维度分组。
- **排序**：`sortBy` + `sortDir`；空值（如空截止日）固定排末尾。
- 清除/重置即删除该键或恢复默认值，不影响任务数据（FR-018）。

## 关系

- 本特性不新增实体间关系；泳道/分组为 `taskify_task` 的派生视图。
- 批量编辑作用于 `taskify_task` 的多行，不改动 `taskify_task_dependency` 或 `taskify_reminder`。

## 状态机扩展

- 看板四列状态机不变（`TODO ↔ IN_PROGRESS ↔ IN_REVIEW ↔ DONE`，仅相邻）。
- 批量设置 `status` 等价于"移动每个选中任务到目标列"，逐任务沿用既有相邻列与硬阻塞校验；整批全或无（任一失败即整体回滚）。

## 迁移

- `taskify-project` 用 Flyway 新增 `V4__add_task_priority.sql`：`ALTER TABLE taskify_task ADD COLUMN priority VARCHAR(8);`（可空，无需回填）。
