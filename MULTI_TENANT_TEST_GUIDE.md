# 多租户功能测试指南

## 概述

本指南验证濮院毛衫 AI 平台的多租户功能，包括：
- 登录时工作室选择（多工作室用户）
- 插件内工作室切换
- 团队管理（仅老板角色）
- 老板首次进入引导

## 前提条件

### 数据库准备

确保数据库中有多租户测试数据：

```sql
-- 查看当前租户用户数据
SELECT tu.id, tu.tenant_id, t.name as tenant_name, tu.user_id, u.mobile, tu.role
FROM tenant_user tu
JOIN tenant t ON tu.tenant_id = t.id
JOIN user_account u ON tu.user_id = u.id
WHERE tu.status = 'active'
ORDER BY tu.tenant_id, tu.role;

-- 查看每个工作室的成员数量
SELECT t.id, t.name, COUNT(*) as member_count
FROM tenant t
LEFT JOIN tenant_user tu ON t.id = tu.tenant_id AND tu.status = 'active'
GROUP BY t.id, t.name;
```

### 测试账号

确保有以下测试账号：

1. **单工作室用户** - 只属于一个工作室
2. **多工作室用户** - 属于两个或更多工作室
3. **Boss 用户** - 某工作室的老板

---

## 测试场景

### 场景 1：单个工作室用户登录

**步骤：**
1. 访问 `http://localhost:5173/login`
2. 输入单工作室用户的手机号和验证码
3. 点击"登录"

**预期结果：**
- 登录成功后直接跳转到 `/dashboard`
- 不会弹出工作室选择对话框
- 顶部导航栏不显示工作室切换下拉菜单

**验证 SQL：**
```sql
SELECT COUNT(*) FROM tenant_user
WHERE user_id = [用户ID] AND status = 'active';
-- 结果应该为 1
```

---

### 场景 2：多个工作室用户登录

**步骤：**
1. 访问 `http://localhost:5173/login`
2. 输入多工作室用户的手机号和验证码
3. 点击"登录"
4. 在工作室选择对话框中选择一个工作室
5. 点击"进入工作室"

**预期结果：**
- 登录成功后弹出工作室选择对话框
- 对话框显示用户所属的所有工作室，包括角色信息
- 显示当前选中的工作室
- 选择工作室后跳转到 `/dashboard`
- LocalStorage 中保存了 `merchant_current_tenant_id` 和 `merchant_current_role`

**验证 SQL：**
```sql
SELECT COUNT(*) FROM tenant_user
WHERE user_id = [用户ID] AND status = 'active';
-- 结果应该 > 1
```

**浏览器验证：**
```javascript
// 在控制台执行
localStorage.getItem('merchant_current_tenant_id')
localStorage.getItem('merchant_current_role')
```

---

### 场景 3：插件内工作室切换

**步骤：**
1. 使用多工作室用户账号登录
2. 访问设计助手插件 `/design-assistant`
3. 在插件顶部导航栏找到工作室切换下拉菜单
4. 记录当前工作室名称
5. 点击下拉菜单，选择另一个工作室
6. 观察页面变化

**预期结果：**
- 插件顶部导航栏显示工作室切换下拉菜单（仅多工作室用户可见）
- 下拉菜单显示当前工作室和所有可用工作室
- 切换工作室后页面自动刷新
- LocalStorage 中 `merchant_current_tenant_id` 已更新
- 页面数据更新为新工作室的数据

**浏览器验证：**
```javascript
// 切换前记录
const oldTenantId = localStorage.getItem('merchant_current_tenant_id');

// 切换后验证
const newTenantId = localStorage.getItem('merchant_current_tenant_id');
console.log('旧工作室ID:', oldTenantId);
console.log('新工作室ID:', newTenantId);
console.log('已切换:', oldTenantId !== newTenantId);
```

**网络验证：**
打开浏览器开发者工具 Network 标签，检查 API 请求头中包含：
```
X-Tenant-Id: [所选工作室ID]
X-User-Id: [用户ID]
```

---

### 场景 4：团队设置页面权限验证

**步骤 4.1：Boss 角色访问团队设置**
1. 使用 Boss 账号登录
2. 访问 `/design-assistant`
3. 检查插件顶部菜单
4. 点击"团队设置"菜单项
5. 验证团队设置页面内容

**预期结果：**
- Boss 用户可以看到"团队设置"菜单项
- 菜单项图标为 👥
- 点击后成功跳转到 `/design-assistant/settings`
- 页面显示成员列表
- 页面显示"邀请成员"按钮

**步骤 4.2：非 Boss 角色访问团队设置**
1. 使用非 Boss 账号登录（如 designer）
2. 访问 `/design-assistant`
3. 检查插件顶部菜单
4. 尝试直接访问 `/design-assistant/settings`

**预期结果：**
- 非 Boss 用户看不到"团队设置"菜单项
- 直接访问 URL 时显示无权限或 403

