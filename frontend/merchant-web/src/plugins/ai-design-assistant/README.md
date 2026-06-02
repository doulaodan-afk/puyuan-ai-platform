# AI 设计助手插件

独立插件结构，用于将 AI 设计助手功能与主框架解耦。

## 目录结构

```
ai-design-assistant/
├── index.ts          # 插件入口
├── api/              # API 请求（插件专用）
│   └── index.ts
├── components/       # 插件内部组件
├── pages/            # 所有页面组件
│   ├── PluginLayout.vue    # 插件布局（顶部导航）
│   ├── create.vue          # 创建设计需求
│   ├── requirement-list.vue # 我的设计需求列表
│   ├── pending-list.vue    # 设计助理待办
│   ├── assistant-detail.vue # 需求复核与任务编辑
│   ├── my-tasks.vue        # 我的任务
│   ├── board.vue           # 任务看板
│   ├── fabric-manage.vue   # 面料库管理
│   ├── message-list.vue     # 消息中心
│   ├── team-settings.vue   # 成员管理
│   └── partner-manage.vue   # 合作方管理
├── router/           # 路由配置
│   └── index.ts
├── stores/           # Pinia Store
│   └── index.ts
├── types/            # TypeScript 类型定义
│   └── index.ts
└── README.md         # 本文档
```

## 路由前缀

所有插件路由使用 `/plugins/ai-design-assistant/` 前缀：

- `/plugins/ai-design-assistant/list` - 我的设计需求
- `/plugins/ai-design-assistant/create` - 创建设计需求
- `/plugins/ai-design-assistant/pending` - 设计助理待办
- `/plugins/ai-design-assistant/detail/:id` - 需求复核与任务编辑
- `/plugins/ai-design-assistant/tasks` - 我的任务
- `/plugins/ai-design-assistant/board` - 任务看板
- `/plugins/ai-design-assistant/fabrics` - 面料库管理
- `/plugins/ai-design-assistant/messages` - 消息中心
- `/plugins/ai-design-assistant/settings` - 成员管理（仅 boss）
- `/plugins/ai-design-assistant/partners` - 合作方管理（仅 boss）

## 主框架集成

### 启用插件

在 `.env` 文件中设置：

```
VITE_ENABLE_AI_DESIGN_ASSISTANT=true
```

### 手动安装（如需禁用）

在 `main.ts` 中注释掉插件加载代码：

```typescript
// if (import.meta.env.VITE_ENABLE_AI_DESIGN_ASSISTANT === 'true') {
//   loadPlugin(app, pinia, router, 'ai-design-assistant', '@/plugins/ai-design-assistant')
// }
```

## 隔离原则

1. **路由隔离**：插件路由独立注册，不影响主框架路由
2. **Store 隔离**：使用独立的 Pinia store (`useDesignAssistantStore`)
3. **API 隔离**：使用 `/api/plugins/ai-design-assistant/*` 前缀
4. **样式隔离**：所有组件使用 `<style scoped>` 防止样式泄漏

## 开发说明

### 新增页面

1. 在 `pages/` 目录下创建新页面组件
2. 在 `router/index.ts` 中添加路由配置
3. 更新 `index.ts` 中的导出

### API 路径

插件 API 统一使用 `/api/plugins/ai-design-assistant/*` 前缀，通过 API 网关路由到后端服务。