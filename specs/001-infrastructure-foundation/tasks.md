---
description: "第一阶段：基础设施建设 任务清单"
---

# 任务清单：第一阶段 — 基础设施建设

**输入**：设计文档来自 `/specs/001-infrastructure-foundation/`

**前置文档**：plan.md（技术栈/结构）、spec.md（用户故事）、data-model.md（实体）、contracts/（接口契约）、research.md（决策）、quickstart.md（验证场景）

**测试**：本项目章程原则 II 规定测试不可协商，故各阶段均包含对应测试任务（后端 JUnit 5 + Testcontainers；前端 Vitest + @vue/test-utils + Playwright）。

**组织方式**：任务按用户故事分组，使每个故事可独立实现、独立测试、作为增量交付。

## 格式说明：`[ID] [P?] [Story] 描述`

- **`- [ ]`**：复选框（必填）
- **`T00x`**：顺序任务 ID
- **`[P]`**：可并行（不同文件、无依赖）
- **`[USx]`**：所属用户故事（仅用户故事阶段标注）
- 描述含精确文件路径

## 路径约定

- **前端**：`frontend/src/`、`frontend/tests/unit/`、`frontend/e2e/`
- **后端**：`backend/<模块>/src/main/java/com/taskify/<模块>/`、`backend/<模块>/src/test/`
- 模块：`taskify-common`、`taskify-gateway`、`taskify-auth`、`taskify-example`

---

## Phase 1: Setup（共享基础设施）

**目的**：项目初始化与基础结构

- [X] T001 创建根目录结构：`frontend/`、`backend/`（含 `taskify-common`、`taskify-gateway`、`taskify-auth`、`taskify-example`）、`.github/workflows/`
- [X] T002 初始化前端项目（Vite + Vue 3 + TypeScript strict + pnpm）：`frontend/package.json`、`frontend/tsconfig.json`、`frontend/vite.config.ts`、`frontend/src/main.ts`
- [X] T003 初始化后端 Maven 父 POM 与四模块骨架：`backend/pom.xml` 及各模块 `pom.xml`（统一 Spring Boot 3.5.x、Java 21、依赖版本管理）
- [X] T004 [P] 配置前端 ESLint + Prettier：`frontend/eslint.config.js`、`frontend/.prettierrc.json`、`frontend/.prettierignore`
- [X] T005 [P] 安装并配置前端依赖（Tailwind CSS、shadcn-vue、@vueuse/core、vue-i18n、vee-validate + zod、lucide-vue-next、unplugin-fonts、Vitest、@vue/test-utils、Playwright）：`frontend/tailwind.config.*`、`frontend/vitest.config.*`、`frontend/playwright.config.*`
- [X] T006 [P] 配置后端测试依赖 JUnit 5 + Testcontainers：`backend/taskify-common/pom.xml` 及各服务模块 `pom.xml`
- [X] T007 [P] 配置仓库根目录 GitHub Actions CI：`.github/workflows/ci.yml`（后端矩阵构建各模块 + 前端 build/test）
- [X] T008 [P] 编写 `backend/docker-compose.yml` 一键拉起 PostgreSQL、RocketMQ、Nacos、Redis

---

## Phase 2: Foundational（阻塞性前置）

**目的**：所有用户故事开始前必须完成的核心基础设施

**⚠️ 关键**：本阶段完成前，任何用户故事都不得开始

- [X] T009 [P] 实现前端设计 token：`frontend/src/styles/tokens.css`（亮/暗两套语义变量，按 `contracts/design-tokens.md`）
- [X] T010 [P] 初始化 vue-i18n 与中文文案：`frontend/src/i18n/index.ts`、`frontend/src/i18n/messages/zh-CN.json`
- [X] T011 [P] 建立前端路由骨架：`frontend/src/router/index.ts`
- [X] T012 [P] 实现 taskify-common 基础实体与主键生成：`backend/taskify-common/src/main/java/com/taskify/common/base/BaseEntity.java`（id UUID v7、created_at、updated_at）、UUID v7 生成器、Mybatis-Plus 公共配置（`.../mybatis/`）
- [X] T013 [P] 配置数据库连接与 Flyway 迁移框架：`backend/taskify-auth` 与 `backend/taskify-example` 的数据源配置与 Flyway 集成

**检查点**：基础就绪，用户故事可开始实现

---

## Phase 3: 用户故事 1 — 统一应用外壳与主题（优先级：P1）🎯 MVP

**目标**：全局布局（顶部导航、内容区、页脚、抽屉导航、登录弹窗）+ 亮/暗/跟随系统三主题切换与持久化

