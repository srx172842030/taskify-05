# 接口契约：统一约定

本文档定义所有后端接口共同遵守的响应结构、错误码、分页与健康检查约定。业务接口只需遵循本约定，无需自行实现。

## 基础路径与版本

- 业务接口统一前缀：`/api/v1`。
- 健康检查：`/health`、`/ready`（不含版本前缀）。

## 成功响应

所有成功响应使用统一封装：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": { },
  "requestId": "0f1c2d3e-...",
  "timestamp": "2026-10-17T10:00:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 稳定错误码，成功恒为 `SUCCESS` |
| `message` | string | 人类可读信息（经 MessageSource 国际化，默认中文） |
| `data` | object/array | 业务数据；列表接口见下方分页结构 |
| `requestId` | string | 请求 ID，用于关联日志 |
| `timestamp` | string | ISO-8601 时间戳 |

## 错误响应（ProblemDetail，RFC 9457）

所有错误响应采用 `ProblemDetail` 结构，并追加自定义字段：

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "参数校验失败：username 不能为空",
  "code": "PARAM_ERROR",
  "requestId": "0f1c2d3e-...",
  "timestamp": "2026-10-17T10:00:00Z"
}
```

| 字段 | 说明 |
|------|------|
| `type` | 错误类型 URI（可指向错误码文档） |
| `title` | HTTP 状态短描述 |
| `status` | HTTP 状态码 |
| `detail` | 具体错误说明（经国际化） |
| `code` | 稳定业务错误码（见下表） |
| `requestId` | 请求 ID |
| `timestamp` | 时间戳 |

## 错误码枚举

| `code` | 含义 | HTTP | 触发场景 |
|--------|------|------|---------|
| `SUCCESS` | 成功 | 200 | 正常返回 |
| `PARAM_ERROR` | 参数错误 | 400 | 入参校验失败、验证码错误 |
| `UNAUTHENTICATED` | 未认证 | 401 | 缺失或无效令牌 |
| `FORBIDDEN` | 无权限 | 403 | 已认证但无权限（默认拒绝） |
| `NOT_FOUND` | 不存在 | 404 | 资源不存在 |
| `CONFLICT` | 冲突 | 409 | 用户名已存在等 |
| `RATE_LIMITED` | 限流 | 429 | 触发限流 |
| `SERVER_ERROR` | 服务器错误 | 500 | 未预期异常（不泄露堆栈） |

## 分页约定

请求参数（列表接口）：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | int | 1 | 页码，从 1 开始 |
| `size` | int | 20 | 每页条数，上限 100 |

列表响应 `data` 结构：

```json
{
  "items": [ ],
  "page": 1,
  "size": 20,
  "total": 123,
  "totalPages": 7
}
```

## 认证与授权约定

- 客户端在 `Authorization: Bearer <token>` 头携带 JWT。
- 网关统一校验令牌并透传身份（如用户 ID）到下游服务。
- 授权默认拒绝：未显式放行的路由/资源一律拒绝。
- 资源级授权：对 `taskify_example` 等资源，校验当前用户是否为属主或具备相应权限。

## 健康检查

| 端点 | 含义 | 成功响应 |
|------|------|---------|
| `GET /health` | 存活（进程可响应） | `200` `{"status":"UP"}` |
| `GET /ready` | 就绪（依赖可达，如 PostgreSQL） | `200` `{"status":"READY"}`；依赖不可用 `503` `{"status":"NOT_READY"}` |

## 通用约束

- 所有响应带 `requestId`，与结构化日志中的请求 ID 一致。
- 所有响应带安全响应头；跨域仅允许 CORS 白名单来源。
