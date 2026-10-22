# 接口契约：项目/任务/评论/看板（taskify-project 服务）

由 `taskify-project` 服务提供，经网关路由暴露，响应/错误结构遵循 `../001-infrastructure-foundation/contracts/api-conventions.md`（统一 `{code,message,data,requestId,timestamp}`、ProblemDetail 错误、`page`/`size` 分页）。所有接口需登录（`Authorization: Bearer <token>`），未认证返回 `UNAUTHENTICATED`。

## 项目

### 创建项目

`POST /api/v1/projects`

请求体：

```json
{ "name": "官网改版", "description": "季度改版计划" }
```

成功响应 `data`：

```json
{ "id": "0f1c...", "name": "官网改版", "description": "季度改版计划", "createdBy": "3f2a...", "createdAt": "2026-10-21T10:00:00Z" }
```

错误：`PARAM_ERROR`（`name` 为空/超长）、`UNAUTHENTICATED`。

### 查询项目列表

`GET /api/v1/projects?page=1&size=20`

成功响应 `data`：分页结构，条目同上（项目对象数组）。

### 查询项目详情

`GET /api/v1/projects/{projectId}`

成功响应 `data`：项目对象（含 `name`、`description`、`createdBy`）。`NOT_FOUND`：项目不存在。

## 任务

### 创建任务

`POST /api/v1/projects/{projectId}/tasks`

请求体（`assigneeId` 可空）：

```json
{ "title": "实现登录页", "description": "…", "assigneeId": "3f2a..." }
```

成功响应 `data`：

```json
{ "id": "…", "projectId": "…", "title": "实现登录页", "description": "…", "assigneeId": "3f2a...", "status": "TODO", "createdBy": "…", "createdAt": "…", "updatedAt": "…" }
```

规则：新任务 `status` 默认 `TODO`。错误：`PARAM_ERROR`（标题为空/超长、`assigneeId` 非存在用户）、`NOT_FOUND`（项目不存在）。

### 查询项目任务列表（看板/列表视图数据源）

`GET /api/v1/projects/{projectId}/tasks?status=TODO&page=1&size=100`

- 可选 `status` 过滤（`TODO`/`IN_PROGRESS`/`IN_REVIEW`/`DONE`）；省略则返回全部任务。
- 成功响应 `data`：分页结构，任务对象数组。

### 查询任务详情

`GET /api/v1/tasks/{taskId}`

成功响应 `data`：任务对象。`NOT_FOUND`：任务不存在。

### 修改任务

`PATCH /api/v1/tasks/{taskId}`

请求体（均可选，至少一项；`assigneeId` 可置空）：

```json
{ "title": "新标题", "description": "…", "assigneeId": null }
```

规则：修改触发 `TASK_UPDATED` 事件。错误：`PARAM_ERROR`、`NOT_FOUND`。

### 移动任务（看板列变更）

`POST /api/v1/tasks/{taskId}/move`

请求体：

```json
{ "toStatus": "IN_PROGRESS" }
```

规则（规格 FR-011，仅限相邻列）：

- 合法转移：`TODO ↔ IN_PROGRESS ↔ IN_REVIEW ↔ DONE`。
- `toStatus` 必须与当前 `status` 相邻，否则 `PARAM_ERROR`。
- 成功响应 `data`：更新后的任务对象（含新 `status`）。
- 移动触发 `KANBAN_MOVED` 事件。

## 评论

### 发表评论

`POST /api/v1/tasks/{taskId}/comments`

请求体：

```json
{ "content": "这个方案可行。" }
```

成功响应 `data`：

```json
{ "id": "…", "taskId": "…", "authorId": "…", "content": "这个方案可行。", "createdAt": "…", "updatedAt": "…" }
```

错误：`PARAM_ERROR`（内容为空/超长）、`NOT_FOUND`（任务不存在）。

### 查询评论列表

`GET /api/v1/tasks/{taskId}/comments?page=1&size=50`

成功响应 `data`：分页结构，评论对象数组，按 `createdAt` 升序。

### 编辑评论（仅作者）

`PATCH /api/v1/comments/{commentId}`

请求体：

```json
{ "content": "修改后的内容" }
```

规则：仅 `author_id == 当前用户`，否则 `FORBIDDEN`。成功响应 `data`：更新后的评论。

### 删除评论（仅作者）

`DELETE /api/v1/comments/{commentId}`

规则：仅 `author_id == 当前用户`，否则 `FORBIDDEN`。成功响应 `data`：`{ "deleted": true }`。

## 状态枚举（看板列）

| 值 | 中文 |
|----|------|
| `TODO` | 待办事项 |
| `IN_PROGRESS` | 进行中 |
| `IN_REVIEW` | 审查中 |
| `DONE` | 已完成 |
