# 面料商模块集成测试文档

## 概述

本文档描述了面料商相关功能的集成测试计划，包括后端 API、管理端、租户端和面料商端的测试用例。

## 测试环境

- **后端 API**: http://localhost:8080
- **管理端**: http://localhost:5174
- **租户端**: http://localhost:5173
- **数据库**: puyuan_ai_mvp (MySQL)

## 测试账号

### 平台管理员（管理端）
- **手机号**: 13800000001
- **验证码**: 123456
- **角色**: platform_super_admin
- **权限**: 所有管理功能

### 工作室老板（租户端）
- **手机号**: 13900000001
- **验证码**: 123456
- **角色**: boss
- **租户**: 濮院毛衫工作室 (ID: 2001)
- **权限**: 租户管理、邀请面料商合作

### 面料商（租户端，supplier 类型）
- **手机号**: 13700000001
- **验证码**: 123456
- **角色**: boss
- **租户**: 杭州纺织城 (ID: 3001)
- **权限**: 查看合作邀请、接受/拒绝合作

## 测试流程

### 1. 面料商入驻申请流程

#### 1.1 面料商提交入驻申请
1. 访问 http://localhost:5173/register-supplier
2. 填写表单：
   - 公司名称: 测试面料商
   - 联系人姓名: 张三
   - 联系人手机: 13700000002
   - 公司地址: 杭州市余杭区
   - 面料品类: 真丝、羊毛
   - 公司介绍: 专注高端面料
   - 营业执照: 上传测试图片
3. 点击"提交申请"
4. 预期：显示"入驻申请提交成功，请等待审核"，跳转到登录页

#### 1.2 管理员审核入驻申请
1. 使用平台管理员账号登录管理端 http://localhost:5174/admin/login
2. 点击"面料商"菜单
3. 查看待审核申请列表
4. 点击"审核"按钮
5. 查看申请详情
6. 选择"通过"
7. 点击"确认"
8. 预期：显示"审核成功"，自动创建租户和用户账号
9. 使用 13700000002 / 123456 登录租户端，验证账号已创建

### 2. 工作室邀请面料商合作流程

#### 2.1 工作室查看可合作供应商
1. 使用工作室老板账号登录租户端 http://localhost:5173/login
2. 导航到 "设计助手" -> "合作方管理"
3. 切换到"可合作供应商"标签
4. 预期：显示所有已入驻但未合作的供应商列表

#### 2.2 工作室发送合作邀请
1. 在"可合作供应商"列表中找到目标供应商
2. 点击"邀请合作"按钮
3. 预期：显示"邀请已发送"，供应商从列表中移除

#### 2.3 工作室查看合作历史
1. 切换到"我的合作"标签
2. 预期：显示刚发送的合作邀请，状态为"待确认"

### 3. 面料商响应合作邀请流程

#### 3.1 面料商查看待处理邀请
1. 使用面料商账号登录租户端 http://localhost:5173/login
2. 导航到 "设计助手" -> "合作方管理"
3. 切换到"我的合作"标签
4. 预期：显示待处理合作邀请列表

#### 3.2 面料商接受合作
1. 找到目标合作邀请
2. 点击"接受"按钮
3. 预期：显示"操作成功"，状态变为"已接受"

#### 3.3 面料商拒绝合作（可选）
1. 找到目标合作邀请
2. 点击"拒绝"按钮
3. 预期：显示"操作成功"，状态变为"已拒绝"

### 4. 创建设计需求并选择面料商

#### 4.1 创建设计需求
1. 使用工作室账号登录租户端
2. 导航到 "设计助手" -> "创建设计需求"
3. 在顶部选择面料商下拉框中，选择已合作的面料商
4. 输入设计需求描述
5. 发送消息
6. 预期：需求创建成功，关联选定的面料商

### 5. 工作室屏蔽供应商流程

#### 5.1 屏蔽已合作的供应商
1. 使用工作室账号登录租户端
2. 导航到 "设计助手" -> "合作方管理"
3. 切换到"我的合作"标签
4. 找到状态为"已接受"的供应商
5. 点击"屏蔽"按钮
6. 输入屏蔽原因
7. 点击"确认屏蔽"
8. 预期：显示"已屏蔽该供应商"，状态变为"已屏蔽"

## API 测试用例

### 1. 面料商入驻申请 API

```bash
# 提交入驻申请
curl -X POST http://localhost:8080/api/supplier/register \
  -H "Content-Type: application/json" \
  -d '{
    "company_name": "测试面料商",
    "contact_name": "张三",
    "contact_mobile": "13700000003",
    "business_license": "https://example.com/license.jpg",
    "address": "杭州市余杭区",
    "fabric_categories": ["真丝", "羊毛"],
    "description": "专注高端面料"
  }'

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "registration_id": 1,
    "status": "pending"
  }
}
```

### 2. 管理员获取入驻申请列表 API

```bash
# 获取入驻申请列表（需要管理员认证）
curl -X GET "http://localhost:8080/api/admin/supplier/registrations?page=1&size=20" \
  -H "Authorization: Bearer <admin_token>"

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "registrations": [
      {
        "id": 1,
        "company_name": "测试面料商",
        "contact_name": "张三",
        "contact_mobile": "13700000003",
        "fabric_categories": ["真丝", "羊毛"],
        "status": "pending",
        "created_at": "2026-05-20T10:00:00"
      }
    ],
    "total": 1
  }
}
```

