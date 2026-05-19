# PROJECT_MEMORY

## 1. 项目基础信息
- 项目名称：濮院毛衫产业 AI 赋能平台
- 当前阶段：MVP 规划完成，进入可开工阶段
- 当前日期：2026-05-18
- 项目目录：`D:\puyuanmaoshan`
- MVP目标上线日：2026-08-20

## 2. 项目目标（MVP）
- 构建多租户 SaaS 平台（商家端 + 管理端）
- 跑通商业闭环：注册 -> 充值 -> 插件调用 -> 扣费 -> 账单
- 首批上线 2 个插件：
  - AI 商品图生成
  - AI 视频脚本生成
- 前端端型策略：网站（Web）+ 微信小程序（APP 暂缓）

## 3. 已完成内容

### 3.1 已产出文档
- `开发需求文档.md`（原始需求）
- `开源项目可以借鉴.md`（开源路线）
- `项目认证与完善规划.md`（已升级至 V1.2）
- `开发任务拆解-可开工版.md`
- `首批库表设计与API清单-可开工版.md`
- `MVP迭代看板-可开工版.md`
- `前端信息架构与页面清单-可开工版.md`
- `前端路由与权限矩阵-可开工版.md`
- `openapi-mvp.yaml`
- `商家Web低保真原型说明-首页与插件页.md`

### 3.2 已产出代码骨架（2026-05-18 新增）
- 前端路由与守卫骨架：
  - `frontend/merchant-web/src/router/*`
  - `frontend/admin-web/src/router/*`
- 前端权限与状态骨架：
  - `frontend/merchant-web/src/stores/auth.ts`
  - `frontend/admin-web/src/stores/adminAuth.ts`
  - `frontend/merchant-web/src/auth/permissions.ts`
- 前端页面占位骨架：
  - `frontend/merchant-web/src/pages/*.vue`
  - `frontend/admin-web/src/pages/*.vue`
- 前端 TypeScript SDK 骨架（基于 openapi 结构分组）：
  - `frontend/shared-sdk/api-client/src/core/*`
  - `frontend/shared-sdk/api-client/src/modules/*`
  - `frontend/shared-sdk/api-client/src/index.ts`
- 后端 Spring Boot 接口桩：
  - `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/controller/*.java`
  - `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/*.java`
  - `backend/java-spring/src/main/resources/application.yml`
  - `backend/java-spring/pom.xml`
- SQL DDL 初稿：
  - `backend/sql/schema_mvp.sql`

### 3.3 本轮新增实现（2026-05-18）
- 前端可运行启动骨架（Vue3 + Pinia + Router）：
  - `frontend/merchant-web/{package.json,index.html,tsconfig.json,vite.config.ts,src/main.ts,src/App.vue,src/styles.css}`
  - `frontend/admin-web/{package.json,index.html,tsconfig.json,vite.config.ts,src/main.ts,src/App.vue,src/styles.css}`
- 后端统一异常处理与错误码：
  - `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/enums/ErrorCode.java`
  - `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/exception/AppException.java`
  - `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/exception/GlobalExceptionHandler.java`
  - `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/ApiResponse.java`（支持 fail、`request_id`）
- 参数校验与响应契约增强：
  - 保留并使用 DTO 上的 `jakarta.validation` 注解
  - `application.yml` 增加 `spring.jackson.property-naming-strategy: SNAKE_CASE`
- MySQL 初始化脚本（含索引与种子数据）：
  - `backend/sql/init_mvp.sql`
  - `backend/sql/seed_mvp.sql`
  - `backend/sql/import_mvp.ps1`

### 3.4 已完成的核心规划
- 完成立项认证结论（商业/技术/合规）
- 完成商业规划补充（9项）、KPI体系、财务测算模板
- 完成按周开发任务拆解与里程碑规划
- 完成首批数据库模型与接口清单
- 完成前端信息架构、路由权限、页面低保真说明
- 生成 OpenAPI MVP 合同文件

## 4. 当前技术与架构决策
- 技术路线（MVP）：NIUCLOUD + one-api（建议）
- 后端能力重点：租户隔离、计费账本、幂等、防重复扣费
- 前端应用：
  - `merchant-web`
  - `admin-web`
  - `merchant-miniapp`
