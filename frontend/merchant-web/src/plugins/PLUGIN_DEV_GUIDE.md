# 插件开发指南

## 快速开始

### 创建新插件

```bat
cd frontend\merchant-web\src\plugins
create-plugin.bat my-plugin "我的插件"
```

脚本会从 `_template` 目录复制模板，自动替换占位符，生成完整的项目结构。

### 手动创建（如果脚本不可用）

1. 复制 `src/plugins/_template` 目录，重命名为你的 `plugin_id`（如 `my-plugin`）
2. 全局搜索替换 `{{PLUGIN_ID}}` → 你的插件ID，`{{PLUGIN_NAME}}` → 插件名称
3. 修改 `vite.config.ts` 中的端口（避免与其他插件冲突）

---

## 两种调试模式

### 模式 A：嵌入主框架调试（推荐）

插件在主框架中运行，共享登录态、全局样式和 API 代理，Vite HMR 自动热更新。

**步骤：**

1. 在 `.env` 中添加你的插件：
   ```
   VITE_DEV_PLUGINS=ai-design-assistant,my-plugin
   ```
   （逗号分隔，可同时加载多个插件）

2. 启动主框架：
   ```bat
   cd frontend\merchant-web
   npm run dev
   ```
   主框架运行在 `http://localhost:5173`

3. 在浏览器中访问 `http://localhost:5173/plugins/my-plugin/home`

**优点：**
- 真实登录态和权限控制
- 共享主框架的全局样式（design.css）
- API 自动代理到后端
- 修改插件代码后 Vite HMR 立即生效，无需刷新页面

**缺点：**
- 需要后端和数据库运行
- 需要真实登录账号

### 模式 B：独立调试

插件在自己的端口独立运行，使用 mock 数据，无需后端。

**步骤：**

1. 安装插件依赖：
   ```bat
   cd frontend\merchant-web\src\plugins\my-plugin
   npm install
   ```

2. 启动独立调试服务：
   ```bat
   npm run dev
   ```
   默认端口 `5181`（可在 `vite.config.ts` 中修改）

3. 在浏览器中访问 `http://localhost:5181/plugins/my-plugin/home`

**优点：**
- 无需后端、数据库
- 无需登录账号（使用 mock auth store）
- 完全隔离，不影响主框架
- 快速原型开发

**缺点：**
- 没有真实 API 数据
- 样式可能与主框架略有差异
- 权限控制是 mock 的

**Mock 认证：** 独立调试时 `@/stores/auth` 被重映射到 `dev/stores/auth.ts`（stub），默认角色为 `boss`。你可以修改 stub 中的 `roleCode` 来模拟不同角色：
```typescript
roleCode: "designer" as string | null,  // 模拟设计师角色
```

---

## 插件目录结构

```
my-plugin/
├── index.ts              # 插件入口（install 函数，注册路由到主框架）
├── manifest.json         # 插件元数据（上传时打包进 ZIP）
├── router/
│   └── index.ts          # 路由配置（所有页面路由）
├── pages/
│   ├── PluginLayout.vue  # 插件布局（导航栏 + 内容区）
│   └── home.vue          # 首页
│   └── ...               # 其他页面
├── api/
│   └── index.ts          # 插件专用 API 请求
├── stores/
│   └── index.ts          # Pinia Store
├── types/
│   └── index.ts          # TypeScript 类型定义
├── dev/                  # 独立调试目录（不影响主框架）
│   ├── index.html
│   ├── main.ts           # 独立调试入口
│   ├── App.vue
│   ├── design.css        # 样式（从主框架复制）
│   └── stores/
│       └── auth.ts       # Mock auth store
├── vite.config.ts        # 独立调试 Vite 配置
├── package.json          # 独立调试依赖
└── tsconfig.json         # TypeScript 配置
```

---

## 路由规则

所有插件路由必须使用 `/plugins/<plugin_id>/` 前缀：

```typescript
// router/index.ts
export const pluginRoutes: RouteRecordRaw[] = [
  {
    path: '/plugins/my-plugin',
    component: PluginLayout,
    children: [
      { path: '', redirect: '/plugins/my-plugin/home' },
      { path: 'home', name: 'MyPluginHome', component: HomePage },
      // ...更多页面
    ],
  },
]
```

**不要使用其他前缀**（如 `/my-plugin/`），否则在 PluginsPage 中点击插件卡片时会跳转到错误路径。

---

## 隔离原则

1. **路由隔离**：插件路由独立注册，前缀 `/plugins/<plugin_id>/`
2. **Store 隔离**：使用独立的 Pinia store（如 `useMyPluginStore`）
3. **API 隔离**：使用 `/api/plugins/<plugin_id>/` 前缀
4. **样式隔离**：所有组件使用 `<style scoped>`

插件**不应该**直接 import 主框架的页面、组件或 store（除了 `@/stores/auth` 用于获取当前角色）。

---

## 打包上传

调试完成后，将插件打包为 ZIP 上传到平台：

1. 确认 `manifest.json` 中的 `plugin_id`、`name`、`version` 正确
2. 打包 ZIP（解压后第一层直接是 manifest.json 等文件，不要嵌套目录）：
   ```bat
   cd frontend\merchant-web\src\plugins\my-plugin
   powershell Compress-Archive -Path manifest.json,index.ts,router,pages,api,stores,types -DestinationPath my-plugin.zip -Force
   ```
3. 通过管理端 API 或前端界面上传 ZIP

---

## 开发 → 上传工作流

```
┌─────────────────┐
│  create-plugin   │  创建脚手架
└────────┬────────┘
         ▼
┌─────────────────┐
│  模式B 独立调试   │  快速原型，mock 数据
│  (localhost:5181) │  不需要后端
└────────┬────────┘
         ▼
┌─────────────────┐
│  模式A 嵌入调试   │  真实环境验证
│  (localhost:5173) │  需要后端 + 登录
└────────┬────────┘
         ▼
┌─────────────────┐
│  打包 ZIP 上传    │  部署到生产环境
└─────────────────┘
```

建议先用模式 B 快速开发原型，再用模式 A 在真实环境中验证，最后打包上传。