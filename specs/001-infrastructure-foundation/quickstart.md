# 快速验证指南：第一阶段 — 基础设施建设

本指南提供可运行的验证场景，证明本功能端到端可用。实现细节（代码结构、迁移、测试用例正文）在 `tasks.md` 与实现阶段完成，此处只给出前置条件、命令与预期结果。

## 前置条件

- Node.js（LTS）+ pnpm（前端）。
- JDK 21 + Maven（后端）。
- Docker 与 docker-compose（用于拉起 PostgreSQL、RocketMQ、Nacos、Redis）。

## 一、一键启动本地环境

```bash
cd backend
docker compose up -d          # 拉起 PostgreSQL、RocketMQ、Nacos、Redis
```

预期：四个中间件容器均健康运行；PostgreSQL、Redis 可连接。

## 二、启动后端

```bash
cd backend
mvn -q -DskipTests install    # 先构建 taskify-common 等模块
# 依次启动（或按实现期的启动脚本）：
#   taskify-gateway、taskify-auth、taskify-example 三个服务
```

预期：`taskify-gateway`、`taskify-auth`、`taskify-example` 注册到 Nacos；日志为结构化格式并带 `requestId`。

## 三、验证后端契约（对照 `contracts/`）

1. **健康检查**
   - `curl -i http://localhost:8080/health` → `200 {"status":"UP"}`。
   - `curl -i http://localhost:8080/ready` → `200 {"status":"READY"}`。
   - 停掉 PostgreSQL 后 `GET /ready` → `503 {"status":"NOT_READY"}`（`/health` 仍 `200`）。

2. **认证流程（对照 `contracts/auth-api.md`）**
   - `POST /api/v1/auth/captcha` → 返回 `captchaId` + `image`。
   - `POST /api/v1/auth/register`（带 `captchaId`/`captchaCode`）→ 返回 `token` + `user`；重复用户名 → `CONFLICT`。
   - `POST /api/v1/auth/login` → 返回 `token`；错误密码 → `UNAUTHENTICATED`。
   - `POST /api/v1/auth/logout`（带 token）→ `{"loggedOut":true}`。

3. **示例 CRUD（对照 `contracts/example-crud-api.md`）**
   - `POST /api/v1/examples`（带 token）→ 创建成功。
   - `GET /api/v1/examples?page=1&size=20` → 分页结构 `{items,page,size,total,totalPages}`。
   - `GET /api/v1/examples/{id}` / `PUT` / `DELETE` → 属主可操作；非属主 → `FORBIDDEN`。

4. **统一错误与校验**
   - 缺参数/非法入参 → `PARAM_ERROR`（ProblemDetail 结构，无堆栈）。
   - 无 token 访问受保护接口 → `UNAUTHENTICATED`。

## 四、启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

预期：打开本地地址，看到顶部导航、内容区、页脚；窄屏下为抽屉导航。

## 五、验证前端（对照规格用户故事）

1. **主题切换**：切换亮/暗/跟随系统；刷新后选择保持；跟随系统模式下改 OS 主题应用外观实时跟随且无首屏闪烁。
2. **设计画廊**：直接访问 `/design` URL，17 个组件在亮/暗两主题下均正确渲染。
3. **状态页**：访问不存在的路由 → 404；触发 500/无权限/加载中/空数据 → 各显示一致状态页。
4. **登录弹窗**：全局登录弹窗可用，注册/登录打通到后端认证接口。

## 六、测试

```bash
cd frontend && pnpm exec vitest run         # 前端单元/组件测试（Vitest + @vue/test-utils）
cd frontend && pnpm exec playwright test   # 前端 E2E
cd backend && mvn test                      # 后端单测 + Testcontainers 集成测试
cd backend && mvn verify                    # 含 CI 同款检查
```

预期：全部测试通过；后端集成测试使用 Testcontainers 拉起真实 PostgreSQL。

## 七、微服务一键启动（容器化）

中间件与应用分两套 compose 管理：

```bash
cd backend
docker compose up -d                                # 1) 先拉中间件（PostgreSQL、RocketMQ、Nacos、Redis）
mvn clean package -Dmaven.test.skip=true            # 2) 本地打包（跳过测试）
docker compose -f docker-compose.app.yml up -d --build   # 3) 打镜像并拉起微服务
```

预期：`taskify-gateway`、`taskify-auth`、`taskify-example` 三个容器启动并注册到 Nacos，经网关可访问健康检查与业务接口。

## 验收对照（成功标准）

| 成功标准 | 验证方式 |
|---------|---------|
| SC-001 组件可组装新页面 | 用现有组件拼一个页面，无自定义样式 |
| SC-002 主题持久化 | 切换后刷新仍保持 |
| SC-003 统一响应 100% 覆盖 | 上述契约验证全部通过 |
| SC-004 非法请求一致报错 | 触发各类错误均无堆栈/崩溃 |
| SC-005 注册登录 <3s | 手工/脚本计时 |
| SC-006 就绪反映依赖状态 | 停 PostgreSQL 验证 `/ready` |
| SC-007 一键启动 <5min | 全新检出执行 `docker compose up -d` 计时 |
| SC-008 画廊两主题正确 | 访问 `/design` 目视检查 |