**独立测试**：启动前端，桌面/手机宽度下外壳正常渲染；切换三种主题并刷新保持；跟随系统随 OS 主题实时变化且无首屏闪烁

### 实现

- [X] T014 [US1] 实现全局布局组件（顶部导航、内容区、页脚）：`frontend/src/layouts/AppLayout.vue`
- [X] T015 [US1] 实现移动端抽屉导航：`frontend/src/layouts/AppDrawer.vue`（与 AppLayout 集成）
- [X] T016 [P] [US1] 实现登录弹窗外壳组件：`frontend/src/components/auth/LoginDialog.vue`（先占位 UI，逻辑在 US2 接线）
- [X] T017 [P] [US1] 实现主题切换 composable：`frontend/src/composables/useTheme.ts`（@vueuse/core 的 useColorMode，class 策略，默认跟随系统）
- [X] T018 [US1] 主题持久化 + 首屏防闪烁内联脚本：`frontend/index.html`（内联脚本在 CSS/JS 加载前设置根元素 class）+ `useTheme` 持久化
- [X] T019 [US1] 将设计 token 接入主题切换（亮/暗变量随 class 切换，组件引用 `var(--token)`）
- [X] T020 [US1] 外壳与主题测试：`frontend/tests/unit/useTheme.spec.ts`（Vitest）+ `frontend/e2e/theme.spec.ts`（Playwright：三主题切换 + 持久化 + 无闪烁）

**检查点**：此时用户故事 1 应可独立运行与验证

---

## Phase 4: 用户故事 3 — 统一后端契约（优先级：P1）

**目标**：开发者只写 service + route；响应格式化、错误处理、入参校验、页码分页、默认拒绝授权均由共享底座提供

**独立测试**：编写 taskify-common 契约单测与网关默认拒绝集成测试，验证统一结构、参数错误、未认证/无权限拒绝、页码分页

### 实现

- [X] T021 [P] [US3] 实现统一成功响应封装：`backend/taskify-common/src/main/java/com/taskify/common/response/ApiResponse.java`（code/message/data/requestId/timestamp）
- [X] T022 [P] [US3] 实现错误码枚举与 ProblemDetail：`backend/taskify-common/src/main/java/com/taskify/common/error/ErrorCode.java`（PARAM_ERROR/UNAUTHENTICATED/FORBIDDEN/NOT_FOUND/CONFLICT/RATE_LIMITED/SERVER_ERROR + HTTP 映射）、`.../error/ProblemDetailBuilder.java`（按 `contracts/api-conventions.md`）
- [X] T023 [US3] 实现全局异常处理：`backend/taskify-common/src/main/java/com/taskify/common/error/GlobalExceptionHandler.java`（@RestControllerAdvice，业务异常→ProblemDetail，不泄露堆栈）
- [X] T024 [US3] 实现入参校验统一封装：`backend/taskify-common/src/main/java/com/taskify/common/validation/`（Spring Validation 绑定统一 PARAM_ERROR 输出）
- [X] T025 [P] [US3] 实现分页约定封装：`backend/taskify-common/src/main/java/com/taskify/common/response/PageResult.java`（items/page/size/total/totalPages）
- [X] T026 [US3] 实现网关路由骨架与默认拒绝授权过滤：`backend/taskify-gateway/src/main/java/com/taskify/gateway/`（路由转发 + deny-by-default 认证/授权过滤器）
- [X] T027 [US3] taskify-common 契约单元测试：`backend/taskify-common/src/test/java/com/taskify/common/`（ApiResponse/ErrorCode/GlobalExceptionHandler/校验/分页）
- [X] T028 [US3] 网关路由与默认拒绝授权集成测试：`backend/taskify-gateway/src/test/java/com/taskify/gateway/`

**检查点**：统一后端契约可独立验证

---

## Phase 5: 用户故事 2 — 账号注册、登录与登出（优先级：P1）

**目标**：账号 + 密码 + 图形验证码注册/登录、无状态会话（JWT）、登出；前端登录弹窗接线

**独立测试**：注册→登录→登出端到端跑通；重复用户名冲突；错误密码不泄露细节；无 token 被拒

**依赖**：US3（统一契约）已完成；Phase 2（DB/Flyway）已完成

### 实现

