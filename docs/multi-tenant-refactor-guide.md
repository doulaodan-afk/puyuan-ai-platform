# 多租户人员模型重构 - 实施指南

## 概述

本文档说明了将「濮院毛衫AI平台」从单租户绑定模型重构为多租户人员模型的完整实施方案。

**核心变更：**
- 用户不再只能属于一个租户，可以同时加入多个工作室
- 老板可以邀请成员到工作室，并为每个成员分配独立角色
- 用户登录后可选择当前工作的工作室
- 菜单根据当前工作室下的角色动态显示

---

## 一、数据库变更

### 1.1 新建表：tenant_user

```sql
CREATE TABLE `tenant_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：boss/designer/design_assistant/pattern_maker',
  `invited_by` BIGINT DEFAULT NULL COMMENT '邀请人 user_id',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`)
);
```

### 1.2 迁移脚本

运行以下脚本完成数据迁移：

```bash
mysql -u root -p123456 puyuan_ai_mvp < backend/sql/migration_multi_tenant_user.sql
```

脚本功能：
1. 创建 `tenant_user` 表
2. 迁移现有用户数据到 `tenant_user` 表
3. 删除 `user_account` 表的 `tenant_role` 字段
4. 创建触发器自动更新 `tenant.member_count`
5. 创建视图方便查询

### 1.3 角色定义

| 角色代码 | 角色名称 | 说明 |
|---------|---------|------|
| boss | 老板 | 拥有所有权限，可邀请/移除成员 |
| designer | 设计师 | 可创建需求、查看任务 |
| design_assistant | 设计助理 | 可复核需求、发布任务 |
| pattern_maker | 版师 | 可处理打版任务 |
| operator | 运营 | 可处理日常运营事务 |
| viewer | 查看者 | 仅查看权限 |

---

## 二、后端变更

### 2.1 新增实体类

**文件：** `TenantUser.java`
- 位置：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/entity/TenantUser.java`
- 包含角色枚举和状态枚举
- 提供便捷方法：`isBoss()`, `hasPermission()`, `isActive()`

### 2.2 新增 Mapper

**文件：** `TenantUserMapper.java`
- 位置：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/mapper/TenantUserMapper.java`
- 提供租户用户查询方法

### 2.3 新增 DTOs

**文件：** `TenantDtos.java`
- 位置：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/TenantDtos.java`
- 包含：LoginResponse, UserTenant, MemberInfo, CommonResponse

### 2.4 新增服务

**TenantMemberService** + **TenantMemberServiceImpl**
- 位置：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/TenantMemberService.java`

主要方法：
```java
// 获取租户成员列表
List<MemberInfo> getTenantMembers(Long tenantId);

// 邀请成员
CommonResponse inviteMember(Long tenantId, Long inviterId, String mobile, String role);

// 修改成员角色
CommonResponse updateMemberRole(Long tenantId, Long operatorId, Long targetUserId, String newRole);

// 移除成员
CommonResponse removeMember(Long tenantId, Long operatorId, Long targetUserId);

// 获取用户所属的所有租户
List<UserTenant> getUserTenants(Long userId);

// 验证权限
boolean hasPermission(Long userId, Long tenantId, String requiredRole);
```

### 2.5 新增 Controller

**TenantMemberController**
- 位置：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/controller/TenantMemberController.java`

API 端点：
```
GET    /api/tenant/members              - 获取成员列表
POST   /api/tenant/invite               - 邀请成员
PUT    /api/tenant/members/{userId}/role - 修改角色
DELETE /api/tenant/members/{userId}       - 移除成员
GET    /api/tenant/user/tenants          - 获取用户租户列表
POST   /api/tenant/switch                - 切换租户
```

### 2.6 认证接口修改

**AuthController** (`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/controller/AuthController.java`)

登录响应变更：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accessToken": "token-123",
    "expiresIn": 7200,
    "userId": 123,
    "mobile": "13800138000",
    "nickname": "张三",
    "tenants": [
      {
        "tenantId": 2001,
        "tenantName": "工作室A",
        "tenantCode": "STUDIO_A",
        "role": "boss",
        "isDefault": true
      },
      {
        "tenantId": 3001,
        "tenantName": "工作室B",
        "tenantCode": "STUDIO_B",
        "role": "designer",
        "isDefault": false
      }
    ]
  }
}
```

### 2.7 租户上下文过滤器

