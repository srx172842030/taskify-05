# 数据模型：Taskify 核心协作功能

本文档描述 002 功能涉及的数据实体、字段、校验规则与关系。沿用 001 约定：表前缀 `taskify_`、主键 UUID v7（应用生成）、Flyway 迁移、时间戳 `TIMESTAMPTZ`。用户表 `taskify_user` 已由 001 建立（本功能复用）。

## 实体总览

| 实体 | 表名 | 所属服务 | 说明 |
|------|------|---------|------|
| 项目 | `taskify_project` | taskify-project | 协作顶层容器 |
| 任务 | `taskify_task` | taskify-project | 项目内工作单元，含看板状态 |
| 评论 | `taskify_comment` | taskify-project | 附属于任务的讨论条目 |
| 通知 | `taskify_notification` | taskify-notification | 实时通知（持久化 + SSE 推送） |

看板四列不建独立表，建模为任务的状态枚举（见下）。

## 项目（taskify_project）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 项目唯一标识 |
| `name` | VARCHAR(128) | 非空 | 项目名称 |
| `description` | TEXT | 可空 | 项目描述 |
| `created_by` | UUID v7 | 非空，外键 → `taskify_user.id` | 创建者 |
| `created_at` | TIMESTAMPTZ | 非空，默认 now() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | 非空，默认 now() | 更新时间 |

**校验规则**：`name` 必填、去空白后长度 1–128；`description` 可空、长度 ≤ 2000。

**访问模型**：阶段一无成员关系表——所有预置用户可访问全部项目；`created_by` 用于记录创建者（通知「看板变更」时使用）并预留未来成员关系扩展。

## 任务（taskify_task）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 任务唯一标识 |
| `project_id` | UUID v7 | 非空，外键 → `taskify_project.id` | 所属项目 |
| `title` | VARCHAR(200) | 非空 | 任务标题 |
| `description` | TEXT | 可空 | 任务描述 |
| `assignee_id` | UUID v7 | 可空，外键 → `taskify_user.id` | 负责人（可留空，见规格澄清 Q3） |
| `status` | VARCHAR(32) | 非空，默认 `TODO` | 看板列（枚举，见下） |
| `created_by` | UUID v7 | 非空，外键 → `taskify_user.id` | 创建者 |
| `created_at` | TIMESTAMPTZ | 非空，默认 now() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | 非空，默认 now() | 更新时间 |

**校验规则**：`title` 必填、去空白后长度 1–200；`description` 可空、长度 ≤ 5000；`assignee_id` 若填必须是存在的用户；`status` 必须为四值之一。

**状态枚举与状态机**（看板列）：

| `status` | 中文列名 |
|----------|---------|
| `TODO` | 待办事项 |
| `IN_PROGRESS` | 进行中 |
| `IN_REVIEW` | 审查中 |
| `DONE` | 已完成 |

**状态转移（仅限相邻列，规格 FR-011）**：`TODO ↔ IN_PROGRESS ↔ IN_REVIEW ↔ DONE`。跨列（非相邻）转移被拒绝（`PARAM_ERROR`）。任务创建时默认 `TODO`。

## 评论（taskify_comment）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 评论唯一标识 |
| `task_id` | UUID v7 | 非空，外键 → `taskify_task.id` | 所属任务 |
| `author_id` | UUID v7 | 非空，外键 → `taskify_user.id` | 评论作者 |
| `content` | TEXT | 非空 | 评论内容 |
| `created_at` | TIMESTAMPTZ | 非空，默认 now() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | 非空，默认 now() | 更新时间（编辑后更新） |

**校验规则**：`content` 必填、去空白后非空、长度 ≤ 5000。

**权限**：仅作者可编辑/删除自己的评论（`PATCH/DELETE /comments/{id}` 校验 `author_id == 当前用户`，否则 `FORBIDDEN`）。

## 通知（taskify_notification）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 通知唯一标识 |
| `recipient_id` | UUID v7 | 非空，外键 → `taskify_user.id` | 接收者 |
| `type` | VARCHAR(32) | 非空 | 通知类型（枚举，见下） |
| `task_id` | UUID v7 | 可空，外键 → `taskify_task.id` | 关联任务 |
| `message` | VARCHAR(500) | 非空 | 通知内容（中文文案） |
| `read` | BOOLEAN | 非空，默认 false | 是否已读 |
| `created_at` | TIMESTAMPTZ | 非空，默认 now() | 创建时间 |

**通知类型枚举**：

| `type` | 触发事件 | 接收者 |
|--------|---------|--------|
| `TASK_CREATED` | 新任务发布 | 任务负责人（非空时） |
| `TASK_UPDATED` | 任务被修改 | 任务负责人（非空时） |
| `KANBAN_MOVED` | 任务看板列变更 | 任务负责人（非空时） + 创建者 |

**去重**：同一事件若负责人 = 创建者，仅一条通知。负责人为空时，`TASK_CREATED`/`TASK_UPDATED` 不产生通知（`KANBAN_MOVED` 仍通知创建者）。

## 关系

- `taskify_project` 1—N `taskify_task`（`project_id`）
- `taskify_task` 1—N `taskify_comment`（`task_id`）
- `taskify_task` 1—N `taskify_notification`（`task_id`，可空）
- `taskify_user` 1—N `taskify_project`（`created_by`）
- `taskify_user` 1—N `taskify_task`（`assignee_id`、`created_by`）
- `taskify_user` 1—N `taskify_comment`（`author_id`）
- `taskify_user` 1—N `taskify_notification`（`recipient_id`）

## 迁移

- 每个服务用 Flyway 管理自己的表：`taskify-project` 管理 `taskify_project`/`taskify_task`/`taskify_comment`，`taskify-notification` 管理 `taskify_notification`。
- 全部建表语句使用 `taskify_` 前缀；预置 5 用户由 `taskify-auth` 迁移提供（3 个示例项目 + 种子数据由 `taskify-project` 迁移/种子提供）。

## 非持久化数据

- **SSE 连接**：通知服务的在线连接注册表（`userId → 连接` 映射），内存态，不落库。
- **未读计数**：可由 `taskify_notification` 实时聚合查询；如需优化，可选缓存于 Redis（键按 `recipient_id`，见 research 第 8 节备注），阶段一可先直接聚合查询。