- [X] T029 [US2] 编写 `taskify_user` 表迁移脚本：`backend/taskify-auth/src/main/resources/db/migration/V1__create_user.sql`（username 唯一非空 VARCHAR(64)、password_hash 非空、created_at/updated_at，按 `data-model.md`）
- [X] T030 [US2] 创建 User 实体与 Mapper：`backend/taskify-auth/src/main/java/com/taskify/auth/entity/User.java`、`.../mapper/UserMapper.java`
- [X] T031 [P] [US2] 实现图形验证码（生成 captchaId + base64 图片、答案短期存储 TTL 5 分钟一次性、校验）：`backend/taskify-auth/src/main/java/com/taskify/auth/captcha/`
- [X] T032 [US2] 实现认证 service（注册/登录/登出、密码哈希、JWT 签发）：`backend/taskify-auth/src/main/java/com/taskify/auth/service/AuthService.java`
- [X] T033 [US2] 实现认证 controller 与 DTO：`backend/taskify-auth/src/main/java/com/taskify/auth/controller/AuthController.java`、`.../dto/`（按 `contracts/auth-api.md`）
- [X] T034 [US2] 实现 JWT 工具与网关认证过滤器：`backend/taskify-gateway/src/main/java/com/taskify/gateway/security/`（校验 Bearer token、透传用户身份）
- [X] T035 [P] [US2] 前端认证 store 与 API 调用：`frontend/src/stores/auth.ts`、`frontend/src/services/auth.ts`（对接 `/api/v1/auth/*`）
- [X] T036 [US2] 登录弹窗接线到认证流程：`frontend/src/components/auth/LoginDialog.vue`（vee-validate + zod 校验 + 验证码展示）
- [X] T037 [US2] 认证后端集成测试：`backend/taskify-auth/src/test/`（Testcontainers + PostgreSQL：注册/登录/登出/重复用户名/错误密码；断言注册与登录耗时 <3s）
- [X] T038 [US2] 认证 E2E：`frontend/e2e/auth.spec.ts`（注册→登录→登出流程；断言注册与登录端到端耗时 <3s）

**检查点**：用户故事 1、3、2 均可独立工作，构成完整 P1 基础

---

## Phase 6: 用户故事 4 — 共享组件库与设计画廊（优先级：P2）

**目标**：17 个基础组件（按钮、输入框、文本域、选择器、复选框、开关、表单含校验提示、对话框、抽屉、下拉菜单、toast、表格、标签页、卡片、徽标、骨架屏、分页）+ `/design` 画廊（无独立导航入口，仅 URL 访问）

**独立测试**：直接访问 `/design` URL 确认每个组件在亮/暗两主题下正确渲染；用现有组件拼一个页面无需自定义样式

**依赖**：Phase 2（token/i18n）；US1（外壳）

### 实现

- [X] T039 [US4] 通过 shadcn-vue CLI 添加基础组件到 `frontend/src/components/ui/`（button/input/textarea/select/checkbox/switch/dialog/drawer/dropdown-menu/table/tabs/card/badge/skeleton/pagination/toast，源码只读不直接修改）
- [X] T040 [US4] 建立业务封装组件：`frontend/src/components/`（将 shadcn-vue 组件绑定到设计 token 与 i18n 文案，形成默认样式/文案）
- [X] T041 [US4] 实现表单组件与校验提示封装：`frontend/src/components/`（vee-validate + zod 集成）
- [X] T042 [US4] 实现 Toast 提示消息封装与全局挂载：`frontend/src/components/`、`frontend/src/composables/useToast.ts`
- [X] T043 [US4] 实现设计画廊页：`frontend/src/pages/design/DesignGallery.vue`（路由 `/design`，无独立导航入口，仅通过 URL 直接访问）
- [X] T044 [US4] 画廊在亮/暗两主题下展示全部组件（主题切换联动）
- [X] T045 [US4] 核心组件单测：`frontend/tests/unit/`（Vitest + @vue/test-utils）
- [X] T046 [US4] 设计画廊 E2E：`frontend/e2e/design.spec.ts`（/design 两主题渲染）

**检查点**：组件库可用、画廊可展示

---

## Phase 7: 用户故事 5 — 通用状态页（优先级：P2）

**目标**：404、500、无权限、加载中、空数据 五种一致的状态页

**独立测试**：访问不存在路由→404；触发 500/无权限/加载中/空数据→各显示一致状态页

**依赖**：Phase 2（token/i18n/router）；US1（外壳）

### 实现

