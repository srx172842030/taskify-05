# 快速验证指南：UI 组件体系重建与复用

**特性**：[spec.md](./spec.md) | **阶段**：Phase 1（验证指南）

本文件给出可运行的端到端验证步骤，用于证明重构完成且无回归。不包含实现代码；具体实现见 [tasks.md](./tasks.md) 与 [contracts/ui-components.md](./contracts/ui-components.md)。

## 前置条件

- Node.js 与 pnpm 已安装；仓库根与 `frontend/` 依赖已安装。
- 后端（可选）运行于 `http://localhost:8080`（前端 `/api` 已代理到此；纯 UI 验证可跳过）。

## 一、重建基础 UI 组件

```bash
cd frontend
# 1) 初始化 shadcn-vue（生成 components.json、src/lib/utils.ts、补依赖）
pnpm shadcn-vue init
# 2) 安装 17 类官方组件（19 个 registry 项）
pnpm shadcn-vue add @shadcn/button @shadcn/input @shadcn/textarea @shadcn/select \
  @shadcn/checkbox @shadcn/switch @shadcn/form @shadcn/field @shadcn/label \
  @shadcn/dialog @shadcn/drawer @shadcn/dropdown-menu @shadcn/sonner \
  @shadcn/table @shadcn/tabs @shadcn/card @shadcn/badge @shadcn/skeleton @shadcn/pagination
```

**预期**：`src/components/ui/` 下出现官方组件（含 `button`、`select`、`sonner` 等目录），`src/lib/utils.ts` 存在。

**核对（MCP 审计清单）**：

- [ ] 导入方式正确（具名/默认一致）
- [ ] 依赖齐全（`reka-ui`、`class-variance-authority`、`clsx`、`tailwind-merge`、`tailwindcss-animate`、`vue-sonner`）
- [ ] 无 lint 告警、无 TypeScript 错误

## 二、重写业务组件与布局

迁移 `components/`（auth/board/notification/project/task/state）与 `layouts/` 全部组件到官方组件，替换 `useToast` 为 `sonner`，删除旧 `ui/*.vue` 与 `composables/useToast.ts`。

**残留核对**：

```bash
# 旧 ui 目录应为空/已删除，无手写基础控件残留
grep -rn "components/ui/\(Button\|Input\|Select\|Tabs\|Dialog\|Drawer\|DropdownMenu\|Toast\|FormField\)" src || echo "无旧组件引用"
# 业务组件/布局/页面内不应再有自写原生控件
grep -rnE "<button|<input|<select|<textarea" src/components src/layouts src/pages/design || echo "无自写基础控件"
```

## 三、运行质量门禁

```bash
cd frontend
pnpm lint          # ESLint，无错误
pnpm format        # 可选：统一格式
pnpm test          # Vitest 单元测试通过
pnpm build         # vue-tsc -b + vite build，无缺失依赖/导入错误
pnpm e2e           # Playwright 端到端通过（如已配置）
```

**预期**：全部命令通过，无新增告警或错误。

## 四、关键用户旅程核对

启动 `pnpm dev`，逐一确认以下流程行为与重构前一致：

1. 登录对话框：打开/关闭、表单校验、提交。
2. 项目列表与详情：标签页切换、创建表单。
3. 看板：卡片展示、拖拽、状态徽标。
4. 任务详情抽屉：抽屉开合、多标签页、依赖/提醒编辑、评论提交。
5. 通知：铃铛与面板、未读徽标。
6. `/design` 页面：所有官方组件正常渲染，手机宽度无横向滚动。

## 五、章程符合性自检

- [ ] `components/ui/` 全部来自官方 registry，仿制品 = 0（SC-001）
- [ ] 业务组件/布局无自写基础控件（SC-002）
- [ ] `/design` 页面 100% 复用官方组件，无内联自定义控件样式（SC-003）
- [ ] 界面满足 WCAG AA（键盘导航、语义标记、对比度）（章程 III）
- [ ] 文档均为中文（章程 VI）
- [ ] 无「跳过 MCP 调研直接手写」的情况；确需自实现处注释 `registry 无此组件`（章程 VII）
