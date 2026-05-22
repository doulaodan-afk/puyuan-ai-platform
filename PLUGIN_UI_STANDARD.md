# 濮院毛衫 AI 平台 - 插件 UI 规范

> **版本**: 1.0.0
> **最后更新**: 2026-05-22
> **适用对象**: 所有 AI 插件开发者（包括主框架、第三方插件、微前端/iframe 插件）

---

## 一、设计令牌（Design Tokens）

所有颜色、字体、圆角、阴影均通过 CSS 变量定义。**严禁硬编码任何色值**。

### 1.1 深色主题（默认）

```css
:root,
[data-theme='dark'] {
  /* ===== 背景与前景 ===== */
  --background: 240 10% 4%;           /* 主背景 - 近黑 */
  --foreground: 0 0% 98%;             /* 主文字 - 接近白色 */

  /* ===== 卡片 ===== */
  --card: 240 8% 7%;                  /* 卡片背景 */
  --card-foreground: 0 0% 98%;        /* 卡片文字 */

  /* ===== 弹出层 ===== */
  --popover: 240 8% 7%;
  --popover-foreground: 0 0% 98%;

  /* ===== 主色调 ===== */
  --primary: 0 0% 98%;               /* 主要按钮背景 */
  --primary-foreground: 240 10% 9%;  /* 主要按钮文字 */

  /* ===== 次要色调 ===== */
  --secondary: 240 5% 12%;
  --secondary-foreground: 0 0% 98%;

  /* ===== 静音/次要文字 ===== */
  --muted: 240 5% 12%;
  --muted-foreground: 240 5% 65%;

  /* ===== 强调色 ===== */
  --accent: 240 5% 14%;
  --accent-foreground: 0 0% 98%;

  /* ===== 危险/错误 ===== */
  --destructive: 0 70% 55%;
  --destructive-foreground: 0 0% 100%;

  /* ===== 边框与输入 ===== */
  --border: 240 5% 16%;
  --input: 240 5% 16%;
  --ring: 0 0% 90%;

  /* ===== 圆角 ===== */
  --radius: 0.625rem;  /* 10px */

  /* ===== 图表色板 ===== */
  --chart-1: 0 0% 98%;
  --chart-2: 217 91% 65%;
  --chart-3: 142 71% 50%;
  --chart-4: 38 92% 55%;
  --chart-5: 271 81% 62%;

  /* ===== 品牌强调色 ===== */
  --accent-blue: 230 85% 65%;
  --accent-blue-foreground: 240 10% 9%;

  /* ===== 阴影 ===== */
  --shadow-sm: 0 1px 2px 0 hsl(0 0% 0% / 0.4);
  --shadow-md: 0 4px 12px -2px hsl(0 0% 0% / 0.5),
               0 2px 6px -2px hsl(0 0% 0% / 0.4);
  --shadow-lg: 0 16px 40px -12px hsl(0 0% 0% / 0.6),
               0 4px 12px -4px hsl(0 0% 0% / 0.5);

  /* ===== 渐变 ===== */
  --gradient-surface: linear-gradient(180deg,
    hsl(240 10% 5%) 0%,
    hsl(240 10% 4%) 100%);
  --gradient-ink: linear-gradient(135deg,
    hsl(240 8% 9%) 0%,
    hsl(240 6% 14%) 100%);
  --gradient-accent: linear-gradient(135deg,
    hsl(230 85% 65%) 0%,
    hsl(265 80% 67%) 100%);
}
```

### 1.2 浅色主题覆盖

