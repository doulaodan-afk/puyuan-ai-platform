# 濮院毛衫 AI 平台

基于插件化架构的 AI 服务管理平台，支持多租户接入、计费管理和灵活的插件扩展。

## 功能特性

### 管理端
- 租户管理：创建、冻结/解冻租户
- 插件管理：创建、编辑、上架/下架插件
- 定价管理：配置 Token 价格和存储费用
- 账单管理：查看充值订单和统计数据
- 审计日志：记录所有管理操作

### 租户端
- 插件管理：启用/禁用可用插件
- 账户管理：查看余额和存储使用情况
- 充值中心：在线充值和订单管理
- 账单中心：查看日账单和月账单
- 消费明细：详细的 Token 消费流水

## 技术栈

### 后端
- Java 21
- Spring Boot 3.3
- MyBatis Plus
- MySQL 8.0

### 前端
- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router

## 快速开始

### 环境要求
- Java 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 后端启动

```bash
cd backend/java-spring
mvn clean package -DskipTests
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

后端服务运行在 `http://localhost:8080`

### 前端启动

**管理端：**
```bash
cd frontend/admin-web
npm install
npm run dev
```
访问：http://localhost:5174

**租户端：**
```bash
cd frontend/merchant-web
npm install
npm run dev
```
访问：http://localhost:5173

### 数据库初始化

```powershell
cd backend/sql
./import_mvp.ps1
```

这将自动创建数据库、表结构并导入初始数据。

## 测试账号

### 管理端
- 手机号：13800000000
- 验证码：123456

### 租户端
- 手机号：13800000001
- 验证码：123456

## 项目结构

```
puyuanmaoshan/
├── backend/
│   ├── java-spring/        # Java Spring Boot 后端
│   │   ├── src/main/java/
│   │   └── src/main/resources/
│   ├── sql/                # 数据库脚本
│   └── postman/            # API 测试集合
├── frontend/
│   ├── admin-web/          # 管理端前端
│   ├── merchant-web/        # 租户端前端
│   └── shared-sdk/         # 共享 SDK
└── docs/                   # 项目文档
```

## 开发指南

### API 文档
OpenAPI 规范文件：[openapi-mvp.yaml](openapi-mvp.yaml)

### 插件开发
插件通过 RESTful API 接入，参考 [openapi-mvp.yaml](openapi-mvp.yaml) 中的插件接口定义。

## 许可证

私有项目，未经授权不得使用。
