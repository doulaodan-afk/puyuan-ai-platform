# 面料商入驻和合作管理功能实施总结

## 实施日期
2026-05-19

## 功能概述
本次实施完成以下三个核心功能：

1. **面料商自主入驻** - 公共注册页面，管理员审核，审核通过后自动创建租户和用户
2. **老板合作方管理** - 老板可邀请已入驻面料商，查看合作列表，屏蔽供应商
3. **需求创建时选择面料商** - 在创建设计需求时可选择已合作的供应商，系统自动分配面料任务

## 数据库变更

### 新增表
1. `supplier_registration` - 面料商入驻申请表
2. `supplier_collaboration` - 面料商合作表

SQL 文件: `backend/sql/supplier_migration.sql`

## 后端实现

### 新增文件
1. `SupplierCollaboration.java` - 实体类
2. `SupplierDtos.java` - DTO 定义（含入驻、审核、合作相关）
3. `SupplierCollaborationMapper.java` - Mapper 接口
4. `SupplierCollaborationService.java` - 服务接口
5. `SupplierCollaborationServiceImpl.java` - 服务实现
6. `SupplierController.java` - 供应商 API 控制器
7. `AdminSupplierController.java` - 管理端审核控制器

### 修改文件
1. `DesignAssistantDtos.java` - CreateRequirementRequest 添加 selectedSupplierId 字段
2. `DesignRequirementServiceImpl.java` - createRequirement 方法处理面料商直接分配

## 前端实现

### 新增页面
1. `SupplierRegisterPage.vue` - 面料商入驻注册页面（公开访问）
2. `AdminSupplierReviewPage.vue` - 管理员入驻审核页面
3. `PartnerManage.vue` - 合作方管理页面（设计助手插件内部）

### 修改文件
1. `routes.ts` - 添加新路由：
   - `/register-supplier` - 公共注册
   - `/admin/supplier-review` - 管理端审核
   - `/design-assistant/partners` - 合作方管理
2. `useMenuFilter.ts` - 添加合作方管理菜单（仅 boss 可见）
3. `DesignRequirementCreate.vue` - 添加面料商选择下拉框
4. `design-assistant.ts` - createRequirement API 添加 selectedSupplierId 参数

## API 接口

### 面料商入驻（公开）
- `POST /api/supplier/register` - 提交入驻申请

### 管理端审核
- `GET /api/admin/supplier/registrations` - 获取待审核列表
- `PUT /api/admin/supplier/registration/{id}/review` - 审核申请

### 合作管理
- `GET /api/supplier/available` - 获取可合作供应商列表
- `POST /api/supplier/collaboration/invite` - 邀请供应商合作
- `GET /api/supplier/collaboration/list` - 获取合作列表
- `PUT /api/supplier/collaboration/respond/{id}` - 供应商响应邀请
- `PUT /api/supplier/collaboration/block/{id}` - 屏蔽供应商

## 业务流程

### 入驻流程
1. 面料商访问 `/register-supplier` 填写入驻信息
2. 提交申请后状态为 `pending`
3. 管理员在 `/admin/supplier-review` 审核申请
4. 审核通过后自动创建：
   - 供应商租户（tenant_type = 'supplier'）
   - 供应商用户（role_code = 'boss'）
   - 租户用户关联

### 合作流程
1. 老板访问 `/design-assistant/partners` 查看可合作供应商
2. 点击"邀请合作"发送邀请（状态：pending）
3. 供应商收到邀请后确认或拒绝
4. 确认后状态变为 `accepted`
5. 老板可随时屏蔽供应商（状态：blocked）

### 需求创建流程
1. 用户访问 `/design-assistant/create`
2. 可选择已合作的供应商
3. 创建需求时，如选择了供应商，系统自动创建面料任务分配给该供应商

## 权限控制

| 角色 | 可访问功能 |
|------|----------|
| 公开用户 | 面料商入驻注册 |
| 管理员 | 入驻审核 |
| Boss | 合作方管理（邀请、屏蔽）、需求创建选供应商 |
| 其他角色 | 仅查看 |

## 技术要点

1. **多租户隔离** - 所有操作严格检查 tenant_id
2. **事务管理** - 涉及多表操作使用 @Transactional
3. **JSON 字段** - fabric_categories 使用 JSON 类型存储
4. **状态机** - pending → approved/rejected → accepted/blocked
5. **UI 集成** - 合作方管理集成到设计助手插件内部布局

## 部署说明

### 1. 执行数据库迁移
```bash
mysql -u root -p puyuan_ai_mvp < backend/sql/supplier_migration.sql
```

### 2. 后端编译
```bash
cd backend/java-spring
mvn clean package
```

### 3. 前端编译
```bash
cd frontend/merchant-web
npm run build
```

### 4. 启动服务
```bash
# 后端
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 前端
npm run dev
```

## 测试指南

### 测试面料商入驻
1. 访问 `http://localhost:5178/register-supplier`
2. 填写公司信息、联系人、面料品类
3. 上传营业执照
4. 提交申请

### 测试管理员审核
1. 访问 `http://localhost:5178/admin/supplier-review`
2. 查看待审核列表
3. 点击通过/驳回
4. 验证是否自动创建了租户和用户

### 测试合作管理
1. 登录 Boss 账号
2. 访问 `/design-assistant/partners`
3. 查看可合作供应商列表
4. 发送合作邀请
5. 查看合作列表

### 测试需求创建选供应商
1. 访问 `/design-assistant/create`
2. 选择已合作的供应商
3. 开始对话并创建需求
4. 验证是否自动创建了面料任务分配给选中的供应商

## 后续优化建议

1. 添加消息通知 - 合作邀请、需求分配等通知
2. 供应商端视图 - 供应商查看待处理任务
3. 面料商评分系统 - 基于合作质量打分
4. 批量导入供应商 - 管理员批量添加供应商
5. 供应商品牌展示 - 供应商可上传公司 Logo 和介绍