```css
[data-theme='light'] {
  --background: 0 0% 100%;
  --foreground: 240 10% 9%;

  --card: 0 0% 100%;
  --card-foreground: 240 10% 9%;

  --popover: 0 0% 100%;
  --popover-foreground: 240 10% 9%;

  --primary: 240 10% 9%;
  --primary-foreground: 0 0% 100%;

  --secondary: 240 5% 96%;
  --secondary-foreground: 240 10% 9%;

  --muted: 240 5% 96%;
  --muted-foreground: 240 4% 60%;

  --accent: 240 5% 94%;
  --accent-foreground: 240 10% 9%;

  --destructive: 0 72% 51%;
  --destructive-foreground: 0 0% 100%;

  --border: 240 6% 90%;
  --input: 240 6% 90%;
  --ring: 240 10% 9%;

  --chart-1: 240 10% 9%;
  --chart-2: 217 91% 60%;
  --chart-3: 142 71% 45%;
  --chart-4: 38 92% 50%;
  --chart-5: 271 81% 56%;

  --accent-blue: 230 85% 60%;
  --accent-blue-foreground: 0 0% 100%;

  --shadow-sm: 0 1px 2px 0 hsl(240 10% 9% / 0.04);
  --shadow-md: 0 4px 12px -2px hsl(240 10% 9% / 0.06),
               0 2px 6px -2px hsl(240 10% 9% / 0.04);
  --shadow-lg: 0 16px 40px -12px hsl(240 10% 9% / 0.12),
               0 4px 12px -4px hsl(240 10% 9% / 0.06);

  --gradient-surface: linear-gradient(180deg,
    hsl(0 0% 100%) 0%,
    hsl(240 10% 98%) 100%);
  --gradient-ink: linear-gradient(135deg,
    hsl(240 10% 9%) 0%,
    hsl(240 8% 18%) 100%);
  --gradient-accent: linear-gradient(135deg,
    hsl(230 85% 60%) 0%,
    hsl(265 80% 62%) 100%);
}
```

---

## 二、使用方式

### 2.1 在 CSS 中使用变量

```css
/* 正确 ✓ */
.element {
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
}

/* 错误 ✗ */
.element {
  background: #1f2a44;      /* 禁止硬编码 */
  color: #ffffff;           /* 禁止硬编码 */
}
```

### 2.2 在 HTML 中使用变量

```html
<!-- 正确 ✓ -->
<div style="background: hsl(var(--card)); color: hsl(var(--foreground))">
  内容
</div>

<!-- 错误 ✗ -->
<div style="background: #1f2a44; color: #ffffff">
  内容
</div>
```

---

## 三、布局与间距

### 3.1 全局布局

```css
/* 页面容器 - 响应式最大宽度 */
.page-container {
  width: 100%;
  padding: 16px;
  box-sizing: border-box;
}

@media (min-width: 640px)  { .page-container { padding: 20px; } }
@media (min-width: 1024px) { .page-container { padding: 24px; max-width: 1400px; margin: 0 auto; } }
@media (min-width: 1280px) { .page-container { padding: 24px; max-width: 1500px; } }
@media (min-width: 1440px) { .page-container { max-width: 1700px; } }
@media (min-width: 1920px) { .page-container { max-width: 1800px; } }
```

### 3.2 卡片布局

```css
.card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 20px;
}

/* 内嵌卡片（带背景） */
.form-card,
.result-card,
.panel {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 20px;
  margin-bottom: 20px;
}
```

### 3.3 间距系统

```css
/* 间距变量 */
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-6: 24px;
--space-8: 32px;

/* 使用示例 */
.element {
  margin-bottom: var(--space-4);  /* 16px */
  padding: var(--space-6);        /* 24px */
}
```

---

## 四、字体系统

### 4.1 字体族

```css
/* 主字体族（按优先级） */
font-family: "Inter", "PingFang SC", "Hiragino Sans GB",
            "Microsoft YaHei", "Source Han Sans SC", "Noto Sans SC",
            system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
```

### 4.2 字号层级

```css
/* 标题 */
h1 { font-size: 28px; font-weight: 600; line-height: 1.2; }
h2 { font-size: 24px; font-weight: 600; line-height: 1.3; }
h3 { font-size: 20px; font-weight: 600; line-height: 1.4; }
h4 { font-size: 18px; font-weight: 500; }

/* 正文 */
body {
  font-size: 14px;
  line-height: 1.6;
}

/* 小字 */
small, .text-sm { font-size: 13px; }
.caption { font-size: 12px; }
```