---

### 场景 5：团队设置功能验证

**步骤 5.1：查看成员列表**
1. 使用 Boss 账号登录
2. 进入团队设置页面 `/design-assistant/settings`
3. 检查成员列表显示

**预期结果：**
- 显示当前工作室的所有成员
- 每行显示：姓名、手机号、角色、状态、邀请人、加入时间
- 第一行显示"当前用户"（不能移除自己）
- 角色下拉框对当前用户禁用

**API 验证：**
```bash
curl -X GET http://localhost:8080/api/tenant/members \
  -H "X-Tenant-Id: 2001" \
  -H "X-User-Id: 101" \
  -H "X-Request-Id: test-001"
```

---

**步骤 5.2：邀请成员**
1. 在团队设置页面点击"邀请成员"按钮
2. 在对话框中输入手机号（如 13900000001）
3. 选择角色（如：设计师）
4. 点击"发送邀请"

**预期结果：**
- 邀请成功后显示成功提示
- 对话框关闭
- 成员列表刷新，新成员出现在列表中
- 新成员角色为"设计师"
- 新成员状态为"活跃"
- 邀请人显示为当前用户昵称

**API 验证：**
```bash
curl -X POST http://localhost:8080/api/tenant/invite \
  -H "X-Tenant-Id: 2001" \
  -H "X-User-Id: 101" \
  -H "X-Request-Id: test-002" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13900000001","role":"designer"}'
```

**后端验证（站内信）：**
```sql
-- 检查是否发送了站内消息
SELECT * FROM station_message
WHERE user_id = [被邀请用户ID]
  AND message_type = 'task'
ORDER BY created_at DESC
LIMIT 5;
```

---

**步骤 5.3：修改成员角色**
1. 在成员列表中找到一个非 Boss、非自己的成员
2. 点击角色下拉框
3. 选择新角色（如：运营）
4. 观察页面变化

**预期结果：**
- 角色修改成功后显示成功提示
- 成员列表刷新，角色已更新
- 尝试修改 Boss 的角色应被拒绝
- 尝试修改自己的角色下拉框应禁用

**API 验证：**
```bash
curl -X PUT http://localhost:8080/api/tenant/members/102/role \
  -H "X-Tenant-Id: 2001" \
  -H "X-User-Id: 101" \
  -H "X-Request-Id: test-003" \
  -H "Content-Type: application/json" \
  -d '{"role":"operator"}'
```

---

**步骤 5.4：移除成员**
1. 在成员列表中找到一个非 Boss、非自己的成员
2. 点击该行的"移除"按钮
3. 在确认对话框中点击"确定"

**预期结果：**
- 显示确认删除对话框
- 删除成功后显示成功提示
- 成员列表刷新，该成员从列表中消失
- 尝试移除自己应显示"当前用户"（无移除按钮）
- 尝试移除唯一的 Boss 应被拒绝

**API 验证：**
```bash
curl -X DELETE http://localhost:8080/api/tenant/members/102 \
  -H "X-Tenant-Id: 2001" \
  -H "X-User-Id: 101" \
  -H "X-Request-Id: test-004"
```

---

### 场景 6：Boss 首次进入引导

**步骤：**
1. 创建一个只有老板一个成员的工作室
2. 使用该 Boss 账号登录
3. 访问设计助手插件 `/design-assistant`

**预期结果：**
- Boss 用户首次进入插件时
- 如果当前工作室只有 1 个成员（老板自己）
- 自动跳转到 `/design-assistant/settings`
- 显示引导提示，提示用户邀请成员

**数据库准备：**
```sql
-- 创建测试数据：只有一个成员的工作室
-- 查找某个只有 1 个成员的工作室
SELECT t.id, t.name, COUNT(*) as member_count
FROM tenant t
LEFT JOIN tenant_user tu ON t.id = tu.tenant_id AND tu.status = 'active'
GROUP BY t.id, t.name
HAVING COUNT(*) = 1;
```

---

## 错误场景验证

### 场景 7：邀请已存在的成员

**步骤：**
1. 使用 Boss 账号登录
2. 进入团队设置页面
3. 尝试邀请一个已经在工作室中的成员

**预期结果：**
- 显示错误提示："该成员已在工作室中"
- 邀请不会重复添加

---

### 场景 8：邀请时未填写必填项

**步骤：**
1. 在邀请对话框中不填写手机号或角色
2. 点击"发送邀请"

**预期结果：**
- 显示表单验证提示
- 不会发送请求

---

### 场景 9：API 请求缺少必需头

**步骤：**
```bash
curl -X GET http://localhost:8080/api/tenant/members \
  -H "X-Request-Id: test-005"
```

**预期结果：**
- 返回 400 或 401 错误
- 错误信息指出缺少 X-Tenant-Id 或 X-User-Id

---

