# Linear / Vercel 风格设计系统 —— 移植包

## 文件清单

| 路径 | 作用 |
|---|---|
| `src/index.css` | CSS 变量 + 主题 token + 工具类 + 动画 |
| `tailwind.config.ts` | Tailwind 配置（消费 HSL 变量、字体栈、keyframes） |
| `src/lib/theme-context.tsx` | 主题状态机（light/dark/auto + 切换动画） |
| `src/components/app/theme-toggle.tsx` | 顶栏三态切换 DropdownMenu |

## 依赖

```bash
npm i tailwindcss-animate class-variance-authority clsx tailwind-merge
npm i lucide-react
npm i @radix-ui/react-dropdown-menu @radix-ui/react-slot
```

并确保已安装 shadcn/ui 的 `button` + `dropdown-menu` 组件（`npx shadcn@latest add button dropdown-menu`）。

## 字体

设计系统使用以下字体栈（系统字体优先 + Web 字体兜底）：

### 正文：Inter + 中文回落
```html
<!-- 推荐：自托管或 Google Fonts CDN -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

完整 fallback 链（已写在 `tailwind.config.ts` 和 `index.css` body 中）：
```
Inter, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei",
"Source Han Sans SC", "Noto Sans SC", system-ui, -apple-system,
BlinkMacSystemFont, sans-serif
```
> macOS/iOS 用户走 PingFang SC，Windows 走 Microsoft YaHei，Linux/Android 走 Noto/Source Han Sans SC，全部系统自带，无需额外下载。

### 等宽：JetBrains Mono
```html
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
```
Fallback：`JetBrains Mono, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace`

### 字体特性
`index.css` 的 `body` 启用 Inter 风格化字形：
```css
font-feature-settings: "ss01", "cv11", "cv02";
```
- `ss01` 单层 a
- `cv11` 单层 g
- `cv02` 平直 l

## 安装步骤（新项目）

1. 复制 4 个文件到对应路径
2. `main.tsx` 包裹 ThemeProvider：
   ```tsx
   import { ThemeProvider } from "@/lib/theme-context"
   <ThemeProvider><App /></ThemeProvider>
   ```
3. 顶栏放 `<ThemeToggle />`
4. （可选）`index.html` 加 Inter / JetBrains Mono `<link>`
5. 确保 `tailwind.config.ts` 的 `darkMode: ["class"]` 已开启

## 主题切换动画

- **首选** View Transitions API（Chrome 111+ / Safari 18+）—— 原生圆形扩散
- **降级** 自定义 `.tide-overlay` 从点击点扩散（需要补一段 CSS，见下）

如果你想用 tide 降级动画，在 `index.css` 末尾加：
```css
.tide-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  background: var(--tide-color);
  clip-path: circle(0% at var(--tide-x) var(--tide-y));
  animation: tide-expand 1.5s cubic-bezier(0.22, 0.8, 0.25, 1) forwards;
}
@keyframes tide-expand {
  0%   { clip-path: circle(0%    at var(--tide-x) var(--tide-y)); }
  60%  { clip-path: circle(150%  at var(--tide-x) var(--tide-y)); }
  100% { clip-path: circle(0%    at var(--tide-x) var(--tide-y)); opacity: 0; }
}
```

## 使用规范（重要）

❌ **禁止** 在组件里写 `text-white` / `bg-black` / `text-gray-500`
✅ **必须** 用语义 token：`text-foreground` / `bg-background` / `text-muted-foreground` / `border-border` / `bg-primary` / `bg-card`

这样主题切换零成本，所有组件自动适配明暗。
