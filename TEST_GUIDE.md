# 个人中心和成员管理功能测试指南

## 前提条件
确保后端服务运行在 `http://localhost:8080`，前端运行在 `http://localhost:5173`

## 1. 登录测试
访问 `http://localhost:5173/login`
- 手机号：`13800000001`
- 验证码：`123456`
- 点击"登录"

## 2. 个人中心测试
登录后，点击顶部导航的用户图标，进入"个人中心"

### 验证内容：
- [ ] 左侧显示用户头像（默认头像）
- [ ] 显示用户昵称"商家老板A"
- [ ] 显示用户角色
- [ ] 微信绑定状态显示"未绑定"
- [ ] 手机绑定状态显示"已绑定"
- [ ] 右侧表单可编辑昵称和邮箱

### 测试编辑：
1. 修改昵称为测试名称
2. 填写邮箱（如 test@example.com）
3. 点击"保存"按钮
4. 验证页面更新

## 3. 成员管理测试
在导航栏中点击"成员管理"菜单

### 验证内容：
- [ ] 显示成员列表（包含6个成员）
- [ ] 每个成员显示头像、昵称、角色标签
- [ ] 显示启用/禁用状态
- [ ] 分页组件正常

### 测试添加成员：
1. 点击"添加成员"按钮
2. 输入手机号：`13900000099`
3. 选择角色（tenant_operator）
4. 点击"确定"
5. 验证成员列表更新

### 测试修改角色：
1. 在成员列表中找到任意成员
2. 点击"修改角色"按钮
3. 选择不同角色
4. 点击"确定"
5. 验证角色标签更新

### 测试启用/禁用：
1. 找到任意成员
2. 点击"禁用"按钮
3. 确认操作
4. 验证状态变为"禁用"
5. 再次点击"启用"
6. 验证状态恢复

### 测试移除成员：
1. 添加测试成员（如13900000099）
2. 点击"移除"按钮
3. 确认操作
4. 验证成员从列表移除

## 4. API 验证命令

### 4.1 登录并获取 Token
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: test123" \
  -d '{"mobile":"13800000001","verify_code":"123456"}'
```

### 4.2 获取个人信息
```bash
curl -s http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 2001" \
  -H "X-Request-Id: test123"
```

### 4.3 获取成员列表
```bash
curl -s "http://localhost:8080/api/tenant/members/v2?page=1&pageSize=10" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 2001" \
  -H "X-Request-Id: test123"
```

### 4.4 获取角色列表
```bash
curl -s http://localhost:8080/api/tenant/roles \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 2001" \
  -H "X-Request-Id: test123"
```

## 5. 常见问题排查

### 5.1 页面显示空白
1. 检查浏览器控制台是否有错误
2. 确认已登录（刷新页面应该仍在登录状态）
3. 确认 token 和 tenantId 正确

### 5.2 API 请求失败
1. 检查后端服务是否正常运行
2. 检查浏览器 Network 面板中的请求详情
3. 确认请求头包含 Authorization 和 X-Tenant-Id

### 5.3 功能按钮无响应
1. 检查是否缺少必要的角色权限
2. boss/merchant_owner/tenant_admin 角色可以使用成员管理功能

## 6. 相关文件
- 后端 Controller: `backend/.../controller/UserController.java`
- 后端 Controller: `backend/.../controller/TenantMemberController.java`
- 前端页面: `frontend/merchant-web/src/pages/ProfilePage.vue`
- 前端页面: `frontend/merchant-web/src/pages/MembersPage.vue`
- 前端路由: `frontend/merchant-web/src/router/routes.ts`
- 前端认证: `frontend/merchant-web/src/stores/auth.ts`