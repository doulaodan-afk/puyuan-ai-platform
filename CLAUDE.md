- [x] 创建 favicon.ico 文件（解决 404 错误）
- [x] 移除 adminAuth.ts 调试日志（验证后移除）
- [x] 更新 CLAUDE.md 记忆文件（2026-05-19）：
  - 记录联调结果、修复内容、完成状态
- [ ] 修复前端字段名不匹配问题（2026-05-19）：
  - 修复 AdminPluginsPage.vue 字段名：`plugin_id` → `pluginId`、`billing_type` → `billingType`
  - 修复 AdminPricingPage.vue 字段名：`token_price_per_1k` → `tokenPricePer1k`、`storage_price_per_gb_month` → `storagePricePerGbMonth` 等
  - 修复 AdminPricingPage.vue 保存后未重新加载：添加 `await loadConfig()` 调用
- [x] 创建 favicon.ico 文件（解决 404 错误）
- [x] 移除 adminAuth.ts 调试日志（验证后移除）
- [x] 更新 CLAUDE.md 记忆文件（2026-05-19）：
  - 记录联调结果、修复内容、完成状态
- [x] 修复前端字段名不匹配问题（2026-05-19）：
  - 修复 AdminPluginsPage.vue 字段名：`plugin_id` → `pluginId`、`billing_type` → `billingType`
  - 修复 AdminPricingPage.vue 字段名：`token_price_per_1k` → `tokenPricePer1k`、`storage_price_per_gb_month` → `storagePricePerGbMonth` 等
  - 修复 AdminPricingPage.vue 保存后未重新加载：添加 `await loadConfig()` 调用

---

## 前端 Element Plus 组件不显示问题（2026-05-22）

**问题现象**：使用 Element Plus 组件（如 el-tabs, el-form, el-button 等）的页面，组件内容不显示，但页面框架正常。

**根本原因**：`main.ts` 中没有导入 Element Plus 的 CSS 文件，导致组件样式丢失，内容不可见。

**解决方案**：在 `main.ts` 中添加：
```typescript
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
app.use(ElementPlus);
```

**涉及文件**：`frontend/merchant-web/src/main.ts`

---

## Tailwind CSS 依赖问题（2026-05-22）

**问题现象**：npm install 后 Tailwind CSS 相关依赖缺失，导致 `postcss.config.js` 无法加载。

**解决方案**：确保安装正确的依赖版本：
```bash
npm install element-plus @element-plus/icons-vue
npm install tailwindcss@3 postcss autoprefixer tailwindcss-animate
```

---

## UI 设计系统冻结规则

1. **严禁随意修改 UI**：除非用户明确要求"调整样式"、"修改布局"、"改变配色"等涉及界面的指令，否则不得改动任何前端 UI 代码（包括但不限于 CSS 文件、Vue 组件模板、样式绑定、深色模式变量、响应式断点等）。

2. **如需修改 UI 必须前置确认**：当判断可能需要改动 UI 时（例如新增功能必须添加按钮、调整布局才能容纳新内容），必须先向用户提出具体方案并获得同意，然后再实施。

3. **已固化的设计系统**：当前 UI 已按照 Linear/Vercel 风格、近黑墨色、HSL 变量、深色模式等规范完成定型。所有后续功能开发应尽量复用现有组件和样式，不破坏视觉一致性。

4. **例外情况**：修复导致功能错误的样式问题（如文字重叠、按钮不可见、响应式断裂）允许直接修改，但需在提交时注明修复原因。

5. **代码审查**：每次涉及 UI 的变更，Claude 在输出代码后应主动列出变更的文件和影响范围。
