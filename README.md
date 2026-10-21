# Taskify

第一阶段：基础设施建设。为后续业务功能提供统一的前端界面基础与后端底座。

## 结构

- `frontend/` — Vue 3 + TypeScript（strict）+ Tailwind CSS + shadcn-vue
- `backend/` — Java 21 + Spring Boot 3.5.x 多模块微服务（`taskify-common`、`taskify-gateway`、`taskify-auth`、`taskify-example`）
- `.github/workflows/` — CI（根目录）

## 本地启动

```bash
# 中间件（PostgreSQL、RocketMQ、Nacos）
cd backend && docker compose up -d

# 后端（gateway/auth/example）
cd backend && mvn -q -DskipTests install

# 前端
cd frontend && pnpm install && pnpm dev
```

详细验证步骤见 `specs/001-infrastructure-foundation/quickstart.md`。
