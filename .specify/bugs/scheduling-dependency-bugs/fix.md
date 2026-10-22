# Bug Fix: 排期/阻塞功能四类缺陷

- **Slug**: scheduling-dependency-bugs
- **Fixed**: 2026-10-22
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

修复 003 功能四类缺陷：网关补配 `/api/v1/reminders/**`（F3 删除提醒 404）；后端硬阻塞改为「被阻塞任务禁止向更靠后的列推进、允许回退」（F4，用户确认语义）；前端被阻塞拖拽回弹（F2）与阻塞状态实时刷新（F1）。

## Changes

| File | Change | Notes |
|------|--------|-------|
| `backend/taskify-gateway/src/main/resources/application.yml` | modified | project 路由 `Path` 追加 `/api/v1/reminders/**` |
| `backend/taskify-project/src/main/java/com/taskify/project/service/TaskService.java` | modified | `move()` 硬阻塞判断由 `target == IN_PROGRESS` 改为 `isBlocked && target.order() > current.order()` |
| `frontend/src/composables/useBoard.ts` | modified | 阻塞判断对齐「向前推进」；被阻塞时 `upsert({...task})` 回弹；进入/离开 DONE 后 `reload()` |
| `frontend/src/stores/tasks.ts` | modified | 新增 `currentProjectId` 与 `reload()` |
| `frontend/src/stores/notifications.ts` | modified | SSE 收到 `TASK_BLOCKED`/`TASK_UNBLOCKED` 时触发任务列表刷新 |
| `backend/taskify-project/src/test/java/com/taskify/project/TaskDependencyIntegrationTest.java` | added test | 新增 `blockedTaskCannotAdvanceButCanRevert` |
| `frontend/tests/unit/useBoard.spec.ts` | modified | 覆盖向前推进拒绝/回退允许/回弹/DONE 刷新 |

## Diff Highlights

```java
// TaskService.move：被阻塞任务禁止向前推进，允许回退
if (isBlocked(id) && target.order() > current.order()) {
    throw BusinessException.of(ErrorCode.TASK_BLOCKED, "任务被未完成的依赖任务阻塞，无法推进");
}
```

```ts
// useBoard.moveTask：被阻塞且向前推进 → 提示并回弹
if (task.blocked && isAdvancing(task.status, toStatus)) {
  push('任务被未完成的依赖任务阻塞，无法推进', 'error')
  upsert({ ...task }) // 触发列重算，回弹 VueDraggable 的本地变更
  return false
}
```

```yaml
# gateway：补配 reminders 路由
- Path=/api/v1/projects/**, /api/v1/tasks/**, /api/v1/comments/**, /api/v1/reminders/**
```

## Tests Added or Updated

- `backend/taskify-project/.../TaskDependencyIntegrationTest.blockedTaskCannotAdvanceButCanRevert` — 锁住「进行中且被阻塞 → 审查中被拒、回退待办被允许」。
- `frontend/tests/unit/useBoard.spec.ts` — 锁住被阻塞向前推进拒绝、回退允许、回弹重算、DONE 刷新。

## Local Verification

- `cd frontend && pnpm exec vue-tsc -b` → 通过（无错误）
- `cd frontend && pnpm test` → 22/22 通过
- `cd backend && mvn -q test -pl taskify-project -am -Dtest=TaskDependencyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false` → `Tests run: 3, Failures: 0, Errors: 0`（含新增用例）

## Deviations from Assessment

- **F2 修复位置**：评估建议改 `KanbanColumn.onAdd`，实际改为在 `useBoard.moveTask` 被阻塞分支调用 `upsert({...task})`，通过 store 变更触发两列 watch 重算——单点修复同时覆盖源列与目标列，无需触碰列组件。
- **F1 触发范围**：评估建议「moveTask DONE 转换 + SSE + DependencyEditor」。实际用「moveTask DONE 转换 + SSE 驱动 reload」覆盖；依赖建立/删除因后端本就发布 `TASK_BLOCKED`/`TASK_UNBLOCKED` 事件，经 SSE 同样触发 reload，故未单独改 `DependencyEditor`。

## Follow-ups

- 网关路由变更需重启 `taskify-gateway` 才生效；`docker compose -f docker-compose.app.yml` 重建网关服务。
- F1 的 SSE 驱动刷新在收到多条 block/unblock 事件时会触发多次 reload，规模下可做防抖合并。
- 建议后续 `/speckit-bug-test` 做端到端回归确认四项修复。
