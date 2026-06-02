# 面料商模块部署文档

## 概述

本文档描述了面料商模块的部署流程，包括数据库迁移、后端配置和前端部署。

## 部署前准备

### 环境要求
- Java 21+
- MySQL 8.0+
- Node.js 18+
- Maven 3.8+

### 服务端口
- 后端 API: 8080
- 管理端前端: 5174
- 租户端前端: 5173

## 1. 数据库部署

### 1.1 执行迁移脚本

```bash
# 进入项目目录
cd d:/puyuanmaoshan

# 执行供应商模块数据库迁移
mysql -u root -p123456 puyuan_ai_mvp < backend/sql/supplier_migration.sql
```

### 1.2 验证表结构

```sql
-- 查看 supplier_registration 表结构
DESC supplier_registration;

-- 查看 supplier_collaboration 表结构
DESC supplier_collaboration;

-- 验证表已创建
SHOW TABLES LIKE '%supplier%';
```

### 1.3 创建测试数据（可选）

```sql
-- 插入测试面料商租户
INSERT INTO tenant (tenant_code, name, status, level, tenant_type, created_at)
VALUES ('SUP-TEST001', '测试面料商有限公司', 1, 'basic', 'supplier', NOW());

-- 获取插入的租户 ID
SET @supplier_tenant_id = LAST_INSERT_ID();

-- 插入测试面料商用户
INSERT INTO user_account (tenant_id, mobile, nickname, role_code, status, created_at)
VALUES (@supplier_tenant_id, '13700000001', '面料商老板', 'boss', 1, NOW());

-- 插入测试入驻申请
INSERT INTO supplier_registration (
  company_name, contact_name, contact_mobile, address,
  fabric_categories, description, status, created_at
)
VALUES (
  '测试面料商有限公司',
  '张三',
  '13700000001',
  '杭州市余杭区',
  JSON_ARRAY('真丝', '羊毛'),
  '专注高端面料供应',
  'approved',
  NOW()
);
```

## 2. 后端部署

### 2.1 检查依赖

```bash
cd backend/java-spring

# 检查 pom.xml 中已包含必要依赖
grep -E "mybatis-plus|jackson" pom.xml
```

### 2.2 验证配置

检查 `src/main/resources/application-dev.yml` 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/puyuan_ai_mvp
    username: root
    password: 123456
  jackson:
    property-naming-strategy: SNAKE_CASE
```

### 2.3 编译打包

```bash
cd backend/java-spring

# 清理并编译
mvn clean package -DskipTests

# 或者不跳过测试
mvn clean package
```

### 2.4 启动后端服务

```bash
# 开发环境
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 生产环境（需配置生产环境配置文件）
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 2.5 验证后端 API

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 验证供应商 API 可用
curl -X GET http://localhost:8080/api/supplier/available?page=1&size=20 \
  -H "X-Tenant-Id: 2001"
```

## 3. 前端部署

### 3.1 管理端部署

```bash
cd frontend/admin-web

# 安装依赖
npm install

# 开发环境启动
npm run dev

# 生产环境构建
npm run build

# 预览构建结果
npm run preview
```

管理端默认运行在: http://localhost:5174

### 3.2 租户端部署

```bash
cd frontend/merchant-web

# 安装依赖
npm install

# 开发环境启动
npm run dev

# 生产环境构建
npm run build

# 预览构建结果
npm run preview
```

租户端默认运行在: http://localhost:5173

### 3.3 验证前端路由

```bash
# 管理端
curl http://localhost:5174/admin/supplier-review

# 租户端
curl http://localhost:5173/design-assistant/partners
```

## 4. 验证部署

### 4.1 管理端验证

1. 访问 http://localhost:5174/admin/login
2. 使用平台管理员账号登录（13800000001 / 123456）
3. 点击"面料商"菜单
4. 验证可以查看入驻申请列表

### 4.2 租户端验证

1. 访问 http://localhost:5173/login
2. 使用工作室账号登录（13900000001 / 123456）
3. 导航到 "设计助手" -> "合作方管理"
4. 验证可以查看可合作供应商列表

## 5. 生产环境配置

### 5.1 数据库配置

创建生产环境数据库：

```sql
CREATE DATABASE puyuan_ai_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用生产数据库迁移脚本
mysql -u root -p puyuan_ai_prod < backend/sql/supplier_migration.sql
```

### 5.2 后端生产配置

创建 `application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://prod-db-host:3306/puyuan_ai_prod?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jackson:
    property-naming-strategy: SNAKE_CASE

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.puyuanmaoshan.platform: info
  file:
    name: logs/platform-api.log
    max-size: 100MB
    max-history: 30
