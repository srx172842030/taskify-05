# 数据模型：第一阶段 — 基础设施建设

本文档描述第一阶段涉及的数据实体、字段、校验规则与关系。表统一前缀 `taskify_`，主键统一 UUID v7（应用生成）。会话为无状态 JWT，无持久化会话表。

## 实体总览

| 实体 | 表名 | 说明 |
|------|------|------|
| 用户 | `taskify_user` | 注册/登录的账号主体 |
| 示例记录 | `taskify_example` | 演示完整 CRUD 的参考实体 |

## 用户（taskify_user）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 用户唯一标识 |
| `username` | VARCHAR(64) | 唯一，非空 | 登录账号名，注册时选定 |
| `password_hash` | VARCHAR(255) | 非空 | 密码哈希（非明文，bcrypt/Argon2 等） |
| `created_at` | TIMESTAMPTZ | 非空，默认 now() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | 非空，默认 now() | 更新时间 |

**校验规则**（注册时）：
- `username`：必填，长度 3–64，唯一，仅允许字母/数字/下划线/连字符。
- `password`：必填，长度 8–128，至少含字母与数字。

**关系**：一个用户拥有零或多个示例记录（`taskify_example.owner_id → taskify_user.id`）。

## 示例记录（taskify_example）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID v7 | 主键，应用生成 | 记录唯一标识 |
| `name` | VARCHAR(128) | 非空 | 名称 |
| `description` | TEXT | 可空 | 描述 |
| `owner_id` | UUID v7 | 非空，外键 → `taskify_user.id` | 属主（用于演示资源级授权） |
| `created_at` | TIMESTAMPTZ | 非空，默认 now() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | 非空，默认 now() | 更新时间 |

**校验规则**：
- `name`：必填，长度 1–128。
- `description`：可空，长度 ≤ 2000。

**资源级授权**：`owner_id` 用于演示「资源级权限检查」——默认拒绝，用户只能访问自己拥有的记录（读、改、删需校验属主身份），见 `contracts/example-crud-api.md`。

## 迁移

- 使用 Flyway 管理表结构版本；迁移脚本按模块组织（如 `taskify-auth` 模块管理 `taskify_user`，`taskify-example` 模块管理 `taskify_example`）。
- 全部建表语句使用 `taskify_` 前缀。

## 非持久化数据

- **验证码**：`captchaId → 答案` 的短期映射（TTL 5 分钟、一次性），存储于 Redis（见 `research.md` 第 7、14 节）。
- **会话令牌**：无状态 JWT，不落库。