- [X] T047 [US5] 实现 404 页面：`frontend/src/pages/error/NotFound.vue`
- [X] T048 [P] [US5] 实现 500 页面：`frontend/src/pages/error/ServerError.vue`（不泄露内部细节）
- [X] T049 [P] [US5] 实现无权限页面：`frontend/src/pages/error/Forbidden.vue`
- [X] T050 [P] [US5] 实现加载中组件：`frontend/src/components/state/LoadingState.vue`
- [X] T051 [P] [US5] 实现空数据组件：`frontend/src/components/state/EmptyState.vue`
- [X] T052 [US5] 接线路由级 404 兜底与错误边界：`frontend/src/router/index.ts`、`frontend/src/main.ts`
- [X] T053 [US5] 状态页 E2E：`frontend/e2e/states.spec.ts`

**检查点**：五种状态页均可触发并显示一致

---

## Phase 8: 用户故事 6 — 参考 CRUD 示例与运维就绪（优先级：P3）

**目标**：示例表 CRUD 参考实现（含资源级授权）+ 请求 ID 日志 + CORS/安全头 + 健康检查 + 一键本地启动验证

**独立测试**：示例表 CRUD 端到端跑通；/health 存活、/ready 依赖不可用时不就绪；日志带 requestId；响应带安全头；一键启动本地环境

**依赖**：US3（契约）、US2（认证，用于资源级授权演示）、Phase 2（DB/Flyway）

### 实现

- [X] T054 [US6] 编写 `taskify_example` 表迁移脚本：`backend/taskify-example/src/main/resources/db/migration/V1__create_example.sql`（name 非空 VARCHAR(128)、description 可空 TEXT、owner_id 非空外键，按 `data-model.md`）
- [X] T055 [US6] 创建 Example 实体与 Mapper：`backend/taskify-example/src/main/java/com/taskify/example/entity/Example.java`、`.../mapper/ExampleMapper.java`
- [X] T056 [US6] 实现示例 CRUD service（含资源级授权：仅属主可读/改/删，默认拒绝）：`backend/taskify-example/src/main/java/com/taskify/example/service/ExampleService.java`
- [X] T057 [US6] 实现示例 CRUD controller 与 DTO：`backend/taskify-example/src/main/java/com/taskify/example/controller/ExampleController.java`、`.../dto/`（按 `contracts/example-crud-api.md`）
- [X] T058 [US6] 实现请求 ID 过滤器与结构化日志：`backend/taskify-common/src/main/java/com/taskify/common/web/`（RequestIdFilter + 结构化日志关联 requestId）
- [X] T059 [US6] 实现 CORS 白名单与安全响应头：`backend/taskify-gateway/src/main/java/com/taskify/gateway/`（CORS 白名单配置 + 安全响应头过滤器）
- [X] T060 [US6] 实现健康检查端点：`/health`（存活）与 `/ready`（依赖就绪，检查 PostgreSQL）——网关与各服务
- [X] T061 [US6] 示例 CRUD 集成测试：`backend/taskify-example/src/test/`（Testcontainers + PostgreSQL + 资源级授权用例：非属主→FORBIDDEN）
- [X] T062 [US6] 按 `quickstart.md` 执行端到端验证（一键启动 + 契约验证 + 前端验证）

**检查点**：所有用户故事独立可用，运维就绪

---

## Phase 9: Polish & 跨切面收尾

**目的**：影响多个故事的一致性与收尾

- [X] T063 [P] 更新 `frontend/docs/design-tokens.md`（token 清单与用法，与 `contracts/design-tokens.md` 一致）
- [X] T064 [P] 更新 README 与文档（中文，按 `quickstart.md` 与章程原则 VI）
- [X] T065 代码清理：移除占位/临时项，统一命名与格式
- [X] T066 全量测试回归：`frontend`（vitest run + playwright test）、`backend`（mvn verify，含矩阵构建）

---

## 依赖与执行顺序

### 阶段依赖

- **Setup（Phase 1）**：无依赖，可立即开始
- **Foundational（Phase 2）**：依赖 Setup 完成——**阻塞所有用户故事**
- **用户故事（Phase 3–8）**：依赖 Foundational 完成
- **Polish（Phase 9）**：依赖所有目标故事完成

### 用户故事依赖

- **US1（外壳/主题）**：Foundational 后可开始，不依赖其他故事（前端独立）
- **US3（统一契约）**：Foundational 后可开始，不依赖其他故事（后端契约）
- **US2（认证）**：依赖 US3（统一契约）与 Foundational（DB）
- **US4（组件库）**：依赖 Foundational（token/i18n）+ US1（外壳）
- **US5（状态页）**：依赖 Foundational（token/i18n/router）+ US1（外壳）
- **US6（CRUD/运维）**：依赖 US3（契约）+ US2（认证）+ Foundational（DB）