---

## 五、组件样式

### 5.1 按钮

```css
/* 主按钮 */
.btn-primary {
  padding: 10px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: calc(var(--radius) - 4px); /* 6px */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 次要按钮 */
.btn-secondary {
  padding: 8px 14px;
  background: hsl(var(--secondary));
  color: hsl(var(--secondary-foreground));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  cursor: pointer;
}

/* 危险按钮 */
.btn-danger {
  padding: 8px 14px;
  background: hsl(var(--destructive));
  color: hsl(var(--destructive-foreground));
  border: none;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  cursor: pointer;
}

/* 图标按钮 */
.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: none;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  color: hsl(var(--muted-foreground));
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}
```

### 5.2 输入框

```css
/* 文本输入框 */
.input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-size: 15px;
  box-sizing: border-box;
  transition: border-color 0.2s ease;
}

.input:focus {
  outline: none;
  border-color: hsl(var(--primary));
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.2);
}

/* 文本域 */
textarea.input {
  resize: vertical;
  min-height: 100px;
}

/* 下拉框 */
select.input {
  cursor: pointer;
}
```

### 5.3 卡片

```css
/* 基础卡片 */
.card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 20px;
}

/* 带阴影的卡片 */
.card-elevated {
  box-shadow: var(--shadow-md);
}

/* 表格容器 */
.table-container {
  overflow: hidden;
  border-radius: var(--radius);
  border: 1px solid hsl(var(--border));
  box-shadow: var(--shadow-sm);
}
```

### 5.4 徽章 / 标签

```css
/* 状态徽章 */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 9999px;  /* 药丸形状 */
  font-size: 12px;
  font-weight: 500;
}

/* 成功 */
.badge-success {
  background: hsl(var(--success) / 0.2);
  color: hsl(var(--success));
}

/* 警告 */
.badge-warning {
  background: hsl(var(--warning) / 0.2);
  color: hsl(var(--warning));
}

/* 危险 */
.badge-danger {
  background: hsl(var(--destructive) / 0.2);
  color: hsl(var(--destructive));
}

/* 信息 */
.badge-info {
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
}
```

### 5.5 开关

```css
/* 切换开关 */
.toggle {
  position: relative;
  width: 44px;
  height: 24px;
  background: hsl(var(--muted));
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.toggle.active {
  background: hsl(var(--primary));
}

.toggle::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.toggle.active::after {
  transform: translateX(20px);
}
```

---

## 六、响应式断点

```css
/* 移动端 - 默认 */
body {
  font-size: 14px;
}

/* 平板 (640px+) */
@media (min-width: 640px) {
  body { font-size: 15px; }
}

/* 小屏桌面 (768px+) */
@media (min-width: 768px) {
  .container { max-width: 720px; }
}

/* 桌面 (1024px+) */
@media (min-width: 1024px) {
  .container { max-width: 960px; }
  .sidebar { display: block; }
}

/* 大屏 (1280px+) */
@media (min-width: 1280px) {
  .container { max-width: 1200px; }
}

/* 超大屏 (1440px+) */
@media (min-width: 1440px) {
  .container { max-width: 1400px; }
}

/* 极宽屏 (1920px+) */
@media (min-width: 1920px) {
  .container { max-width: 1600px; }
}
```

---

## 七、主题切换

### 7.1 控制方式

通过 `data-theme` 属性控制深色/浅色模式：

```javascript
// 设置深色主题
document.documentElement.setAttribute('data-theme', 'dark');

// 设置浅色主题
document.documentElement.setAttribute('data-theme', 'light');

// 获取当前主题
const currentTheme = document.documentElement.getAttribute('data-theme');
```

### 7.2 切换动画实现

主题切换采用覆盖层淡入淡出效果：

