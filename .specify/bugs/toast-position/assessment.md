# Bug Assessment: Toast 弹出位置错误（未悬浮、占据主面板空间）

- **Slug**: toast-position
- **Created**: 2026-10-25
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> toast 弹出位置错误，没有悬浮在任何位置，而是出现在页面最底部，占用了主面板空间。

## Symptom

Toast 通知（vue-sonner 的 `<Toaster />`）本应悬浮在视口角落（默认右下），但实际渲染在文档流末尾——出现在 `<AppLayout>` 下方的最底部，作为块级元素占用主面板空间，而非覆盖在内容上。

## Reproduction

1. 启动前端（后端非必需）并触发任一 toast，例如项目创建成功、任务移动提示、评论发表等。
2. 观察：toast 出现在页面最底部，未悬浮覆盖在内容上方，且挤压/占用了布局空间。
3. 期望：toast 悬浮在视口固定位置（如右下角），不参与文档流。

## Suspected Code Paths

- `frontend/src/main.ts:5` — 仅导入 `./styles/tokens.css`，未导入 vue-sonner 的样式。
- `frontend/src/components/ui/sonner/Sonner.vue:5` — 仅 `import { Toaster as Sonner } from "vue-sonner"`（JS 组件），未导入其 CSS。
- `node_modules/vue-sonner/lib/index.css:20-21` — 定义 `[data-sonner-toaster] { position: fixed; … }`，但该文件未被任何入口加载。

## Root Cause Hypothesis

vue-sonner 的定位样式（`[data-sonner-toaster] { position: fixed; … }`）位于 `vue-sonner/lib/index.css`，经 `package.json` 导出为 `vue-sonner/style.css`。该 CSS 未被任何入口导入：`main.ts` 仅导入 `tokens.css`，`Sonner.vue` 只导入 JS 组件、未导入样式。因此 toast 视口失去 `position: fixed`，退化为文档流中的普通块元素，渲染在 `<Toaster />` 所在位置（`App.vue` 中 `<AppLayout>` 之后），即页面最底部并占用空间。

Confidence: **high**（根因由代码证据直接支持：CSS 定义存在但未加载）。

## Proposed Remediation

**Preferred**: 在 `frontend/src/main.ts` 中补充导入 `import 'vue-sonner/style.css'`，与 `import './styles/tokens.css'` 并列，使 vue-sonner 的固定定位样式生效。这是最小、最标准的修复。

**Alternatives**:
- 在 `frontend/src/components/ui/sonner/Sonner.vue` 的 `<script>` 中导入该 CSS，使组件自带样式（自包含）。代价：若未来有多处 Toaster 或需集中管理样式覆盖，需回到组件层处理。
- 在 `tokens.css` 中手动补 `[data-sonner-toaster] { position: fixed; }` 等规则。代价：重复维护 vue-sonner 内部样式，易与上游漂移，不推荐。

**Files likely to change**:
- `frontend/src/main.ts`（一行导入）

**Tests to add or update**:
- 新增 e2e 冒烟：触发一次 toast 后，断言 toast 视口元素具有固定定位（`position: fixed`）或位于视口内（不占用文档流高度）。现有 e2e 未覆盖 toast 定位。

## Risks & Considerations

- 导入 vue-sonner CSS 后，需确认其默认样式（背景、圆角、阴影、字体）与项目设计令牌一致；shadcn-vue 的 `Sonner.vue` 已通过 `toastOptions.classes` 覆盖 toast 外观，导入 CSS 后应无冲突，但需目视回归检查（亮/暗两主题）。
- 无数据 / 迁移 / 性能 / 安全风险；纯样式导入，影响面小。

## Open Questions

- 无（根因明确）。