### 场景 10：切换到不属于的工作室

**步骤：**
1. 使用某个工作室的账号登录
2. 尝试访问其他工作室的数据（通过修改请求头）

**预期结果：**
- TenantContextFilter 拦截请求
- 返回 403 错误
- 错误信息："您不在该工作室中或已被移除"

---

## 测试检查清单

### 登录流程
- [ ] 单工作室用户登录后直接进入首页
- [ ] 多工作室用户登录后弹出选择对话框
- [ ] 工作室选择对话框显示正确的工作室列表
- [ ] 选择工作室后正确设置当前租户上下文

### 工作室切换
- [ ] 插件内工作室切换下拉正常显示（多工作室用户）
- [ ] 切换后页面刷新
- [ ] 切换后所有 API 请求携带新的 X-Tenant-Id
- [ ] 切换后数据正确更新
- [ ] 单工作室用户不显示工作室切换下拉

### 团队管理
- [ ] Boss 角色可以看到团队设置菜单
- [ ] 非 Boss 角色看不到团队设置菜单
- [ ] 非 Boss 角色直接访问 URL 被拒绝
- [ ] 成员列表正确显示
- [ ] 邀请成员功能正常
- [ ] 修改角色功能正常
- [ ] 移除成员功能正常
- [ ] 邀请成员时发送站内消息

### 老板引导
- [ ] Boss 首次进入插件，只有自己时自动跳转到团队设置
- [ ] 再次进入插件不再跳转（已有成员）

### API 安全
- [ ] 所有 API 请求携带 X-Tenant-Id
- [ ] TenantContextFilter 正确验证租户上下文
- [ ] 非工作室成员访问数据被拒绝

---

## 已实现的后端接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取成员列表 | GET | /api/tenant/members | 获取当前工作室成员 |
| 邀请成员 | POST | /api/tenant/invite | 邀请新成员 |
| 修改角色 | PUT | /api/tenant/members/{userId}/role | 修改成员角色 |
| 移除成员 | DELETE | /api/tenant/members/{userId} | 移除成员 |
| 获取用户租户列表 | GET | /api/tenant/user/tenants | 获取用户所有工作室 |
| 切换租户 | POST | /api/tenant/switch | 切换工作室 |

---

## 已实现的前端文件

### 新增文件
- `src/views/design-assistant/PluginLayout.vue` - 插件独立布局
- `src/views/design-assistant/useMenuFilter.ts` - 菜单过滤逻辑
- `src/views/design-assistant/TeamSettingsPage.vue` - 团队设置页面

### 修改文件
- `src/App.vue` - 恢复为最小模板
- `src/router/routes.ts` - 添加插件嵌套路由
- `src/router/guards.ts` - 修复权限检查
- `src/utils/http.ts` - 修复请求头
- `src/pages/PluginsPage.vue` - 修复租户ID引用
- `src/stores/designAssistant.ts` - 修复消息设置bug

### 已存在文件（未修改）
- `src/stores/auth.ts` - 多租户状态管理
- `src/components/TenantSwitch.vue` - 工作室切换下拉
- `src/components/TenantSelectionDialog.vue` - 登录时租户选择

---

## 启动测试

### 1. 启动后端

```bash
cd D:\puyuanmaoshan\backend\java-spring
java -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 2. 启动前端

```bash
cd D:\puyuanmaoshan\frontend\merchant-web
npm run dev
```

### 3. 访问测试

打开浏览器访问：`http://localhost:5173`

---

## 已知限制

1. **设计助手 store 类型问题** - 部分页面引用 `../stores/design-assistant` 但实际文件是 `stores/designAssistant.ts`
2. **部分页面缺少类型定义** - 函数参数使用隐式 `any` 类型
3. **面料管理 API 不完整** - `updateFabric` 和 `deleteFabric` 函数未导出

这些问题属于预存在的问题，与多租户功能无关。

---

## 测试报告模板

测试完成后，请填写以下表格：

| 场景 | 测试结果 | 备注 |
|------|---------|------|
| 单工作室用户登录 | ☐ 通过 / ☐ 失败 | |
| 多工作室用户登录 | ☐ 通过 / ☐ 失败 | |
| 插件内工作室切换 | ☐ 通过 / ☐ 失败 | |
| 团队设置菜单可见性 | ☐ 通过 / ☐ 失败 | |
| 查看成员列表 | ☐ 通过 / ☐ 失败 | |
| 邀请成员 | ☐ 通过 / ☐ 失败 | |
| 修改成员角色 | ☐ 通过 / ☐ 失败 | |
| 移除成员 | ☐ 通过 / ☐ 失败 | |
| Boss 首次进入引导 | ☐ 通过 / ☐ 失败 | |
| API 请求头验证 | ☐ 通过 / ☐ 失败 | |
| 权限控制验证 | ☐ 通过 / ☐ 失败 | |