### 故事内顺序

- 迁移/实体 → service → controller/端点 → 集成
- 后端：契约（US3）先于业务（US2/US6）
- 前端：外壳（US1）先于组件库（US4）/状态页（US5）

### 并行机会

- Setup 中 T004–T008 可并行；Foundational 中 T009–T013 可并行
- Foundational 完成后：US1 与 US3 可并行（前端/后端互不依赖）
- US3 完成后：US2 可启动；同时 US1 完成后 US4/US5 可并行
- 同一故事内标记 [P] 的任务可并行（不同文件）

---

## 并行示例：用户故事 5（状态页）

```text
# 并行启动所有独立状态页：
Task: "实现 500 页面 frontend/src/pages/error/ServerError.vue"
Task: "实现无权限页面 frontend/src/pages/error/Forbidden.vue"
Task: "实现加载中组件 frontend/src/components/state/LoadingState.vue"
Task: "实现空数据组件 frontend/src/components/state/EmptyState.vue"
```

---

## 实施策略

### MVP 优先（用户故事 1）

1. 完成 Phase 1：Setup
2. 完成 Phase 2：Foundational（关键——阻塞所有故事）
3. 完成 Phase 3：用户故事 1（外壳 + 主题）
4. **停下验证**：独立测试用户故事 1
5. 可演示/部署

> 注：本功能为基础设施，P1 三故事（US1 + US3 + US2）共同构成「完整基础 MVP」；US1 是最小可视化增量。

### 增量交付

1. Setup + Foundational → 基础就绪
2. + US1（外壳/主题）→ 验证 → 演示（最小 MVP）
3. + US3（统一契约）→ 验证 → 后端底座就绪
4. + US2（认证）→ 验证 → 完整 P1 基础
5. + US4、US5 → 验证 → 组件库与状态页
6. + US6（CRUD/运维）→ 验证 → 参考实现 + 可部署
7. 每步不破坏前序故事

### 并行团队策略

- 团队先共同完成 Setup + Foundational
- Foundational 完成后：
  - 开发者 A：US1（前端外壳/主题）
  - 开发者 B：US3（后端统一契约）
- US3 完成后：开发者 B 转 US2（认证）；A 转 US4/US5
- 最后 US6 由熟悉契约与认证的成员完成

---

## 备注

- `[P]` = 不同文件、无依赖，可并行
- `[USx]` 将任务映射到具体用户故事，便于追踪
- 每个用户故事应可独立完成、独立测试
- 遵守章程原则：代码质量（I）、测试不可协商（II）、UX 一致（III）、性能预算（IV）、简洁（V）、文档中文（VI）
- 每完成一个任务或逻辑组后提交
- 可在任一检查点停下验证故事独立性

## Phase 10: Convergence

- [X] T067 实现 Redis + Redisson 基础设施：`backend/docker-compose.yml` 新增 redis 服务、`backend/pom.xml` 依赖管理加入 `redisson-spring-boot-starter` 并由 `taskify-common` 依赖、各服务 `application.yml` 新增 Redis 连接配置、`taskify-common` 新增 `cache/` 包（RedissonConfig） per plan:存储 + research §14 (missing)
- [X] T068 将 `CaptchaService` 由内存 `ConcurrentHashMap` 改为 Redis 存储（TTL 5 分钟、一次性消费） per research §7/§14 + data-model 非持久化数据 (missing)
- [X] T069 提供验证码测试后门（测试环境固定验证码）并补全注册→登录→登出 E2E per US2 + tasks T038 (partial)

## Phase 11: Convergence

- [X] T070 创建微服务容器化：`backend/docker-compose.app.yml`（gateway/auth/example 三服务）及 `taskify-gateway/Dockerfile`、`taskify-auth/Dockerfile`、`taskify-example/Dockerfile`（简单 Dockerfile，本地 `mvn clean package -Dmaven.test.skip=true` 打包后打镜像） per plan 结构决策 + research §15 (missing)

## Phase 12: Convergence

- [X] T071 将 `taskify-gateway` 网关依赖由 `spring-cloud-starter-gateway` 更换为 `spring-cloud-starter-gateway-server-webflux`（WebFlux 版） per plan 主要依赖 + research §4 (contradicts)

## Phase 13: Convergence

- [X] T072 调整 `taskify-gateway/src/main/resources/application.yml` 的 `spring.cloud.gateway.*` 配置方式（routes/globalcors 前缀）以匹配 `spring-cloud-starter-gateway-server-webflux` per 用户输入 + T071 (missing)
