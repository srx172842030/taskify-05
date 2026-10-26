# 实施计划：UI 组件体系重建与复用

**分支**：`004-rebuild-ui-components` | **日期**：2026-10-23 | **规格**：[spec.md](./spec.md)

**输入**：来自 `/specs/004-rebuild-ui-components/spec.md` 的功能规格说明

## 概述

删除前端 `components/ui/` 下 17 个 AI 手写的 shadcn 风格仿制组件，改由 shadcn-vue 官方 registry 安装同功能组件；随后重写 `components/` 与 `layouts/` 下所有业务组件与布局，使其复用官方 UI 组件，消除自写基础控件与重复实现；最后重写 `/design` 页面，使其所有界面元素复用官方组件。整个重构不改变既有功能与交互行为，且须通过既有质量门禁（类型检查、静态分析、测试、构建）。

## 技术上下文

**语言/版本**：Vue 3.5（`<script setup lang="ts">`）、TypeScript 5.7

**主要依赖**：shadcn-vue 2.8（CLI，当前位于仓库根）、reka-ui（官方无头组件基础）、class-variance-authority、clsx、tailwind-merge、tailwindcss-animate、vue-sonner；已有：lucide-vue-next、vee-validate + zod（表单）

**存储**：不涉及（纯前端代码结构重构，无新增持久化数据）

**测试**：Vitest（单元）、Playwright（e2e）、vue-tsc（类型检查）、ESLint + Prettier

**目标平台**：Web 浏览器（Vite 开发服务器 + 生产构建）

**项目类型**：Web 应用（Vue 3 SPA，仓库内 frontend 子项目）

**性能目标**：重构不引入新业务逻辑，包体积与首屏性能不显著退化

**约束**：WCAG AA 可访问性基线（章程原则 III）；不改变后端 API 与数据模型；文档中文化（章程原则 VI）；复用官方组件、禁止跳过 MCP 调研直接手写（章程原则 VII）

**规模/范围**：17 个 UI 组件 + 14 个业务/状态组件 + 2 个布局 + `/design` 页面重写；另有 `ProjectDetailPage`、`ProjectListPage` 两页需最小适配（导入路径）

## 章程检查

*门禁：必须在 Phase 0 研究前通过；Phase 1 设计后复查。*

| 章程原则 | 符合情况 | 判定 |
|---------|---------|------|
| I. 代码质量优先 | 删除仿制品与重复实现，正符合；须通过类型检查、静态分析、测试 | ✅ 通过 |
| II. 测试标准（不可协商） | 重构须以既有测试套件 + 类型检查 + 构建为门禁；行为不回归 | ✅ 通过 |
| III. 用户体验一致性 | 统一到共享设计系统、单一实现来源，正是本特性目标 | ✅ 通过 |
| IV. 性能要求 | 重构不引入新逻辑，须在前后度量包体积与首屏，无明显退化 | ✅ 通过 |
| V. 简洁性与可维护性 | 以官方组件替换仿制品即是最简方案，不引入多余抽象 | ✅ 通过 |
| VI. 文档中文化 | 全部产出文档以中文撰写 | ✅ 通过 |
| VII. UI 组件复用（不可复制实现） | 本特性核心目标；须遵循「官方注册表优先 + MCP 调研 + registry 无此组件注释」 | ✅ 通过 |

**结论**：无违反项，无「必须豁免的复杂度」需要记录，`Complexity Tracking` 留空。

## 项目结构

### 文档（本特性）

```text
specs/004-rebuild-ui-components/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出（组件清单与映射）
├── quickstart.md        # Phase 1 输出（验证指南）
├── contracts/           # Phase 1 输出（UI 组件契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks，本命令不生成）
```

### 源代码（仓库根，frontend 子项目）

```text
frontend/
├── components.json                 # 新增：shadcn-vue 初始化配置
├── package.json                    # 变更：补 shadcn-vue 相关依赖
├── tailwind.config.js              # 变更：补 tailwindcss-animate 插件
├── src/
│   ├── lib/
│   │   └── utils.ts                # 新增：cn() 工具（clsx + tailwind-merge）
│   ├── components/
│   │   ├── ui/                     # 重建：官方 shadcn-vue 组件（19 项）
│   │   ├── auth/ board/ notification/ project/ task/  # 重写：复用 ui/
│   │   └── state/                  # 重写：LoadingState 复用 Skeleton
│   ├── layouts/                    # 重写：复用 ui/
│   ├── pages/design/               # 重写：复用 ui/
│   ├── pages/projects/             # 最小适配：导入路径
│   └── composables/                # 适配：useToast → sonner
└── ...
```

**结构决策**：沿用仓库既有 frontend 结构，不改变目录骨架。仅新增 shadcn-vue 所需 `frontend/components.json` 与 `frontend/src/lib/utils.ts`；UI 组件集中于 `src/components/ui/`，业务组件保留在原领域子目录（auth/board/notification/project/task/state），仅改内部实现为复用官方组件。shadcn-vue 的初始化与安装以 `frontend/` 为目标目录（详见 [research.md](./research.md) 决策 D1）。

## Complexity Tracking

> 无章程违反项，本节留空。
