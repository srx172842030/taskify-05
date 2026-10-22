# 数据模型：Taskify 任务时间与依赖关系

本文档只列 003 功能的新增/变更实体；未列出的实体与约定沿用 `../002-taskify-core/data-model.md`（表前缀 `taskify_`、主键 UUID v7 应用生成、Flyway 迁移、时间戳 `TIMESTAMPTZ`/`OffsetDateTime`）。

## 变更总览

| 实体 | 表名 | 所属服务 | 变更 |
|------|------|---------|------|
| 任务 | `taskify_task` | taskify-project | 新增开始/截止时间 + 全天标记 |
| 提醒节点 | `taskify_reminder` | taskify-project | 新增 |
| 阻塞关系 | `taskify_task_dependency` | taskify-project | 新增 |
| 通知 | `taskify_notification` | taskify-notification | 新增 3 个类型（无表结构变更） |

## 任务（taskify_task，新增列）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `start_at` | TIMESTAMPTZ | 可空 | 开始时间（具体时刻，或全天时的日期@00:00） |
| `start_all_day` | BOOLEAN | 非空，默认 false | 开始时间是否为全天 |
| `due_at` | TIMESTAMPTZ | 可空 | 截止时间（具体时刻，或全天时的日期@23:59:59） |
| `due_all_day` | BOOLEAN | 非空，默认 false | 截止时间是否为全天 |

**校验规则**：

- 同一字段的「全天」与「具体时刻」不可混用（由 `*_all_day` 标记控制）。
- 开始与截止均设置时，截止须不早于开始；全天按日期比较。
- 「被阻塞」（不可开始）为读时计算：任务存在至少一个 `blocker` 且其 `status != DONE` 时为真，不落列。

## 提醒节点（taskify_reminder，新增）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 提醒唯一标识 |
| `task_id` | UUID v7 | 非空，外键 → `taskify_task.id` | 所属任务 |
| `mode` | VARCHAR(16) | 非空 | `ABSOLUTE` / `RELATIVE` |
| `trigger_at` | TIMESTAMPTZ | 可空（ABSOLUTE 时非空） | 绝对触发时间 |
| `offset_minutes` | INT | 可空（RELATIVE 时非空，>0） | 相对「提前」分钟数 |
| `anchor` | VARCHAR(8) | 可空（RELATIVE 时非空） | `START` / `DUE` |
| `fired` | BOOLEAN | 非空，默认 false | 是否已触发（防重复） |
| `created_at` | TIMESTAMPTZ | 非空 | 创建时间 |

**校验规则**：

- `ABSOLUTE`：`trigger_at` 必填，`offset_minutes`/`anchor` 为空。
- `RELATIVE`：`offset_minutes > 0` 且 `anchor` 必填，`trigger_at` 为空。
- 相对提醒解析：`anchor=START` → `start_at - offset_minutes`；`anchor=DUE` → `due_at - offset_minutes`。锚定时间字段未设置时无法解析、不触发（配置界面提示）。

## 阻塞关系（taskify_task_dependency，新增）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 关系唯一标识 |
| `blocker_id` | UUID v7 | 非空，外键 → `taskify_task.id` | 被依赖任务（阻塞者） |
| `dependent_id` | UUID v7 | 非空，外键 → `taskify_task.id` | 依赖任务（被阻塞者） |
| `created_at` | TIMESTAMPTZ | 非空 | 创建时间 |

**约束**：

- `UNIQUE(blocker_id, dependent_id)`；`blocker_id != dependent_id`。
- 两任务须同项目；应用层禁止依赖环（沿 blocker 方向 DFS 检查）。

## 通知（taskify_notification，扩展类型）

在 002 既有 `TASK_CREATED`/`TASK_UPDATED`/`KANBAN_MOVED` 之外新增：

| `type` | 触发事件 | 接收者 |
|--------|---------|--------|
| `REMINDER` | 提醒节点到达 | 任务负责人（非空时） |
| `TASK_BLOCKED` | 任务被阻塞 | 任务负责人（非空时） |
| `TASK_UNBLOCKED` | 任务解除阻塞 | 任务负责人（非空时） |

负责人为空时上述类型不产生通知（与既有规则一致）。

## 关系（新增）

- `taskify_task` 1—N `taskify_reminder`（`task_id`）。
- `taskify_task` 1—N `taskify_task_dependency`（作为 `blocker_id` 与 `dependent_id` 两个方向）。

## 状态机扩展（硬阻塞）

- 看板四列状态机不变（`TODO ↔ IN_PROGRESS ↔ IN_REVIEW ↔ DONE`，仅相邻）。
- 新增硬阻塞前置条件：目标为 `IN_PROGRESS` 且任务「被阻塞」时，`move` 被拒绝（`TASK_BLOCKED`）。
- 边界：任务已在 `IN_PROGRESS` 时新建立阻塞关系而变为被阻塞，不回退列，仅展示警示；后续再进入 `IN_PROGRESS` 的动作被阻止。

## 迁移

- `taskify-project` 用 Flyway 管理：`taskify_task` 增加 4 列（ALTER）；新建 `taskify_reminder`、`taskify_task_dependency`。
- `taskify-notification` 无表结构变更（仅 `NotificationType` 枚举扩展）。
