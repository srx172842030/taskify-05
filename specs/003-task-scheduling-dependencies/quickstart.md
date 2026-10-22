# 快速验证指南：Taskify 任务时间与依赖关系

本文档提供端到端可运行的验证场景，证明 003 功能（开始/截止时间、全天标记、多提醒、硬阻塞依赖）真实可用。前置环境与启动命令见 `../../README.md` 与 `../002-taskify-core/quickstart.md`（中间件 + 后端 + 前端均已就绪）。

## 前置条件

1. 中间件与后端服务已启动（`taskify-project`、`taskify-notification` 等）。
2. 前端 `pnpm dev` 已启动，浏览器登录预置账号（如 `pm_user` / `taskify123`）。
3. 项目内已有若干任务（可复用 002 示例项目，或新建项目与任务）。

## 场景 1：设置开始/截止时间（含全天）

1. 打开某项目，点击任务卡片打开右侧详情抽屉 → 编辑任务。
2. 为任务设置开始时间与截止时间，将其中一个标为「全天」。
3. 保存后，在任务卡片、任务列表、详情抽屉三处确认时间一致展示（全天只显示日期）。

**后端校验**（curl，`$TOKEN` 为登录令牌，`$TASK` 为任务 ID）：

```bash
# 截止早于开始应被拒绝（PARAM_ERROR）
curl -s -X PATCH "http://localhost:8080/api/v1/tasks/$TASK" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"startAt":"2026-10-28T09:00:00Z","dueAt":"2026-10-25T09:00:00Z"}'
```

预期：返回 `PARAM_ERROR`，任务时间不变。

## 场景 2：硬阻塞（建立依赖 + 阻止开始 + 解除通知）

1. 创建任务 A 与任务 B（A 未完成）。
2. 在 B 详情中建立「A 阻塞 B」（A 为被依赖任务）。
3. 确认 B 被标记「不可开始」并列出阻塞任务 A。
4. 尝试把 B 拖到「进行中」（或调用 move 接口），确认被阻止并给出警示。

```bash
# 目标为 IN_PROGRESS 且 B 被阻塞，应返回 TASK_BLOCKED
curl -s -X POST "http://localhost:8080/api/v1/tasks/$B/move" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toStatus":"IN_PROGRESS"}'
```

5. 将 A 移到「已完成」：确认 B 的「不可开始」标记解除、可正常开始，且 B 负责人收到「解除阻塞」通知（铃铛下拉面板，无需刷新）。

**预期**：move 被拒绝返回 `TASK_BLOCKED`；A 完成后 B 可移动且收到通知。

## 场景 3：多个提醒节点（绝对 + 相对）

1. 为任务配置两个提醒：一个绝对时间点、一个「截止前 1 天」的相对提前量。
2. 将截止时间设为很近的将来（或临时把相对提醒锚定到已设时间），等待调度器扫描（扫描周期 ≤ 1s，通常几秒内触发）。
3. 确认负责人收到「提醒」通知（铃铛面板，无需刷新）。

```bash
# 创建绝对提醒
curl -s -X POST "http://localhost:8080/api/v1/tasks/$TASK/reminders" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"mode":"ABSOLUTE","triggerAt":"2026-10-25T09:00:00Z"}'

# 创建相对提醒（截止前 1 天 = 1440 分钟）
curl -s -X POST "http://localhost:8080/api/v1/tasks/$TASK/reminders" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"mode":"RELATIVE","offsetMinutes":1440,"anchor":"DUE"}'
```

**预期**：提醒创建成功；到期后负责人收到 `REMINDER` 通知，且已触发提醒不重复触发。

## 场景 4：循环依赖被拒绝

```bash
# A 阻塞 B 后，再尝试 B 阻塞 A，应返回 PARAM_ERROR
curl -s -X POST "http://localhost:8080/api/v1/tasks/$A/dependencies" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"blockerTaskId\":\"$B\"}"
```

**预期**：返回 `PARAM_ERROR`，不建立循环依赖。

## 场景 5：无日历视图 + 手机宽度

1. 浏览全部页面，确认不存在日历/时间轴视图，排期与依赖信息仅在卡片/列表/详情中表达。
2. 在手机宽度（开发者工具响应式）下查看任务时间与阻塞信息，确认可读且无横向滚动。

## 自动化验证（开发期）

- 后端：`cd backend && mvn -q -DskipTests install` 后运行 `taskify-project` 与 `taskify-notification` 的集成测试（时间校验、提醒解析、硬阻塞 move 拒绝、循环依赖拒绝、阻塞状态通知）。
- 前端：`cd frontend && pnpm test`（组件/单元：时间字段、阻塞徽标、提醒表单）+ `pnpm exec playwright test`（E2E：硬阻塞拖拽被阻止、提醒通知、手机宽度无横向滚动）。