```

### 5.3 环境变量配置

```bash
# 数据库配置
export DB_USERNAME=prod_db_user
export DB_PASSWORD=prod_db_password

# Redis 配置
export REDIS_HOST=prod-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=redis_password

# 启动服务
java -jar target/platform-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DB_USERNAME=$DB_USERNAME \
  --DB_PASSWORD=$DB_PASSWORD
```

### 5.4 前端生产构建

管理端 Vite 配置 (`vite.config.ts`)：

```typescript
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      "/api": {
        target: "https://api.puyuan-ai.com",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    assetsDir: "assets",
    sourcemap: false,
    minify: "terser",
  },
});
```

## 6. Docker 部署（可选）

### 6.1 后端 Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/platform-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6.2 前端 Dockerfile

管理端：

```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### 6.3 Docker Compose

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: puyuan_ai_prod
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/sql:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"

  backend:
    build: ./backend/java-spring
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/puyuan_ai_prod
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root_password
    ports:
      - "8080:8080"

  admin-web:
    build: ./frontend/admin-web
    depends_on:
      - backend
    ports:
      - "5174:80"

  merchant-web:
    build: ./frontend/merchant-web
    depends_on:
      - backend
    ports:
      - "5173:80"

volumes:
  mysql-data:
```

## 7. 监控和日志

### 7.1 健康检查

```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# 指标端点
curl http://localhost:8080/actuator/metrics
```

### 7.2 日志查看

```bash
# 后端日志
tail -f logs/platform-api.log

# 查看供应商相关日志
grep "Supplier" logs/platform-api.log
```

## 8. 回滚计划

### 8.1 数据库回滚

```sql
-- 删除供应商相关表
DROP TABLE IF EXISTS supplier_collaboration;
DROP TABLE IF EXISTS supplier_registration;
```

### 8.2 应用回滚

```bash
# 停止服务
kill -9 $(cat app.pid)

# 恢复旧版本
cp backup/platform-api-0.0.1-OLD.jar app.jar

# 重启服务
java -jar app.jar --spring.profiles.active=prod
```

## 9. 故障排查

### 9.1 常见问题

**问题**: 数据库连接失败
```
解决: 检查数据库地址、端口、用户名、密码配置
```

**问题**: API 请求 404
```
解决: 检查路由配置和控制器路径映射
```

**问题**: 前端构建失败
```
解决: 检查 Node.js 版本，清理 node_modules 重新安装
```

**问题**: 跨域请求错误
```
解决: 检查 Vite proxy 配置和后端 CORS 配置
```

### 9.2 日志分析

```bash
# 查看错误日志
grep "ERROR" logs/platform-api.log

# 查看供应商模块日志
grep -A 5 "Supplier" logs/platform-api.log

# 查看慢查询
grep "Slow query" logs/platform-api.log
```

## 10. 性能优化建议

1. **数据库优化**
   - 为 supplier_registration 表的 contact_mobile 字段添加索引
   - 为 supplier_collaboration 表的 status 字段添加索引
   - 配置数据库连接池参数

2. **缓存策略**
   - 使用 Redis 缓存可合作供应商列表
   - 设置合理的缓存过期时间

3. **前端优化**
   - 启用路由懒加载
   - 配置 CDN 加速静态资源
   - 启用 Gzip 压缩

4. **后端优化**
   - 配置线程池参数
   - 启用异步处理
   - 实现请求限流

## 11. 安全建议

1. **数据安全**
   - 敏感信息加密存储（手机号、地址）
   - API 请求添加签名验证
   - 定期备份数据库

2. **访问控制**
   - 实现细粒度权限控制
   - 添加操作审计日志
   - 配置请求频率限制

3. **传输安全**
   - 强制使用 HTTPS
   - 配置 SSL/TLS 证书
   - 实施 CSP 策略