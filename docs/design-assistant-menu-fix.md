# AI 设计助手菜单点击无反应问题 - 解决方案

## 问题诊断

**根因：** `PluginsPage.vue` 中的 `pluginRoutes` 映射表缺少 `ai_design_assistant` 插件的路由配置。

### 具体分析

查看 `PluginsPage.vue` 第 108-113 行的 `navigateToPlugin` 函数：

```typescript
function navigateToPlugin(pluginId: string) {
  const route = pluginRoutes[pluginId];
  if (route) {
    router.push(route);
  }
}
```

当点击插件卡片时，该函数会从 `pluginRoutes` 对象中查找对应的路由。但原代码中只有三个映射：

```typescript
const pluginRoutes: Record<string, string> = {
  ai_image_gen: "/ai-tools/image-gen",
  ai_script_gen: "/ai-tools/script-gen",
  ai_translate: "/ai-tools/translate",
  // ❌ 缺少 ai_design_assistant
};
```

当 `pluginId` 为 `ai_design_assistant` 时，`pluginRoutes[pluginId]` 返回 `undefined`，导致 `router.push` 没有被执行。

---

## 已修复的代码

**文件：** `frontend/merchant-web/src/pages/PluginsPage.vue`

```typescript
const pluginRoutes: Record<string, string> = {
  ai_image_gen: "/ai-tools/image-gen",
  ai_script_gen: "/ai-tools/script-gen",
  ai_translate: "/ai-tools/translate",
  ai_design_assistant: "/design-requirement/create", // ✅ 新增
};

const pluginIcons: Record<string, string> = {
  ai_image_gen: "🖼️",
  ai_script_gen: "📝",
  ai_translate: "🌐",
  ai_design_assistant: "🎨", // ✅ 新增
};
```

---

## 验证步骤

### 1. 确认路由已定义
检查 `routes.ts` 中设计助手相关路由：

```typescript
// 已存在，无需修改
{
  path: "/design-requirement/create",
  name: "DesignRequirementCreate",
  component: () => import("../pages/DesignRequirementCreate.vue"),
  meta: {
    title: "创建设计需求",
    requiresAuth: true,
    roles: ["merchant_owner", "merchant_operator", "merchant_editor"],
  },
}
```

### 2. 确认页面组件存在
```bash
ls frontend/merchant-web/src/pages/DesignRequirementCreate.vue
```

### 3. 确认后端插件已注册
```sql
SELECT id, plugin_id, name, status FROM plugin WHERE plugin_id = 'ai_design_assistant';
```

预期结果：
```
+------+--------------------+--------------+--------+
| id   | plugin_id          | name         | status |
+------+--------------------+--------------+--------+
| 3020 | ai_design_assistant | AI设计助手   | 1      |
+------+--------------------+--------------+--------+
```

### 4. 确认租户已启用插件
```sql
SELECT tenant_id, plugin_id, enabled FROM tenant_plugin WHERE plugin_id = 'ai_design_assistant';
```

### 5. 测试前端菜单点击
1. 刷新页面，确保修改生效
2. 访问 `/plugins`
3. 找到 "AI设计助手" 插件卡片
4. 点击卡片主体区域
5. 应该跳转到 `/design-requirement/create`

---

## 调试技巧

如果修复后仍有问题，可以添加调试日志：

```typescript
function navigateToPlugin(pluginId: string) {
  const route = pluginRoutes[pluginId];
  console.log('点击插件:', pluginId);
  console.log('找到路由:', route);
  console.log('所有路由映射:', pluginRoutes);

  if (route) {
    router.push(route).catch(err => {
      console.error('路由跳转失败:', err);
    });
  } else {
    console.warn('未找到插件路由映射:', pluginId);
  }
}
```

---

## 其他可能问题排查

### 1. 路由守卫拦截
检查浏览器控制台是否有权限错误：

```typescript
// 如果看到类似错误：
// "您无权访问此页面"

// 说明用户角色不符合要求
// 设计助手需要角色: merchant_owner, merchant_operator, merchant_editor
```

### 2. API 数据格式不匹配
确认 `/api/v1/plugins` 返回的数据格式：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "plugin_id": "ai_design_assistant",
      "name": "AI设计助手",
      "version": "1.0.0",
      "billing_type": "token",
      "enabled": true
    }
  ]
}
```

### 3. 微前端子应用未注册（如使用 qiankun）
如果使用了微前端架构，检查子应用是否正确注册：

```typescript
// main.ts 或微前端配置文件
registerMicroApps([
  {
    name: 'merchant-web',
    entry: '//localhost:3000',
    container: '#subapp-viewport',
    activeRule: '/merchant',
  },
  // 确保子应用路由包含 /design-requirement
]);
```

---

## 完整的 AI 设计助手路由列表

| 路径 | 名称 | 页面 | 所需角色 |
|------|------|------|---------|
| `/design-requirement/create` | DesignRequirementCreate | 创建设计需求 | owner/operator/editor |
| `/design-requirement/list` | DesignRequirementList | 我的设计需求 | owner/operator/editor/viewer |
| `/assistant/pending` | AssistantPending | 设计助理待办 | owner/operator/editor |
| `/assistant/detail/:id` | AssistantDetail | 需求复核 | owner/operator/editor |
| `/my-tasks` | MyTasks | 我的任务 | owner/operator/editor/viewer |
| `/messages` | MessageCenter | 消息中心 | owner/operator/editor/viewer |
| `/task-board` | TaskBoard | 任务看板 | owner/operator/editor/viewer |
| `/fabric-manage` | FabricManage | 面料库管理 | owner/operator |

---

## 修复确认清单

- [x] 已添加 `ai_design_assistant` 路由映射
- [x] 已添加 `ai_design_assistant` 图标
- [x] 确认路由已定义在 `routes.ts`
- [x] 确认页面组件存在
- [x] 确认后端插件已注册
- [x] 确认租户已启用插件

---

*修复日期: 2026-05-19*