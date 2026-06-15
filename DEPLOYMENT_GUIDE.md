# 濮院毛衫 AI 平台 - 部署保障体系使用指南

> **版本**: v2.0 | **更新日期**: 2026-06-04

---

## 目录

1. [体系概览](#体系概览)
2. [部署检查清单](#部署检查清单)
3. [自动化部署脚本](#自动化部署脚本)
4. [Docker 环境配置](#docker-环境配置)
5. [Flyway 数据库迁移](#flyway-数据库迁移)
6. [核心功能测试](#核心功能测试)
7. [回滚预案](#回滚预案)
8. [常见问题](#常见问题)

---

## 体系概览

```
部署保障体系
├── DEPLOYMENT_CHECKLIST.md     # 部署检查清单（人工核对）
├── deploy.sh / deploy.bat      # 自动化部署脚本
├── test-core.sh                # 核心功能冒烟测试
├── docker-compose.yml          # 本地开发 Docker 环境
├── deploy/
│   ├── docker-compose.yml      # 生产 Docker 环境
│   ├── Dockerfile.backend      # 后端基础镜像
│   ├── nginx/nginx.conf        # Nginx 配置
│   └── .env.example            # 环境变量模板
├── backend/java-spring/
│   ├── Dockerfile              # 后端多阶段构建
│   └── src/main/resources/db/migration/  # Flyway 迁移脚本
├── frontend/
│   ├── merchant-web/Dockerfile # 商家端 Dockerfile
│   └── admin-web/Dockerfile    # 管理端 Dockerfile
└── DEPLOYMENT_GUIDE.md         # 本文件
```

---

## 部署检查清单

**文件**: `DEPLOYMENT_CHECKLIST.md`

部署前按顺序逐项检查，包含 7 大类共 50+ 检查项：

| 类别 | 检查项数 | 说明 |
|------|---------|------|
| 代码提交检查 | 8 | 代码状态、版本号、安全审计 |
| 编译检查 | 8 | 前后端编译、测试、类型检查 |
| 数据库迁移检查 | 7 | Flyway 脚本、备份、字符集 |
| 环境变量检查 | 12 | 密钥、第三方配置、开关 |
| 测试检查 | 10 | 冒烟测试、功能验证 |
| Docker 服务检查 | 8 | 容器状态、健康检查 |
| 安全检查 | 6 | HTTPS、安全头、端口暴露 |

**使用方式**:
1. 部署前打开 `DEPLOYMENT_CHECKLIST.md`
2. 逐项检查，通过后勾选 ☑
3. 所有项目通过后方可执行生产部署
4. 部署完成后签字归档

---

## 自动化部署脚本

### deploy.sh (Linux/Mac)

**功能**: 一键完成从代码拉取到健康检查的全流程。

```bash
# 完整部署
bash deploy.sh

# 仅部署后端（跳过前端构建）
bash deploy.sh --skip-frontend

# 使用已有 jar 包部署（跳过编译）
bash deploy.sh --skip-build

# 跳过数据库迁移
bash deploy.sh --skip-db

# 回滚到上一个版本
bash deploy.sh --rollback
```

**执行流程**:
```
1. 环境检查 (Java 17+, Node 18+, Maven, Git, Docker)
2. 拉取最新代码 (git fetch + reset --hard)
3. 后端编译打包 (mvn clean compile test package)
4. 前端编译打包 (merchant-web + admin-web)
5. 备份当前版本 (jar + 前端 + 数据库)
6. 数据库迁移 (Flyway 自动 + 手动 SQL)
7. 部署产物 (复制 jar + dist 到 deploy/)
8. 重启服务 (docker-compose restart)
9. 健康检查 (curl /actuator/health)
```

### deploy.bat (Windows)

功能与 `deploy.sh` 一致，适配 Windows 环境：

```batch
REM 完整部署
deploy.bat

REM 跳过前端构建
deploy.bat --skip-frontend

REM 回滚
deploy.bat --rollback
```

---

## Docker 环境配置

### 本地开发环境

```bash
# 启动完整开发环境（MySQL + Redis + 后端 + Nginx）
docker-compose up -d

# 重建并启动
docker-compose up -d --build

# 查看日志
docker-compose logs -f backend

# 停止
docker-compose down
```

**服务端口**:
| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3307 | 映射到宿主机 3307 |
| Redis | 6379 | 默认端口 |
| 后端 | 8080 | Spring Boot API |
| Nginx | 80 | 反向代理 + 静态文件 |

### 生产环境

```bash
cd deploy/

# 1. 构建后端基础镜像（首次或镜像变更时）
docker build -t puyuan-ai-platform-backend:latest -f Dockerfile.backend .

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入生产配置

# 3. 部署后端 jar 和前端 dist 到 deploy/ 目录
cp ../backend/java-spring/target/platform-api-0.0.1-SNAPSHOT.jar ./
cp -r ../frontend/merchant-web/dist ./frontend/merchant-web/
cp -r ../frontend/admin-web/dist ./frontend/admin-web/

# 4. 启动服务
docker-compose up -d

# 5. 验证
curl http://localhost:8080/actuator/health
```

### Dockerfile 说明

#### 后端多阶段构建 (`backend/java-spring/Dockerfile`)
- **阶段 1**: Maven 编译 (maven:3.9-eclipse-temurin-17-alpine)
- **阶段 2**: 运行时 (eclipse-temurin:17-jdk-alpine)
- JVM 参数: `-Xms512m -Xmx1024m -XX:+UseG1GC`
- 包含 healthcheck

#### 前端 Dockerfile (`frontend/*/Dockerfile`)
- **阶段 1**: Node.js 构建 (node:18-alpine)
- **阶段 2**: Nginx 静态文件服务 (nginx:alpine)

#### 生产基础镜像 (`deploy/Dockerfile.backend`)
- 预构建镜像，jar 通过 volume 挂载
- 避免每次部署重新构建 Docker 镜像
- JVM: `-Xms512m -Xmx1024m -XX:+UseG1GC`

---

## Flyway 数据库迁移

### 配置说明

Flyway 已在 `application-docker.yml` 和 `application-prod.yml` 中启用：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true    # 新数据库自动建立基线
    baseline-version: 1
    table: flyway_schema_history
    encoding: UTF-8
    out-of-order: false
    validate-on-migrate: true    # 启动时校验迁移脚本完整性
    clean-disabled: true         # 禁止 clean 操作（安全）
```

### 迁移脚本规范

脚本位置: `backend/java-spring/src/main/resources/db/migration/`

**命名规范**:
- 版本迁移: `V{版本号}__{描述}.sql`
- 回滚脚本: `U{版本号}__{描述}_rollback.sql`

**现有脚本**:
```
V1.0__baseline_schema.sql          # 基线架构（26张表）
V1.2__user_profile_and_admin.sql   # 用户资料 + 角色管理
V1.3__plugin_ai_model.sql          # 插件 AI 模型关联
U1.0__baseline_schema_rollback.sql # V1.0 回滚脚本
```

**编写新迁移脚本**:
```sql
-- V1.4__add_new_feature.sql
-- 描述: 添加新功能表

CREATE TABLE IF NOT EXISTS `new_table` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    ...
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对于列变更，使用幂等写法
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'existing_table' AND COLUMN_NAME = 'new_column';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `existing_table` ADD COLUMN `new_column` VARCHAR(100) NULL COMMENT ''新列''',
    'SELECT ''Column already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 执行方式

Flyway 会在 Spring Boot 启动时自动执行迁移，无需手动干预。

如需手动执行:
```bash
# 使用 Maven 插件
cd backend/java-spring
mvn flyway:migrate -Dflyway.url=jdbc:mysql://localhost:3306/puyuan_ai_mvp \
    -Dflyway.user=root -Dflyway.password=yourpassword

# 查看迁移状态
mvn flyway:info
```

---

## 核心功能测试

**文件**: `test-core.sh`

### 测试覆盖

| 测试类别 | 测试项 | 说明 |
|---------|--------|------|
| 健康检查 | 服务健康检查 | `/actuator/health` |
| 登录功能 | 短信验证码发送 | `/api/v1/auth/send-code` |
| 登录功能 | 短信验证码登录 | `/api/v1/auth/login` |
| 登录功能 | Token 有效性 | `/api/v1/user/profile` |
| 插件列表 | 获取插件列表 | `/api/v1/plugin/list` |
| 插件列表 | 列表非空验证 | 数据校验 |
| 插件列表 | 按分类筛选 | `?category=image` |
| AI 调用 | AI 服务可用性 | 健康检查组件状态 |
| AI 调用 | AI 插件调用 | `/api/v1/plugin/invoke` |
| 系统信息 | API 文档 | `/v3/api-docs` |
| 系统信息 | 数据库连接 | health db 组件 |
| 性能基准 | 响应时间 | < 3s |
| 性能基准 | 并发请求 | 5 并发 ≥ 80% 成功率 |

### 使用方式

```bash
# 默认测试 localhost:8080
bash test-core.sh

# 指定测试地址
bash test-core.sh --base-url https://ai.puyuanmaoshan.com

# 详细输出
bash test-core.sh --base-url http://localhost:8080 --verbose
```

**预期输出**:
```
╔════════════════════════════════════════╗
║   濮院毛衫 AI 平台 - 核心功能测试    ║
║   测试环境: http://localhost:8080     ║
╚════════════════════════════════════════╝

[TEST] 1. 健康检查
  [服务健康检查] PASS

[TEST] 2. 登录功能测试
  [发送短信验证码] PASS
  [短信验证码登录] PASS
  [Token 有效性验证] PASS

...

========================================
  测试结果汇总
========================================
  通过: 12
  失败: 0
  跳过: 1
  总计: 13
```

---

## 回滚预案

### 使用部署脚本回滚

```bash
# 自动回滚到上一次部署的版本
bash deploy.sh --rollback
```

回滚流程:
1. 查找最新备份目录 (`deploy/backups/YYYYMMDD_HHMMSS/`)
2. 停止后端和 Nginx 服务
3. 恢复 Jar 文件
4. 恢复前端文件
5. 恢复数据库（如有备份）
6. 启动服务
7. 执行健康检查

### 手动回滚

```bash
# 1. 停止服务
cd deploy/
docker-compose stop backend nginx

# 2. 恢复 jar
cp backups/20260604_120000/platform-api-0.0.1-SNAPSHOT.jar ./

# 3. 恢复前端
rm -rf frontend/merchant-web/dist
cp -r backups/20260604_120000/frontend/merchant-web/dist frontend/merchant-web/

# 4. 恢复数据库
docker-compose exec -T mysql mysql -u root -p < backups/20260604_120000/database_backup.sql

# 5. 启动服务
docker-compose start backend nginx
```

### 数据库备份

```bash
# 手动备份
docker-compose -f deploy/docker-compose.yml exec -T mysql \
    mysqldump -u root -p puyuan_ai_mvp \
    --single-transaction --routines --triggers \
    > backup_$(date +%Y%m%d_%H%M%S).sql
```

---

## 常见问题

### Q: Flyway 迁移失败怎么办？

1. 检查 `flyway_schema_history` 表，确认当前版本
2. 修正迁移脚本中的错误
3. 如果脚本已部分执行，手动修复数据库状态
4. 修复后重新启动应用

### Q: 部署后前端页面空白？

1. 检查 Nginx 配置中静态文件路径是否正确
2. 确认 `frontend/*/dist` 目录存在且有文件
3. 检查浏览器控制台是否有 404 错误
4. 确认前端构建时 API 地址配置正确

### Q: 健康检查一直失败？

1. 查看后端日志: `docker-compose logs backend`
2. 确认数据库连接: MySQL 容器是否 running
3. 确认 Redis 连接: Redis 容器是否 running
4. 检查环境变量: `.env` 文件是否正确

### Q: 如何添加新的环境变量？

1. 在 `deploy/.env.example` 中添加新变量
2. 在 `deploy/docker-compose.yml` 的 backend 服务中添加映射
3. 在后端 `application-docker.yml` 中添加读取
4. 更新 `DEPLOYMENT_CHECKLIST.md` 中的环境变量检查项

### Q: 备份文件占用太多磁盘空间？

部署脚本默认保留最近 5 次备份，自动清理旧备份。
也可以手动清理: `rm -rf deploy/backups/20260601_*`