- 接口规范：
  - Header：`X-Tenant-Id`、`X-Request-Id`、`Idempotency-Key`
  - 统一响应：`code/message/data/request_id`

## 5. 待办事项（TODO）

### 5.1 立即待办（开工前）
- [ ] 评审并冻结 OpenAPI（`openapi-mvp.yaml`）
- [x] 输出首版 SQL DDL（按库表设计文档）
- [x] 明确前后端仓库结构（`frontend`/`backend` 已落地，分支策略待执行）
- [x] 生成 MySQL 初始化脚本与种子数据（`init_mvp.sql` + `seed_mvp.sql`）
- [ ] 建立 CI 流水线（构建/测试/静态检查）
- [ ] 确认支付通道与充值回调字段

### 5.2 开发中待办（P0/P1）
- [ ] 完成认证与租户 RBAC 主链路（已落 Controller/Router 骨架 + 统一异常处理）
- [ ] 完成账户钱包、账本、幂等能力（已落 DDL 与接口桩）
- [ ] 完成插件注册与统一 invoke 网关（已落接口桩）
- [ ] 完成 AI 商品图插件 MVP
- [ ] 完成 AI 视频脚本插件 MVP
- [ ] 完成管理后台（租户/插件/定价）

### 5.3 上线前待办（P2）
- [ ] 完成日/月账单与对账重跑工具
- [ ] 完成内容安全审核与审计日志闭环
- [ ] 完成压测与稳定性优化
- [ ] 完成 UAT、灰度与试运营上线

## 6. 风险与关注点
- 计费准确性与争议处理（高优先）
- 上游模型成本波动与质量波动
- 合规材料与内容安全链路是否按时落地
- MVP阶段避免范围膨胀，优先保障闭环
- 当前环境缺少 Java/Maven 运行时，后端骨架暂未本地编译验证
- 前端 merchant/admin 两端已执行 `npm run build` 验证通过

## 7. 维护约定
- 本文件作为项目记忆主文件。
- 后续每次完成任务后，必须更新：
  - 已完成内容
  - 待办事项状态
  - 关键决策或风险变化
- 从 2026-05-18 起，凡是用户要求我“写代码”或“修改代码”，任务完成后我将自动更新本文件，记录本次代码改动与影响范围。

## 2026-05-18 Backend Progress Update (Codex)
- Replaced backend Maven build file with Spring Boot 3.3.2 setup and dependencies:
  - mybatis-plus-spring-boot3-starter
  - mysql-connector-j
  - lombok
  - spring-boot-starter-validation
  - springdoc-openapi-starter-webmvc-ui
- Added MyBatis Plus pagination configuration and mapper scanning.
- Generated entities for schema_mvp.sql tables:
  - tenant
  - user_account
  - plugin
  - tenant_plugin
  - account_wallet
  - idempotency_record
  - billing_ledger
  - recharge_order
  - billing_statement_daily
  - plugin_invoke_log
  - audit_log
- Generated corresponding BaseMapper interfaces.
- Generated corresponding IService interfaces and ServiceImpl classes.
- Added utility classes for request context parsing and business/order number generation.
- Replaced controller stubs with service-based database logic:
  - AuthController: login/profile from tenant + user_account
  - AccountController: balance, ledger query, recharge order create/confirm, recharge order query
  - PluginController: plugin list, enable/disable, invoke with wallet deduction + ledger + invoke log + idempotency record
  - BillingController: daily/monthly statements from statement table with ledger fallback aggregation
  - AdminTenantController: tenant paging + freeze
  - AdminPluginController: plugin CRUD-style management (list/create/update/delete) + publish
  - AdminPricingController: moved to PricingConfigService
  - AdminBillingController: dashboard aggregation + recharge order query
- Added dev profile config:
  - backend/java-spring/src/main/resources/application-dev.yml
  - Includes datasource for MySQL84/puyuan_ai_mvp, MyBatis Plus config, log levels, springdoc paths.