```javascript
// useTheme.ts 核心逻辑
function setMode(next, origin) {
  // 创建覆盖层
  const overlay = document.createElement('div');
  overlay.style.cssText = `
    position: fixed;
    inset: 0;
    z-index: 99999;
    pointer-events: none;
    opacity: 0;
    transition: opacity 0s;
  `;

  // 设置覆盖层颜色（目标主题色）
  overlay.style.background = next === 'dark'
    ? 'hsl(240, 10%, 4%)'   // 深色主题色
    : 'hsl(0, 0%, 100%)';    // 浅色主题色

  document.body.appendChild(overlay);

  // 强制重排
  overlay.offsetHeight;

  // 动画序列
  requestAnimationFrame(() => {
    // 1. 淡入覆盖层
    overlay.style.opacity = '1';

    // 2. 中途切换主题
    setTimeout(() => {
      document.documentElement.setAttribute('data-theme', next);
    }, 150);

    // 3. 淡出覆盖层，显示新主题
    setTimeout(() => {
      overlay.style.opacity = '0';
      overlay.style.transition = 'opacity 0.3s ease-out';
    }, 200);

    // 4. 移除覆盖层
    setTimeout(() => {
      overlay.remove();
    }, 500);
  });
}
```

### 7.3 CSS 变量版本（无 JavaScript）

```css
/* 纯 CSS 媒体查询自动切换 */
@media (prefers-color-scheme: dark) {
  :root {
    --background: 240 10% 4%;
    --foreground: 0 0% 98%;
    /* ... 其他变量 */
  }
}

/* 用户手动覆盖 */
[data-theme='dark'] {
  --background: 240 10% 4%;
  --foreground: 0 0% 98%;
}

[data-theme='light'] {
  --background: 0 0% 100%;
  --foreground: 240 10% 9%;
}
```

---

## 八、与主框架通信规范

### 8.1 Iframe 通信（postMessage）

插件通过 `window.parent.postMessage` 与主框架通信：

```javascript
// 发送消息
window.parent.postMessage({
  type: 'PLUGIN_READY',
  payload: {
    pluginId: 'my-plugin-id',
    version: '1.0.0',
    capabilities: ['theme', 'auth']
  }
}, '*');

// 监听来自主框架的消息
window.addEventListener('message', (event) => {
  const { type, payload } = event.data;

  switch (type) {
    case 'THEME_CHANGE':
      // 接收主题变更通知
      const newTheme = payload.theme; // 'dark' | 'light'
      document.documentElement.setAttribute('data-theme', newTheme);
      break;

    case 'AUTH_REQUIRED':
      // 处理需要认证的情况
      redirectToLogin();
      break;

    case 'PLUGIN_INVOKE':
      // 处理来自主框架的调用
      handleInvoke(payload);
      break;
  }
});
```

### 8.2 消息类型定义

```typescript
// 插件发送给主框架的消息
interface PluginMessage {
  type: 'PLUGIN_READY' | 'PLUGIN_INVOKE_RESULT' | 'PLUGIN_ERROR';
  payload: any;
}

// PLUGIN_READY - 插件加载完成
{
  type: 'PLUGIN_READY',
  payload: {
    pluginId: string;
    version: string;
    capabilities: string[];
  }
}

// PLUGIN_INVOKE_RESULT - 插件调用结果
{
  type: 'PLUGIN_INVOKE_RESULT',
  payload: {
    invokeId: string;
    success: boolean;
    result?: any;
    error?: string;
  }
}

// 主框架发送给插件的消息
interface FrameworkMessage {
  type: 'THEME_CHANGE' | 'PLUGIN_INVOKE' | 'AUTH_REQUIRED';
  payload: any;
}

// THEME_CHANGE - 主题变更
{
  type: 'THEME_CHANGE',
  payload: {
    theme: 'dark' | 'light';
  }
}

// PLUGIN_INVOKE - 主框架调用插件
{
  type: 'PLUGIN_INVOKE',
  payload: {
    invokeId: string;
    method: string;
    args: any[];
  }
}
```

---

## 九、插件样板代码

### 9.1 入口 HTML 模板

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>插件名称</title>

  <!-- 加载设计令牌（必须） -->
  <style>
    :root {
      --background: 240 10% 4%;
      --foreground: 0 0% 98%;
      --card: 240 8% 7%;
      --card-foreground: 0 0% 98%;
      --primary: 0 0% 98%;
      --primary-foreground: 240 10% 9%;
      --secondary: 240 5% 12%;
      --secondary-foreground: 0 0% 98%;
      --muted: 240 5% 12%;
      --muted-foreground: 240 5% 65%;
      --accent: 240 5% 14%;
      --accent-foreground: 0 0% 98%;
      --destructive: 0 70% 55%;
      --destructive-foreground: 0 0% 100%;
      --border: 240 5% 16%;
      --input: 240 5% 16%;
      --ring: 0 0% 90%;
      --radius: 0.625rem;
    }

    [data-theme='light'] {
      --background: 0 0% 100%;
      --foreground: 240 10% 9%;
      --card: 0 0% 100%;
      --card-foreground: 240 10% 9%;
      --primary: 240 10% 9%;
      --primary-foreground: 0 0% 100%;
      --secondary: 240 5% 96%;
      --secondary-foreground: 240 10% 9%;
      --muted: 240 5% 96%;
      --muted-foreground: 240 4% 60%;
      --accent: 240 5% 94%;
      --accent-foreground: 240 10% 9%;
      --destructive: 0 72% 51%;
      --destructive-foreground: 0 0% 100%;
      --border: 240 6% 90%;
      --input: 240 6% 90%;
      --ring: 240 10% 9%;
    }

    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      background: hsl(var(--background));
      color: hsl(var(--foreground));
      font-family: "Inter", "PingFang SC", "Microsoft YaHei", sans-serif;
      font-size: 14px;
      line-height: 1.6;
      min-height: 100vh;
    }
  </style>
</head>
<body>
  <div id="app"></div>

  <script>
    // 监听主题变更
    window.addEventListener('message', (event) => {
      if (event.data.type === 'THEME_CHANGE') {
        document.documentElement.setAttribute('data-theme', event.data.payload.theme);
      }
    });

    // 通知主框架插件已就绪
    window.parent.postMessage({
      type: 'PLUGIN_READY',
      payload: {
        pluginId: 'my-plugin',
        version: '1.0.0',
        capabilities: ['theme']
      }
    }, '*');
  </script>
</body>
</html>
```

### 9.2 React 组件模板

```tsx
import React, { useEffect, useState } from 'react';

