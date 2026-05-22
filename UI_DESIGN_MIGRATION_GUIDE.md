# 濮院毛衫 AI 平台 UI 设计系统迁移指南

## 概述

本指南说明如何将现有组件的样式迁移到新的 Linear / Vercel 风格设计系统。设计系统使用 HSL CSS 变量，支持 Light / Dark / Auto 三态主题切换。

---

## 设计系统架构

### Web 端（merchant-web + admin-web）

```
src/design.css          # 设计系统主文件（CSS 变量 + 深色模式）
tailwind.config.js      # Tailwind 配置（映射 CSS 变量）
useTheme.ts            # 主题管理逻辑
ThemeToggle.vue        # 主题切换组件
```

### 小程序端（merchant-miniapp）

```
src/styles/design-tokens.scss   # SCSS 变量定义
src/styles/themes/_light.scss   # 亮色主题
src/styles/themes/_dark.scss    # 暗色主题
src/styles/animations.scss      # 动画定义
src/styles/utilities.scss       # 工具类
src/stores/theme.ts             # 主题 store
theme.json                     # 主题配置
```

---

## 核心原则

### 1. 禁止硬编码颜色

**❌ 错误做法：**
```vue
<template>
  <div style="background-color: #ffffff; color: #161616;">
    内容
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #e5e5e5;
}
</style>
```

**✅ 正确做法：**
```vue
<template>
  <div class="bg-background text-foreground">
    内容
  </div>
</template>

<style scoped>
.card {
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
}

/* 或使用 Tailwind */
.card {
  @apply bg-background text-foreground border-border;
}
</style>
```

### 2. 使用语义化变量

优先使用语义化变量而非具体颜色值：

| 用途 | CSS 变量 | Tailwind 类 |
|------|----------|-------------|
| 主背景 | `--background` | `bg-background` |
| 前景文字 | `--foreground` | `text-foreground` |
| 卡片背景 | `--card` | `bg-card` |
| 卡片文字 | `--card-foreground` | `text-card-foreground` |
| 主色按钮 | `--primary` | `bg-primary` |
| 次级表面 | `--secondary` | `bg-secondary` |
| 边框/输入框 | `--border` | `border-border` |
| 柔和文字 | `--muted-foreground` | `text-muted-foreground` |

### 3. 圆角和阴影使用变量

```css
/* 圆角 */
.rounded-md { border-radius: var(--radius); }          /* 10px */
.rounded-sm { border-radius: calc(var(--radius) - 4px); } /* 6px */
.rounded-xl { border-radius: calc(var(--radius) + 4px); } /* 14px */

/* 阴影 */
.shadow-soft { box-shadow: var(--shadow-md); }
.shadow-elevated { box-shadow: var(--shadow-lg); }
```

---

## Web 端迁移步骤

### Step 1: 替换硬编码颜色

查找并替换所有硬编码颜色：

```bash
# 搜索硬编码的颜色值
grep -rn "#[0-9a-fA-F]\{6\}" src/
grep -rn "rgb(" src/
grep -rn "rgba(" src/
```

### Step 2: 使用 CSS 变量

将颜色值替换为 CSS 变量：

| 原颜色 | 亮色变量 | 暗色变量 | 说明 |
|--------|----------|----------|------|
| `#ffffff` | `hsl(var(--background))` | `hsl(240 10% 4%)` | 背景 |
| `#161616` | `hsl(var(--foreground))` | `hsl(0 0% 98%)` | 前景 |
| `#e5e5e5` | `hsl(var(--border))` | `hsl(240 5% 16%)` | 边框 |
| `#f5f5f5` | `hsl(var(--secondary))` | `hsl(240 5% 12%)` | 次级表面 |
| `#6366f1` | `hsl(var(--accent-blue))` | `hsl(230 85% 65%)` | 强调色 |

### Step 3: 使用 Tailwind 工具类

优先使用 Tailwind 工具类而非自定义 CSS：

```vue
<!-- ❌ 错误 -->
<div class="custom-card"></div>

<style scoped>
.custom-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
</style>

<!-- ✅ 正确 -->
<div class="bg-card rounded-xl p-4 shadow-soft"></div>
```

### Step 4: 添加主题切换支持

确保组件在暗色模式下正常工作：