**TenantContextFilter**
- 位置：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/filter/TenantContextFilter.java`
- 验证 `X-Tenant-Id` 和 `X-User-Id` Header
- 验证用户是否在租户中有权限
- 自动跳过登录、注册等路径

### 2.8 工具类增强

**RequestContextUtil** 新增方法：
```java
public static long parseUserId(String userIdHeader);
```

---

## 三、前端变更

### 3.1 Auth Store 重构

**文件：** `frontend/merchant-web/src/stores/auth.ts`

新增状态：
```typescript
{
  userId: number,
  mobile: string,
  nickname: string,
  currentTenantId: number,
  currentRole: string,
  tenants: UserTenant[],
}
```

新增方法：
```typescript
switchTenant(tenantId: number)    // 切换工作室
getMembers()                        // 获取成员列表
inviteMember(mobile, role)          // 邀请成员
updateMemberRole(userId, role)      // 修改角色
removeMember(userId)                // 移除成员
getHeaders()                         // 获取请求头（含租户ID）
```

### 3.2 租户选择对话框

**文件：** `frontend/merchant-web/src/components/TenantSelectionDialog.vue`

功能：
- 登录后自动弹出（当用户属于多个工作室时）
- 显示所有工作室列表
- 单个工作室时自动进入

### 3.3 工作室切换组件

**文件：** `frontend/merchant-web/src/components/TenantSwitch.vue`

功能：
- 显示在导航栏
- 下拉切换工作室
- 切换后刷新当前页面

### 3.4 菜单过滤组合式函数

**文件：** `frontend/merchant-web/src/composables/useMenuFilter.ts`

功能：
- 定义所有菜单项及所需角色
- 根据当前角色过滤菜单
- 提供权限检查方法

### 3.5 团队设置页面

**文件：** `frontend/merchant-web/src/pages/TeamSettingsPage.vue`

功能：
- 仅老板可访问
- 显示成员列表
- 邀请新成员
- 修改成员角色
- 移除成员

### 3.6 路由配置

在 `routes.ts` 中新增：
```typescript
{
  path: "/design-assistant/settings",
  name: "TeamSettings",
  component: () => import("../pages/TeamSettingsPage.vue"),
  meta: {
    title: "团队设置",
    requiresAuth: true,
    roles: ["merchant_owner"],
  },
}
```

---

## 四、API 使用示例

### 4.1 登录

```typescript
// POST /api/v1/auth/login
const response = await fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    mobile: '13800138000',
    verifyCode: '123456'
  })
})
const data = await response.json()
// data.data.tenants 包含用户所属的所有工作室
```

### 4.2 切换工作室

```typescript
// POST /api/tenant/switch
const response = await fetch('/api/tenant/switch', {
  method: 'POST',
  headers: authStore.getHeaders(),
  body: JSON.stringify({ tenantId: 3001 })
})
// 前端直接更新 X-Tenant-Id header，无需重新登录
```

### 4.3 邀请成员

```typescript
// POST /api/tenant/invite
const result = await authStore.inviteMember('13800138001', 'designer')
if (result.success) {
  console.log('邀请成功')
}
```

---

## 五、部署步骤

### 5.1 后端部署

1. 编译后端：
```bash
cd backend/java-spring
mvn clean package
```

2. 运行迁移脚本：
```bash
mysql -u root -p puyuan_ai_mvp < backend/sql/migration_multi_tenant_user.sql
```

3. 启动后端：
```bash
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 5.2 前端部署

1. 构建前端：
```bash
cd frontend/merchant-web
npm run build
```

2. 部署到 Nginx 或静态服务器

### 5.3 验证

1. 使用旧账号登录，验证能自动进入工作室
2. 创建新用户，验证无工作室时提示联系老板
3. 老板登录后，进入团队设置页面邀请成员
4. 成员登录后，验证能看到对应工作室
5. 测试切换工作室功能

---

## 六、常见问题

### Q1: 迁移后老用户无法登录？

**A:** 检查迁移脚本是否成功执行。运行以下SQL验证：
```sql
SELECT COUNT(*) FROM tenant_user;
```

如果结果为0，需要手动迁移数据。

### Q2: 切换工作室后数据没更新？

**A:** 确保所有API请求都携带了正确的 `X-Tenant-Id` header。使用 `authStore.getHeaders()` 获取 headers。

### Q3: 老板能看到所有成员的Token余额吗？

**A:** 当前实现不包含余额查询，如有需求可以在成员列表中增加余额显示。

### Q4: 移除成员后，该成员的历史数据怎么处理？

**A:** 当前实现为软删除（status = 'inactive'），历史数据保留。如需完全删除，可另外开发数据归档功能。

---

## 七、后续优化方向

1. **成员邀请流程优化**
   - 支持短信邀请链接
   - 支持通过微信分享邀请

2. **权限细粒度控制**
   - 增加更细的权限点
   - 支持自定义权限组合

3. **数据隔离优化**
   - 每个租户独立的数据空间
   - 跨租户数据访问审计

4. **团队统计**
   - 成员活跃度统计
   - 任务完成率统计

---

*文档版本：1.0*
*最后更新：2026-05-19*