### 3. 管理员审核入驻申请 API

```bash
# 审核通过
curl -X POST http://localhost:8080/api/admin/supplier/review/1 \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "approve"
  }'

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "tenant_id": 3002,
    "user_id": 10002,
    "message": "审核通过，已自动创建账号"
  }
}

# 审核驳回
curl -X POST http://localhost:8080/api/admin/supplier/review/1 \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "reject",
    "reject_reason": "信息不完整"
  }'
```

### 4. 获取可合作供应商列表 API

```bash
# 获取可合作供应商列表
curl -X GET "http://localhost:8080/api/supplier/available?page=1&size=20" \
  -H "X-Tenant-Id: 2001"

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "suppliers": [
      {
        "tenant_id": 3001,
        "tenant_name": "杭州纺织城",
        "tenant_code": "SUP-1234567890",
        "fabric_categories": [],
        "created_at": "2026-05-20T09:00:00"
      }
    ],
    "total": 1
  }
}
```

### 5. 邀请面料商合作 API

```bash
# 发送合作邀请
curl -X POST http://localhost:8080/api/supplier/collaboration/invite \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 2001" \
  -H "X-User-Id: 9001" \
  -d '{
    "supplier_tenant_id": 3001
  }'

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "success": true,
    "message": "合作邀请已发送"
  }
}
```

### 6. 获取合作列表 API

```bash
# 获取合作列表
curl -X GET "http://localhost:8080/api/supplier/collaboration/list?status=pending&page=1&size=20" \
  -H "X-Tenant-Id: 2001"

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "collaborations": [
      {
        "id": 1,
        "merchant_tenant_id": 2001,
        "supplier_tenant_id": 3001,
        "supplier_name": "杭州纺织城",
        "status": "pending",
        "invited_by": 9001,
        "inviter_name": "工作室老板",
        "responded_by": null,
        "responder_name": null,
        "responded_at": null,
        "created_at": "2026-05-20T11:00:00"
      }
    ],
    "total": 1
  }
}
```

### 7. 响应合作邀请 API

```bash
# 接受合作
curl -X PUT http://localhost:8080/api/supplier/collaboration/respond/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10001" \
  -d '{
    "action": "accept"
  }'

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "success": true,
    "message": "已响应合作邀请"
  }
}

# 拒绝合作
curl -X PUT http://localhost:8080/api/supplier/collaboration/respond/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10001" \
  -d '{
    "action": "reject",
    "reason": "暂无合作意向"
  }'
```

### 8. 屏蔽供应商 API

```bash
# 屏蔽供应商
curl -X PUT http://localhost:8080/api/supplier/collaboration/block/1 \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 2001" \
  -H "X-User-Id: 9001" \
  -d '{
    "reason": "服务不满足需求"
  }'

# 预期响应
{
  "code": 0,
  "message": "success",
  "data": {
    "success": true,
    "message": "已屏蔽该供应商"
  }
}
```

## 数据库验证

### supplier_registration 表
```sql
-- 查看入驻申请记录
SELECT * FROM supplier_registration ORDER BY created_at DESC;

-- 验证已审核通过记录
SELECT id, company_name, contact_mobile, status, tenant_id, user_id
FROM supplier_registration
WHERE status = 'approved';
```

### supplier_collaboration 表
```sql
-- 查看合作记录
SELECT * FROM supplier_collaboration ORDER BY created_at DESC;

-- 验证合作状态分布
SELECT status, COUNT(*) as count
FROM supplier_collaboration
GROUP BY status;
```

### tenant 表
```sql
-- 验证面料商租户
SELECT tenant_id, tenant_code, name, tenant_type, status
FROM tenant
WHERE tenant_type = 'supplier';
```

## 边界情况测试

1. **重复邀请**: 尝试邀请已合作的供应商
   - 预期：返回错误提示"已存在合作记录"

2. **重复申请**: 使用相同手机号提交多次入驻申请
   - 预期：第二次申请返回错误提示"该手机号已入驻"

3. **权限验证**: 使用非 boss 角色用户尝试邀请供应商
   - 预期：返回 HTTP 403 Forbidden

4. **无效操作**: 尝试响应已处理过的合作邀请
   - 预期：返回错误提示"该合作记录已处理"

5. **驳回审核**: 管理员驳回入驻申请但不提供原因
   - 预期：前端提示"驳回时必须填写驳回原因"

## 性能测试

1. **批量创建**: 连续提交 100 个入驻申请
2. **分页查询**: 测试大量数据的分页性能
3. **并发邀请**: 多个工作室同时邀请同一供应商

## 已知限制

1. 面料商和租户端使用同一登录入口，通过租户类型区分
2. 目前面料商品类信息未从 fabric_library 表获取，暂时为空
3. 暂未实现面料商独立的管理界面
4. 合作邀请暂未实现消息通知功能

## 后续优化

1. 实现面料商独立管理界面
2. 添加消息通知功能（站内消息、短信、邮件）
3. 集成 fabric_library 表获取面料品类信息
4. 添加合作历史统计和数据分析
5. 实现面料商资质审核流程