```vue
<template>
  <div class="card">
    <h2 class="text-card-foreground">标题</h2>
    <p class="text-muted-foreground">内容</p>
  </div>
</template>

<style scoped>
.card {
  @apply bg-card text-card-foreground border-border;
}
</style>
```

---

## 小程序端迁移步骤

### Step 1: 导入设计系统变量

```scss
@import './styles/design-tokens.scss';
@import './styles/themes/_light.scss';

// 使用变量
.my-card {
  background: $background;
  color: $foreground;
  border: 1px solid $border;
  border-radius: $radius-xl;
  padding: $spacing-lg;
  box-shadow: $shadow-sm;
}
```

### Step 2: 使用主题类

小程序不支持动态 CSS 变量，使用主题类切换：

```tsx
import { useThemeStore } from '@/stores/theme';

const themeStore = useThemeStore();

// 根据主题应用类名
const containerClass = computed(() => ({
  'light': true,
  'dark': themeStore.isDark
}));
```

```scss
// 样式文件
.container {
  background: $background-light;
  color: $foreground-light;
}

.dark .container {
  background: $background-dark;
  color: $foreground-dark;
}
```

### Step 3: 使用工具类

导入 utilities.scss 使用预定义的工具类：

```scss
@import './styles/utilities.scss';

// 使用工具类
.my-element {
  @extend .card;
  @extend .shadow-soft;
  @extend .rounded-xl;
}
```

---

## 常见问题

### Q1: 如何处理动态颜色？

**Web 端：** 使用 CSS 变量结合 `calc()` 函数

```css
.dynamic-opacity {
  background: hsl(var(--foreground) / 0.12);
}
```

**小程序端：** 使用 RGBA 格式

```scss
.dynamic-opacity {
  background: rgba(0, 0, 0, 0.12);
}

.dark .dynamic-opacity {
  background: rgba(255, 255, 255, 0.12);
}
```

### Q2: 如何处理渐变色？

使用设计系统预定义的渐变变量：

```css
/* Web 端 */
.gradient-surface {
  background: var(--gradient-surface);
}

.gradient-ink {
  background: var(--gradient-ink);
}

.gradient-accent {
  background: var(--gradient-accent);
}
```

```scss
/* 小程序端 */
.gradient-surface {
  background: $gradient-surface-light;
}

.dark .gradient-surface {
  background: $gradient-surface-dark;
}
```

### Q3: 如何处理动画？

使用统一的缓动函数 `cubic-bezier(0.22, 0.8, 0.25, 1)`：

```css
/* Web 端 */
.animate-fade-in {
  animation: fade-in 0.4s cubic-bezier(0.22, 0.8, 0.25, 1) both;
}
```

```scss
/* 小程序端 */
@import './styles/animations.scss';

.animate-fade-in {
  animation: fade-in 0.4s $ease-out-soft both;
}
```

### Q4: 如何处理第三方组件库样式？

使用 CSS 覆盖或 scoped 样式：

```vue
<template>
  <nut-button class="btn-primary">按钮</nut-button>
</template>

<style scoped>
.btn-primary {
  background: hsl(var(--primary)) !important;
  color: hsl(var(--primary-foreground)) !important;
  border-radius: var(--radius) !important;
}

.dark .btn-primary {
  background: hsl(0 0% 98%) !important;
  color: hsl(240 10% 9%) !important;
}
</style>
```

---

## 检查清单

迁移完成后，请检查以下项目：

- [ ] 所有硬编码颜色已替换为 CSS 变量或 SCSS 变量
- [ ] 所有圆角使用 `var(--radius)` 或 `$radius-*` 变量
- [ ] 所有阴影使用 `var(--shadow-*)` 或 `$shadow-*` 变量
- [ ] 所有间距使用统一的 `spacing` 变量
- [ ] 组件在亮色模式下显示正常
- [ ] 组件在暗色模式下显示正常
- [ ] 主题切换动画平滑（Web 端）
- [ ] 文字对比度满足 WCAG AA（≥4.5:1）
- [ ] 无障碍支持（focus-visible, reduced-motion）

---

## 变量映射表

### 颜色变量

