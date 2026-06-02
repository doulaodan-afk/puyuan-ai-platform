# AI 设计助手插件 - 预发布检查清单

## 一、代码检查

### 1.1 后端 Spring Boot

- [x] Entity 类：Requirement.java, Task.java, Fabric.java, Message.java
- [x] DTO 类：AiDesignAssistantDtos.java
- [x] Service 接口：AiDesignAssistantPluginService.java
- [x] Service 实现：AiDesignAssistantPluginServiceImpl.java
- [x] Controller：AiDesignAssistantPluginController.java
- [x] 所有 25+ API 端点已实现

**API 端点清单：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/plugins/ai-design-assistant/requirements | 需求列表 |
| GET | /api/plugins/ai-design-assistant/requirements/{id} | 需求详情 |
| POST | /api/plugins/ai-design-assistant/requirements | 创建需求 |
| PUT | /api/plugins/ai-design-assistant/requirements/{id} | 更新需求 |
| DELETE | /api/plugins/ai-design-assistant/requirements/{id} | 删除需求 |
| GET | /api/plugins/ai-design-assistant/requirements/{id}/ai-summary | AI 总结 |
| GET | /api/plugins/ai-design-assistant/tasks | 任务列表 |
| GET | /api/plugins/ai-design-assistant/tasks/{id} | 任务详情 |
| POST | /api/plugins/ai-design-assistant/tasks | 创建任务 |
| PUT | /api/plugins/ai-design-assistant/tasks/{id} | 更新任务 |
| DELETE | /api/plugins/ai-design-assistant/tasks/{id} | 删除任务 |
| POST | /api/plugins/ai-design-assistant/tasks/{id}/accept | 接受任务 |
| POST | /api/plugins/ai-design-assistant/tasks/{id}/reject | 拒绝任务 |
| POST | /api/plugins/ai-design-assistant/tasks/{id}/ship | 发货 |
| POST | /api/plugins/ai-design-assistant/tasks/{id}/deliver | 确认收货 |
| GET | /api/plugins/ai-design-assistant/fabrics | 面料列表 |
| GET | /api/plugins/ai-design-assistant/fabrics/{id} | 面料详情 |
| POST | /api/plugins/ai-design-assistant/fabrics | 创建面料 |
| PUT | /api/plugins/ai-design-assistant/fabrics/{id} | 更新面料 |
| DELETE | /api/plugins/ai-design-assistant/fabrics/{id} | 删除面料 |
| GET | /api/plugins/ai-design-assistant/messages | 消息列表 |
| GET | /api/plugins/ai-design-assistant/messages/{id} | 消息详情 |
| POST | /api/plugins/ai-design-assistant/messages | 发送消息 |
| POST | /api/plugins/ai-design-assistant/messages/{id}/read | 标记已读 |
| POST | /api/plugins/ai-design-assistant/messages/read-all | 全部已读 |
| DELETE | /api/plugins/ai-design-assistant/messages/{id} | 删除消息 |
| GET | /api/plugins/ai-design-assistant/statistics | 统计数据 |

### 1.2 前端 Vue 组件

- [x] index.ts - 插件安装入口
- [x] api/index.ts - API 客户端（含 Mock 模式）
- [x] router/index.ts - 路由配置
- [x] stores/index.ts - Pinia 状态管理
- [x] types/index.ts - TypeScript 类型
- [x] manifest.json - 插件配置清单
- [x] package-plugin.sh - 打包脚本

### 1.3 页面组件 (11个)

- [x] pages/index.vue - 工作台/首页
- [x] pages/requirement-list.vue - 需求列表
- [x] pages/requirement-create.vue - 创建需求
- [x] pages/requirement-detail.vue - 需求详情
- [x] pages/task-board.vue - 任务看板
- [x] pages/fabric-library.vue - 面料库
- [x] pages/my-tasks.vue - 我的任务
- [x] pages/messages.vue - 消息中心
- [x] pages/team-settings.vue - 团队设置
- [x] pages/partner-manage.vue - 合作伙伴
- [x] pages/board.vue - 数据统计看板

---

## 二、部署检查

### 2.1 Docker 部署

- [x] backend/java-spring/Dockerfile - 后端 Docker 镜像
- [x] docker-compose.yml - 完整服务编排（Java Spring + PostgreSQL + Redis）

### 2.2 数据库

- [x] sql/plugin_lifecycle_migration.sql - 插件生命周期表结构

### 2.3 环境配置

- [x] VITE_USE_MOCK 环境变量支持
- [x] API 代理配置 /api/plugins/* -> backend

---

## 三、文档检查

- [x] PLUGIN_API_SPECIFICATION.md - API 规范文档
- [x] test-cases/AI-Design-Assistant-Test-Cases.md - 测试用例文档
- [x] frontend/merchant-web/src/plugins/ai-design-assistant/README.md - 前端组件说明

---

## 四、功能验证

### 4.1 核心流程

- [ ] 需求创建 -> AI 对话 -> AI 总结 -> 发布需求
- [ ] 任务创建 -> 供应商接受 -> 提交结果 -> 物流发货 -> 确认收货
- [ ] 面料增删改查
- [ ] 消息通知读写

### 4.2 Mock 模式测试

- [ ] 前端 VITE_USE_MOCK=true 模式正常运行
- [ ] API 返回 Mock 数据格式正确

### 4.3 集成测试

- [ ] 后端 `/api/plugins/ai-design-assistant/*` 接口正常响应
- [ ] 前端代理正确转发请求到后端

---

## 五、性能检查

- [ ] Mock 数据响应时间 < 100ms
- [ ] 列表查询支持分页
- [ ] 大数据量无内存溢出

---

## 六、安全检查

- [ ] X-Tenant-Id 和 X-User-Id 请求头传递
- [ ] SQL 注入防护（使用 MyBatis 参数化查询）
- [ ] 无敏感信息泄露（日志、响应）

---

## 七、上线前准备

### 7.1 环境变量

```bash
# 后端
SPRING_PROFILES_ACTIVE=prod
DB_HOST=postgres
DB_PORT=5432
DB_NAME=puyuanmaoshan
DB_USER=postgres
DB_PASSWORD=<strong-password>

# 前端
VITE_USE_MOCK=false
VITE_ENABLE_AI_DESIGN_ASSISTANT=true
```

### 7.2 构建命令

```bash
# 后端构建
cd backend/java-spring
./mvnw package -DskipTests

# 前端构建
cd frontend/merchant-web
npm run build

# Docker 构建
docker-compose build
```

### 7.3 启动命令

```bash
# Docker Compose 启动全部服务
docker-compose up -d

# 单独启动后端
docker run -p 8080:8080 puyuanmaoshan-backend
```

---

## 八、问题追踪

| 问题 | 状态 | 负责人 | 备注 |
|------|------|--------|------|
| Message.java setTitle 方法签名错误 | ✅ 已修复 | AI Assistant | 2026-05-22 |
| Mock 数据路径与后端不一致 | ✅ 已修复 | AI Assistant | 2026-05-22 |

---

**最后更新：** 2026-05-22
**版本：** 1.0.0