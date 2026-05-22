项目设计系统描述（可直接喂给 Claude / 其他 AI）

整体定位
Linear / Vercel 风格的高端极简设计系统，主打安静的近黑墨色调，强调克制、留白和细节质感。

---

配色系统（全部 HSL，通过 CSS 变量驱动）

白天模式（Light）
- 背景 `--background: 0 0% 100%`（纯白）
- 前景文字 `--foreground: 240 10% 9%`（近黑墨色，带极轻冷调）
- 卡片/弹层 同背景白
- 主色 Primary `240 10% 9%`（近黑，Linear 风格"墨"）+ 白色前景
- 次级表面 Secondary/Muted `240 5% 96%`（极浅灰）
- 强调表面 Accent `240 5% 94%`
- 边框/输入 `240 6% 90%`（细发丝灰）
- Destructive `0 72% 51%`（克制红）
- 品牌点缀（选用）Accent-Blue `230 85% 60%`（微靛蓝光晕，仅用于关键高亮）

黑夜模式（Dark）
- 背景 `240 10% 4%`（真·近黑画布，深邃）
- 前景 `0 0% 98%`
- 卡片 `240 8% 7%`（比背景略亮一档，制造层次）
- 主色翻转 白色 `0 0% 98%` 作主色
- 次级表面 `240 5% 12%`
- 边框 `240 5% 16%`
- Accent-Blue `230 85% 65%`（夜间稍提亮）

图表色板（克制 5 色）
近黑 / 蓝 `217 91% 60%` / 绿 `142 71% 45%` / 琥珀 `38 92% 50%` / 紫 `271 81% 56%`

渐变（三种语义渐变）
- `--gradient-surface`：白→极浅灰（页面表面）
- `--gradient-ink`：墨色 135° 渐变（深色块/按钮）
- `--gradient-accent`：靛蓝→紫罗兰 `230°→265°`（点睛色块）

阴影（三档软阴影）
全部基于 `hsl(240 10% 9% / α)`，白天用 4%–12% 透明度的冷调投影；夜间用纯黑 40%–60%。
- `--shadow-sm` 1px 微影
- `--shadow-md` 4px+2px 复合
- `--shadow-lg` 16px+4px 浮起感

---

圆角与间距
- 基础圆角 `--radius: 0.625rem`（10px），衍生 sm/md/lg/xl
- 卡片统一 `rounded-xl`、按钮 `rounded-md`、徽章 `rounded-full`
- 容器最大宽 `max-w-7xl`，内边距移动 `px-4` / 桌面 `px-8`

---

字体系统
- 正文：Inter + 中文回落 `PingFang SC / Hiragino Sans GB / Microsoft YaHei / Source Han Sans SC / Noto Sans SC`
- 等宽：JetBrains Mono / SF Mono / Menlo
- 字体特性：`font-feature-settings: "ss01","cv11","cv02"`（Inter 风格化字形）
- 抗锯齿 `-webkit-font-smoothing: antialiased` + `text-rendering: optimizeLegibility`
- 文字平衡 `.text-balance` 工具类（`text-wrap: balance`）

---

主题切换（Light / Dark / Auto 三态）

状态机
- 三种模式：`light` / `dark` / `auto`（跟随系统）
- 持久化到 `localStorage` key = `puyuan-theme`
- `auto` 模式监听 `matchMedia("(prefers-color-scheme: dark)")` 的 change 事件实时跟随
- 解析后通过 `document.documentElement.classList.toggle("dark")` + `style.colorScheme` 切换

切换动画（亮点）
1. 首选 View Transitions API（Chrome 111+ / Safari 18+）—— 原生圆形扩散转场
2. 降级方案：自定义 `.tide-overlay` 覆盖层从点击点（`--tide-x` / `--tide-y` CSS 变量）扩散
   - 600ms 时切换主题
   - 1500ms 时移除遮罩
   - 遮罩色 = 目标主题背景色（夜→`oklch(0.18 0.008 50)` 暖土色；昼→`oklch(0.985 0.008 75)` 奶油色）

UI 控件
顶栏 DropdownMenu，三个选项各带图标：
- 白天 ☀️ Sun
- 黑夜 🌙 Moon  
- 跟随系统 🖥️ Monitor
当前选中显示 ✓ Check + `text-primary` 高亮；auto 模式时按钮右下角显示一个 `bg-primary` 圆点指示。

---

工具类（utilities）
- `.gradient-surface` / `.gradient-ink` / `.gradient-accent` —— 三种渐变背景
- `.shadow-soft` / `.shadow-elevated` —— 软阴影
- `.hairline` —— 60% 透明的细发丝边
- `.glass` —— `bg-background/70 backdrop-blur-xl backdrop-saturate-150`（毛玻璃导航）
- `.hover-lift` —— `hover:-translate-y-0.5`（GPU 友好的微浮起）
- `.font-mono` —— 等宽字体 + `ss01` 特性

---

动画
统一缓动 `cubic-bezier(0.22, 0.8, 0.25, 1)`（命名 `ease-out-soft`），内置 keyframes：
- `fade-in` 0.4s（轻微上移 4px 淡入）
- `slide-up-fade` 0.45s（上移 8px 淡入）
- `subtle-pulse` 2.4s 循环（scale 1→1.06）
- `shimmer` 2s 线性（骨架屏）
- `accordion-down/up` 0.2s

无障碍：`@media (prefers-reduced-motion: reduce)` 全局降级到 0.01ms。

---

选择高亮
`::selection { background: hsl(var(--foreground)/0.12) }` —— 用前景色 12% 透明，自动适配明暗。

焦点环
`:focus-visible { ring-2 ring-ring/40 ring-offset-2 }` —— 极细克制焦点环。

---

核心设计哲学（一句话）
> 「近黑墨色 + 细发丝边 + 软阴影 + 微动效」，所有色值走 HSL CSS 变量，组件层禁止硬编码颜色，全部用 `bg-primary` / `text-foreground` / `border-border` 等语义 token，保证主题切换零成本。

需要的话我可以把 `index.css` + `tailwind.config.ts` + `theme-context.tsx` + `theme-toggle.tsx` 四个文件原文打包给你，直接复制到新项目即可复用。