| 用途 | Web 端 CSS 变量 | 小程序端 SCSS 变量 |
|------|-----------------|-------------------|
| 主背景 | `--background` | `$background-light` / `$background-dark` |
| 前景文字 | `--foreground` | `$foreground-light` / `$foreground-dark` |
| 卡片背景 | `--card` | `$card-light` / `$card-dark` |
| 卡片文字 | `--card-foreground` | `$card-foreground-light` / `$card-foreground-dark` |
| 主色 | `--primary` | `$primary-light` / `$primary-dark` |
| 次级表面 | `--secondary` | `$secondary-light` / `$secondary-dark` |
| 边框 | `--border` | `$border-light` / `$border-dark` |
| 柔和文字 | `--muted-foreground` | `$muted-foreground-light` / `$muted-foreground-dark` |
| 强调色 | `--accent-blue` | `$accent-blue-light` / `$accent-blue-dark` |

### 尺寸变量

| 用途 | Web 端 | 小程序端 |
|------|--------|----------|
| 基础圆角 | `var(--radius)` | `$radius-lg` (10px) |
| 小圆角 | `calc(var(--radius) - 4px)` | `$radius-sm` (6px) |
| 大圆角 | `calc(var(--radius) + 4px)` | `$radius-xl` (12px) |
| 小间距 | `8px` | `$spacing-sm` (8px) |
| 中间距 | `12px` | `$spacing-md` (12px) |
| 大间距 | `16px` | `$spacing-lg` (16px) |
| 超大间距 | `24px` | `$spacing-2xl` (24px) |

### 阴影变量

| 用途 | Web 端 | 小程序端 |
|------|--------|----------|
| 小阴影 | `var(--shadow-sm)` | `$shadow-sm-light` / `$shadow-sm-dark` |
| 中阴影 | `var(--shadow-md)` | `$shadow-md-light` / `$shadow-md-dark` |
| 大阴影 | `var(--shadow-lg)` | `$shadow-lg-light` / `$shadow-lg-dark` |

---

## 工具类快速参考

### Web 端 Tailwind 工具类

```css
/* 颜色 */
bg-background              /* 主背景 */
text-foreground            /* 前景文字 */
bg-card                    /* 卡片背景 */
border-border              /* 边框 */
text-muted-foreground      /* 柔和文字 */
bg-primary                 /* 主色按钮 */

/* 圆角 */
rounded-md                 /* 中等圆角 (6px) */
rounded-lg                 /* 大圆角 (10px) */
rounded-xl                 /* 超大圆角 (14px) */
rounded-full               /* 完整圆形 */

/* 阴影 */
shadow-sm                  /* 小阴影 */
shadow-soft                /* 软阴影 */
shadow-elevated            /* 浮起阴影 */

/* 渐变 */
gradient-surface           /* 表面渐变 */
gradient-ink               /* 墨色渐变 */
gradient-accent            /* 强调渐变 */

/* 工具 */
glass                      /* 毛玻璃效果 */
hairline                   /* 细发丝边 */
hover-lift                 /* 悬停浮起 */
```

### 小程序端 SCSS 工具类

```scss
/* 卡片 */
.card                      /* 基础卡片 */
.card-elevated             /* 浮起卡片 */

/* 按钮 */
.btn-primary               /* 主色按钮 */
.btn-secondary             /* 次级按钮 */
.btn-ghost                 /* 透明按钮 */

/* 状态标签 */
.tag                       /* 基础标签 */
.tag-success               /* 成功标签 */
.tag-warning               /* 警告标签 */
.tag-danger                /* 危险标签 */
.tag-info                  /* 信息标签 */

/* 文本 */
.text-muted                /* 柔和文字 */
.text-primary              /* 主色文字 */
.text-accent               /* 强调文字 */

/* 布局 */
.page-container            /* 页面容器 */
.flex-center               /* 居中布局 */
.flex-between              /* 两端对齐 */

/* 空状态 */
.empty-state               /* 空状态容器 */
```

---

## 资源链接

- **设计系统文档**：[UI设计系统.md](./UI设计系统.md)
- **Web 端设计系统**：`frontend/merchant-web/src/design.css`
- **小程序端设计系统**：`frontend/merchant-miniapp/src/styles/`
- **Tailwind 配置**：`tailwind.config.js`

---

## 技术支持

如有问题，请查看以下资源：

1. Linear Design System 参考：https://linear.app/design
2. Vercel Design System 参考：https://vercel.com/design
3. Tailwind CSS 文档：https://tailwindcss.com/docs
4. Taro 框架文档：https://taro-docs.jd.com/