// 主题 Hook
function useTheme() {
  const [theme, setTheme] = useState('dark');

  useEffect(() => {
    const handleMessage = (event) => {
      if (event.data.type === 'THEME_CHANGE') {
        setTheme(event.data.payload.theme);
        document.documentElement.setAttribute('data-theme', event.data.payload.theme);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  return theme;
}

// 示例组件
function PluginPage() {
  const theme = useTheme();

  return (
    <div style={{
      padding: '24px',
      background: 'hsl(var(--card))',
      borderRadius: 'var(--radius)',
      border: '1px solid hsl(var(--border))'
    }}>
      <h1 style={{
        fontSize: '24px',
        fontWeight: '600',
        color: 'hsl(var(--foreground))',
        marginBottom: '16px'
      }}>
        插件标题
      </h1>

      <button
        style={{
          padding: '10px 16px',
          background: 'hsl(var(--primary))',
          color: 'hsl(var(--primary-foreground))',
          border: 'none',
          borderRadius: 'calc(var(--radius) - 4px)',
          cursor: 'pointer'
        }}
        onClick={() => {
          window.parent.postMessage({
            type: 'PLUGIN_INVOKE_RESULT',
            payload: { success: true }
          }, '*');
        }}
      >
        提交
      </button>
    </div>
  );
}

export default PluginPage;
```

### 9.3 Vue 3 组件模板

```vue
<template>
  <div class="plugin-container">
    <h1>插件标题</h1>
    <button class="btn-primary" @click="handleSubmit">提交</button>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

// 监听主题变更
onMounted(() => {
  window.addEventListener('message', (event) => {
    if (event.data.type === 'THEME_CHANGE') {
      document.documentElement.setAttribute('data-theme', event.data.payload.theme)
    }
  })

  // 通知主框架插件已就绪
  window.parent.postMessage({
    type: 'PLUGIN_READY',
    payload: {
      pluginId: 'my-plugin',
      version: '1.0.0'
    }
  }, '*')
})

function handleSubmit() {
  window.parent.postMessage({
    type: 'PLUGIN_INVOKE_RESULT',
    payload: { success: true }
  }, '*')
}
</script>

<style scoped>
.plugin-container {
  padding: 24px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
}

h1 {
  font-size: 24px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 16px;
}

.btn-primary {
  padding: 10px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: calc(var(--radius) - 4px);
  cursor: pointer;
}
</style>
```

---

## 十、强制约束

### 10.1 禁止事项

1. **禁止硬编码色值**
   ```css
   /* 错误 ✗ */
   background: #1f2a44;
   color: #ffffff;

   /* 正确 ✓ */
   background: hsl(var(--background));
   color: hsl(var(--foreground));
   ```

2. **禁止覆盖全局变量**
   ```css
   /* 错误 ✗ */
   :root {
     --background: #1f2a44; /* 禁止重新定义 */
   }

   /* 正确 ✓ */
   /* 只能读取和使用，不能修改 */
   ```

3. **禁止忽略主题切换**
   ```javascript
   /* 错误 ✗ */
   document.body.style.background = '#1f2a44'; // 硬编码

   /* 正确 ✓ */
   document.body.style.background = 'hsl(var(--background))';
   ```

4. **禁止使用不在令牌中的颜色**
   - 所有 UI 颜色必须来自设计令牌
   - 如需新颜色，必须更新令牌并通知团队

### 10.2 必须遵守

1. **必须支持深色/浅色模式**
   - 所有组件必须能在这两种模式下正常显示
   - 测试时必须切换两种模式验证

2. **必须响应主题变更事件**
   - 监听 `THEME_CHANGE` 消息
   - 立即更新 DOM 元素的 `data-theme` 属性

3. **必须遵循响应式断点**
   - 移动端优先设计
   - 至少测试 3 个断点：<640px, 768px-1024px, >1024px

4. **必须声明插件能力**
   - 在 `PLUGIN_READY` 消息中声明 `capabilities`
   - 包括 `theme` 表示支持主题切换

---

## 十一、快速参考表

| 元素 | CSS 变量 | Tailwind 类 |
|------|----------|-------------|
| 主背景 | `hsl(var(--background))` | `bg-background` |
| 主文字 | `hsl(var(--foreground))` | `text-foreground` |
| 卡片背景 | `hsl(var(--card))` | `bg-card` |
| 主要按钮 | `hsl(var(--primary))` | `bg-primary` |
| 次要按钮 | `hsl(var(--secondary))` | `bg-secondary` |
| 边框 | `hsl(var(--border))` | `border-border` |
| 圆角 | `var(--radius)` | `rounded` |
| 成功色 | `hsl(var(--success))` | `bg-green-500` |
| 危险色 | `hsl(var(--destructive))` | `bg-destructive` |

---

## 十二、常见问题

**Q: 如何获取当前主题？**
```javascript
const theme = document.documentElement.getAttribute('data-theme') || 'dark';
```

**Q: 如何响应主题变更？**
```javascript
window.addEventListener('message', (e) => {
  if (e.data.type === 'THEME_CHANGE') {
    document.documentElement.setAttribute('data-theme', e.data.payload.theme);
  }
});
```

**Q: 如何在插件内触发主题切换？**
```javascript
// 发送消息给主框架
window.parent.postMessage({
  type: 'REQUEST_THEME_CHANGE',
  payload: { theme: 'light' }
}, '*');
```

---

> **最后更新**: 2026-05-22
> **文档维护**: 前端团队
> **版本**: 1.0.0