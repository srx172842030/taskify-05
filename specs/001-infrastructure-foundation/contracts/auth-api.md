# 接口契约：认证与授权（taskify-auth 服务）

认证接口由 `taskify-auth` 服务提供，经网关路由暴露。响应/错误结构遵循 `api-conventions.md`。

## 获取图形验证码

`POST /api/v1/auth/captcha`

请求体：无（或可选空对象）。

成功响应 `data`：

```json
{
  "captchaId": "3f2a...",
  "image": "data:image/png;base64,..."
}
```

说明：`captchaId` 在注册/登录时回传；验证码一次性、5 分钟过期。

## 注册

`POST /api/v1/auth/register`

请求体：

```json
{
  "username": "alice",
  "password": "secret123",
  "captchaId": "3f2a...",
  "captchaCode": "a8x2"
}
```

成功响应 `data`：

```json
{
  "token": "eyJ...",
  "user": { "id": "...", "username": "alice" }
}
```

错误：
- `PARAM_ERROR`：字段缺失/不合法、验证码错误或过期。
- `CONFLICT`：`username` 已存在。

## 登录

`POST /api/v1/auth/login`

请求体（同注册，含验证码）：

```json
{
  "username": "alice",
  "password": "secret123",
  "captchaId": "3f2a...",
  "captchaCode": "a8x2"
}
```

成功响应 `data`：

```json
{
  "token": "eyJ...",
  "user": { "id": "...", "username": "alice" }
}
```

错误：
- `PARAM_ERROR`：字段缺失/不合法、验证码错误或过期。
- `UNAUTHENTICATED`：用户名或密码错误（不披露具体哪个错误）。

## 登出

`POST /api/v1/auth/logout`

请求头：`Authorization: Bearer <token>`。

成功响应 `data`：

```json
{ "loggedOut": true }
```

说明：无状态登出由客户端丢弃 token 完成；第一阶段服务端无吊销列表（见 `research.md` 第 6 节）。

## 令牌约定

- JWT 载荷至少含：`sub`（用户 ID）、`username`、`iat`、`exp`。
- 短期有效（默认 24 小时）；有效期在实现期以配置确认。

## 授权约定

- 网关校验令牌，未通过返回 `UNAUTHENTICATED`。
- 已认证但无权限返回 `FORBIDDEN`（默认拒绝）。
- 资源级权限以 `taskify_example.owner_id` 演示（见 `example-crud-api.md`）。
