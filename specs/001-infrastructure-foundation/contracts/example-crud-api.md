# 接口契约：示例表 CRUD（taskify-example 服务）

示例表 `taskify_example` 的完整 CRUD 接口，作为后续所有业务接口的参考实现，演示统一响应、入参校验、页码分页、认证与资源级授权。响应/错误结构遵循 `api-conventions.md`。

## 通用

- 前缀：`/api/v1/examples`
- 全部接口需认证（`Authorization: Bearer <token>`）。
- 资源级授权：用户仅能访问 `owner_id` 为自身的记录；越权返回 `FORBIDDEN`。

## 创建

`POST /api/v1/examples`

请求体：

```json
{
  "name": "示例条目",
  "description": "可选描述"
}
```

成功响应 `data`：创建的完整记录（含 `id`、`owner_id`、时间戳）。

错误：`PARAM_ERROR`（`name` 缺失/超长）；`UNAUTHENTICATED`（无令牌）。

## 查询单个

`GET /api/v1/examples/{id}`

成功响应 `data`：完整记录。

错误：`NOT_FOUND`（不存在）；`FORBIDDEN`（非属主）。

## 分页列表

`GET /api/v1/examples?page=1&size=20`

成功响应 `data`：分页结构（`items`、`page`、`size`、`total`、`totalPages`）；仅返回当前用户为属主的记录。

## 更新

`PUT /api/v1/examples/{id}`

请求体：

```json
{
  "name": "新名称",
  "description": "新描述"
}
```

成功响应 `data`：更新后的完整记录。

错误：`NOT_FOUND`（不存在）；`FORBIDDEN`（非属主）；`PARAM_ERROR`（字段不合法）。

## 删除

`DELETE /api/v1/examples/{id}`

成功响应 `data`：`{ "deleted": true }`（或 204）。

错误：`NOT_FOUND`（不存在）；`FORBIDDEN`（非属主）。

## 示例记录结构

```json
{
  "id": "0192...",
  "name": "示例条目",
  "description": "可选描述",
  "ownerId": "0191...",
  "createdAt": "2026-10-17T10:00:00Z",
  "updatedAt": "2026-10-17T10:00:00Z"
}
```
