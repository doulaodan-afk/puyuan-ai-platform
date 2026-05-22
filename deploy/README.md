# 濮院毛衫 AI 平台 - 生产环境部署指南

## 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [SSL 证书配置](#ssl-证书配置)
- [日常运维](#日常运维)

## 环境要求

### 服务器要求

- **操作系统**: Linux (推荐 Ubuntu 20.04+ 或 CentOS 7+)
- **CPU**: 2 核心以上
- **内存**: 4GB 以上
- **磁盘**: 20GB 以上可用空间

### 软件要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Git**: 用于代码拉取
- **域名**: 已解析到服务器 IP

## 快速开始

### 1. 克隆代码

```bash
git clone https://github.com/your-org/puyuan-ai-platform.git
cd puyuan-ai-platform
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置文件
nano .env
```

### 3. 一键部署

```bash
# 进入部署目录
cd deploy

# 赋予执行权限
chmod +x deploy.sh certbot-setup.sh

# 执行部署
./deploy.sh
```

### 4. 配置 SSL 证书（生产环境）

```bash
# 配置 SSL 证书
sudo ./certbot-setup.sh

# 更新 .env 中的域名
# CERTBOT_DOMAIN=your-domain.com
# CERTBOT_EMAIL=admin@your-domain.com
```

## 配置说明

### 环境变量（.env）

| 变量名 | 说明 | 默认值 | 必填 |
|---------|------|---------|--------|
| MYSQL_ROOT_PASSWORD | MySQL root 密码 | - | ✅ |
| MYSQL_DATABASE | 数据库名 | puyuan_ai_prod | ✅ |
| MYSQL_USER | 数据库用户 | puyuan_user | ✅ |
| MYSQL_PASSWORD | 数据库密码 | - | ✅ |
| REDIS_PASSWORD | Redis 密码 | - | ✅ |
| CONTENT_SECURITY_ENABLED | 内容安全检查 | true | ❌ |
| WX_MINIAPP_APPID | 微信小程序 AppID | - | ❌ |
| WX_MINIAPP_SECRET | 微信小程序 Secret | - | ❌ |
| AI_MOCK_ENABLED | AI Mock 模式 | false | ❌ |
| OPENAI_API_KEY | OpenAI API Key | - | ❌ |

### 端口说明

| 服务 | 内部端口 | 外部端口 | 说明 |
|-----|---------|---------|------|
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| Backend | 8080 | - | 后端服务 |
| Nginx HTTP | - | 80 | HTTP（重定向）|
| Nginx HTTPS | - | 443 | HTTPS |

## SSL 证书配置

### 使用 Let's Encrypt（推荐）

Let's Encrypt 提供免费 SSL 证书，90 天自动续期。

#### 配置步骤

1. **确保域名解析正确**

```bash
# 检查域名解析
ping your-domain.com
```

2. **配置防火墙规则**

```bash
# Ubuntu
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# CentOS
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --reload
```

3. **运行证书配置脚本**

```bash
cd deploy

# 更新 .env 中的域名配置
CERTBOT_DOMAIN=your-domain.com
CERTBOT_EMAIL=admin@your-domain.com

# 运行配置脚本
sudo ./certbot-setup.sh
```

4. **验证证书**

```bash
# 重启 Nginx
docker-compose restart nginx

# 检查证书状态
docker-compose logs nginx

# 访问 HTTPS 测试
curl -I https://your-domain.com
```

### 使用自有证书

如果使用自己购买的 SSL 证书：

1. 将证书文件放到 `nginx/ssl/` 目录：
   - `fullchain.pem` - 证书链文件
   - `privkey.pem` - 私钥文件

2. 重启 Nginx：
```bash
docker-compose restart nginx
```

## 日常运维

### 查看日志

```bash
# 所有服务日志
docker-compose logs -f

# 特定服务日志
docker-compose logs -f backend
docker-compose logs -f nginx

# 日志文件位置
tail -f nginx/logs/access.log
tail -f nginx/logs/error.log
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend
docker-compose restart nginx
```

### 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止特定服务
docker-compose stop backend
```

### 更新代码

```bash
# 拉取最新代码
git pull origin main

# 重新构建镜像
docker-compose build

# 重启服务
docker-compose up -d
```

### 数据备份

```bash
# 备份 MySQL 数据
docker-compose exec mysql mysqldump -u${MYSQL_USER} -p${MYSQL_PASSWORD} \
    ${MYSQL_DATABASE} > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份 Redis 数据（可选）
docker-compose exec redis redis-cli -a ${REDIS_PASSWORD} BGSAVE

# 备份到远程服务器
scp backup_*.sql user@backup-server:/backups/
```

### 监控服务健康

```bash
# 检查所有服务状态
docker-compose ps

# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查 Nginx 状态
curl -I http://localhost/
curl -I https://your-domain.com/
```

## 性能优化

### Nginx 已配置优化

- ✅ Gzip 压缩
- ✅ 静态文件缓存（30 天）
- ✅ API 限流（插件调用：60/min，通用：10/s）
- ✅ HTTP/2 支持
- ✅ 安全响应头

### 后端已配置优化

- ✅ Redis 缓存（租户插件列表、定价配置、系统配置）
- ✅ 令牌桶限流算法
- ✅ JVM 优化参数

## 故障排查

### 常见问题

**1. 服务无法启动**

```bash
# 查看详细日志
docker-compose logs backend
docker-compose logs mysql
docker-compose logs redis

# 检查端口占用
netstat -tulpn | grep :8080
netstat -tulpn | grep :443
```

**2. 数据库连接失败**

```bash
# 检查 MySQL 是否正常启动
docker-compose exec mysql mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e "SELECT 1"

# 检查网络连接
docker-compose exec backend ping mysql
```

**3. SSL 证书过期**

```bash
# 手动续期
./certbot-renew.sh

# 重启 Nginx
docker-compose restart nginx
```

**4. 磁盘空间不足**

```bash
# 检查磁盘使用
df -h

# 清理 Docker 镜像
docker image prune -a

# 清理 Docker 卷（慎用）
docker volume prune
```

## 安全建议

1. **定期更新密码**：建议每 3 个月更新一次数据库和 Redis 密码
2. **启用内容安全**：生产环境建议开启内容安全过滤
3. **配置防火墙**：只开放必要的端口（80, 443）
4. **定期备份**：建议每天自动备份数据库
5. **监控告警**：配置服务监控和告警通知

## 技术支持

如有问题，请联系：
- 邮箱：support@puyuanmaoshan.com
- 文档：https://docs.puyuanmaoshan.com
- GitHub Issues：https://github.com/your-org/puyuan-ai-platform/issues
