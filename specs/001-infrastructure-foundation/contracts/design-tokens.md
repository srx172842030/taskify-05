# 契约：设计 token 语义

设计 token 使用**语义化命名**，不使用具体颜色名。所有组件仅引用语义 token，实现换肤时只改 token 值。亮/暗两套定义在 `frontend/src/styles/tokens.css`；清单与用法同步到 `frontend/docs/design-tokens.md`。

## 语义 token 清单

| token | 用途 |
|-------|------|
| `--background` | 页面/内容区背景 |
| `--foreground` | 主文字颜色 |
| `--card` | 卡片背景 |
| `--card-foreground` | 卡片文字 |
| `--popover` | 弹层（下拉/气泡）背景 |
| `--popover-foreground` | 弹层文字 |
| `--primary` | 主色（按钮、链接、焦点） |
| `--primary-foreground` | 主色上的文字 |
| `--secondary` | 次要背景/次级按钮 |
| `--secondary-foreground` | 次要前景 |
| `--muted` | 弱化背景 |
| `--muted-foreground` | 弱化文字 |
| `--accent` | 强调色 |
| `--accent-foreground` | 强调色上的文字 |
| `--destructive` | 危险/删除操作色 |
| `--destructive-foreground` | 危险色上的文字 |
| `--border` | 边框色 |
| `--input` | 输入控件边框/背景 |
| `--ring` | 焦点环色 |
| `--radius` | 圆角半径 |

## 命名规则

- 一律语义化，禁止 `--blue-500`、`--gray-100` 之类具体颜色名。
- 每个 token 在亮/暗两套主题下均有定义。
- 组件与布局通过 `var(--token)` 引用，禁止硬编码颜色值。

## 主题

- 亮色：`:root` 或 `[data-theme="light"]`。
- 暗色：`[data-theme="dark"]` 或 `.dark`（与 `useColorMode` 的 class 策略对应）。
- 「跟随系统」由入口内联脚本根据 `prefers-color-scheme` 设置初始 class，避免首屏闪烁。

## 组件绑定

- 按钮：`--primary` / `--secondary` / `--destructive` + `--radius`。
- 表单控件：`--input` / `--border` / `--ring`。
- 文本：`--foreground` / `--muted-foreground`。
- 卡片/弹层：`--card` / `--popover`。
