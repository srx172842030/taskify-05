# 设计 token 清单与用法

语义化命名，禁用具体颜色名。token 定义于 `src/styles/tokens.css`（亮/暗两套）。

## 清单

| token | 用途 |
|-------|------|
| `--background` / `--foreground` | 页面背景 / 主文字 |
| `--card` / `--card-foreground` | 卡片背景 / 文字 |
| `--popover` / `--popover-foreground` | 弹层背景 / 文字 |
| `--primary` / `--primary-foreground` | 主色 / 主色上文字 |
| `--secondary` / `--secondary-foreground` | 次要背景 / 前景 |
| `--muted` / `--muted-foreground` | 弱化背景 / 文字 |
| `--accent` / `--accent-foreground` | 强调色 / 强调色上文字 |
| `--destructive` / `--destructive-foreground` | 危险色 / 危险色上文字 |
| `--border` | 边框 |
| `--input` | 输入控件边框/背景 |
| `--ring` | 焦点环 |
| `--radius` | 圆角 |

## 用法

- 组件内通过 `var(--token)` 引用，禁止硬编码颜色值。
- Tailwind 已在 `tailwind.config.js` 将语义 token 映射为颜色类（如 `bg-primary`、`text-muted-foreground`）。
- 换主题只改 `tokens.css` 中亮/暗两套值。

## 主题

- 亮色：`:root`；暗色：`.dark`。
- 三模式切换由 `src/composables/useTheme.ts`（@vueuse/core `useColorMode`，class 策略）驱动；入口内联脚本在首屏前设置 class 避免闪烁。