- Note:
  - Code generation completed without local Java/Maven runtime validation in this environment.

## 2026-05-18 Postman and Runtime Check Update (Codex)
- Added Postman collection file:
  - backend/postman/puyuan-ai-mvp.postman_collection.json
  - Covers implemented endpoints in Auth/Account/Plugins/Billing/Admin modules.
  - Includes sample request bodies and sample response descriptions per request.
- Verified backend runtime + DB connectivity against MySQL puyuan_ai_mvp:
  - Started app with profile dev and called APIs that require database reads.
  - health, tenant profile, and recharge order query returned code=0.
  - No datasource connection failure observed.

## 2026-05-18 Frontend Integration Update (Codex)
- merchant-web auth flow switched from stub to real backend calls.
  - Updated store: frontend/merchant-web/src/stores/auth.ts
  - Reads and persists access token + tenant id in localStorage.
  - Calls /api/v1/tenant/profile for role/tenant status.
- Added working login page implementation.
  - Updated: frontend/merchant-web/src/pages/LoginPage.vue
  - Calls /api/v1/auth/login with mobile + verify_code.
  - Saves token/tenant and redirects to intended route.
- Added working plugin list page implementation.
  - Updated: frontend/merchant-web/src/pages/PluginsPage.vue
  - Calls /api/v1/plugins and supports enable/disable actions.
- Updated route guard error handling.
  - Updated: frontend/merchant-web/src/router/guards.ts
  - If profile load fails, clears auth and redirects to /login.
- Added Vite dev proxy to backend.
  - Updated: frontend/merchant-web/vite.config.ts
  - Proxies /api to http://127.0.0.1:8080 to avoid CORS in dev.
- Validation:
  - Ran merchant-web npm run build successfully.

## 2026-05-18 ������¼��P0/P1��
- ��Χ��ƫ���ϸ� MVP �ĵ��ƽ�����������Ʒ������ع��ܡ�
- ��� P0��
  - ���� `RechargeOrderWorkflowServiceImpl`��ͳһʵ�ֳ�ֵ�µ�/ȷ�ϵ������ݵȡ��˱����ˡ����ظ����ˡ�
  - `PluginController.invoke` ��Ϊ���� `PluginInvokeWorkflowService`�������ݵ���۷���·���������/�������������־д�롣
  - `AccountController` ��ֵ����/ȷ�ϸ�Ϊ���� workflow service��֧�� confirm ���� `Idempotency-Key`��
- ��� P1�������� API����
  - �⻧�����ⶳ `POST /api/v1/admin/tenants/{tenant_id}/unfreeze`��
  - �⻧�ײ͸��� `PUT /api/v1/admin/tenants/{tenant_id}/level`��
  - ������Ʋ�ѯ `GET /api/v1/admin/audit`��֧�� tenant_id/action/ʱ�䷶Χ��ҳ��ѯ����
- ��� P2��
  - ���� `BillingStatementScheduler`��ÿ���賿���� `billing_statement_daily`��ÿ�¾ۺ�д��Ƽ�¼��
  - `PlatformApplication` ���� `@EnableScheduling`��
- ǰ�� merchant-web��
  - ʵ�� `/dashboard`��`/account/balance`��`/account/recharge`��`/account/ledger`��`/billing`��ȫ���Խ���ʵ API��
- ǰ�� admin-web��
  - admin ��¼�� stub ��Ϊ��ʵ��¼ + profile ��ȡ��
  - ʵ�� `/admin/dashboard`��`/admin/tenants`��`/admin/plugins`��`/admin/pricing`��`/admin/billing`��`/admin/audit`��
  - ���� Vite `/api` ��������� `http://127.0.0.1:8080`��
- Postman��
  - ���¼��ϣ����������� `unfreeze`/`level`/`audit` �ӿ���Ŀ��
- ��֤��
  - ��� `mvn -DskipTests package` ͨ����
  - merchant-web `npm run build` ͨ����
  - admin-web `npm run build` ͨ����
  - ����̬��־ȷ�� MySQL ���ӳɹ���Hikari ��ʼ���ɹ����ɲ�ѯ `account_wallet`����
