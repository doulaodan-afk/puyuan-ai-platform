# 生产环境部署指南

## 🚀 快速开始

### 前置要求
- Ubuntu 20.04 LTS 系统
- 至少 4GB RAM
- 至少 50GB 硬盘空间
- 服务器公网 IP

### 一键部署

```bash
# 1. 登录服务器
ssh root@your-server-ip

# 2. 克隆项目
git clone <project-repo> ~/puyuanmaoshan
cd ~/puyuanmaoshan

# 3. 执行部署脚本
chmod +x deploy.sh
./deploy.sh
```

脚本会自动完成：
- ✅ 安装 Docker & Docker Compose
- ✅ 安装 Java 17
- ✅ 构建后端应用
- ✅ 启动 MySQL、Redis、后端、Nginx
- ✅ 构建并部署前端

---

## 📋 手动部署步骤

### 步骤 1: 安装 Docker

```bash
# 更新系统
sudo apt-get update
sudo apt-get upgrade -y

# 安装依赖
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 设置仓库
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证
docker --version
```

### 步骤 2: 安装 Java 17

```bash
sudo apt-get install -y openjdk-17-jdk

# 验证
java -version
```

### 步骤 3: 准备项目

```bash
# 克隆项目
git clone <project-repo> ~/puyuanmaoshan
cd ~/puyuanmaoshan

# 创建生产配置
cat > .env.prod << 'EOF'
MYSQL_ROOT_PASSWORD=your_secure_password_123
MYSQL_PASSWORD=your_secure_password_456
MYSQL_DATABASE=puyuan_ai_mvp
MYSQL_USER=puyuan_user
REDIS_PASSWORD=your_secure_password_789
EOF
```

⚠️ **重要**：修改所有密码为强密码！

### 步骤 4: 构建后端

```bash
cd backend/java-spring
mvn clean package -Pprod -DskipTests
docker build -t puyuan-api:latest .
cd ../..
```

### 步骤 5: 启动服务

```bash
docker-compose up -d --env-file .env.prod

# 查看启动日志
docker-compose logs -f backend

# 等待服务启动（约 30 秒）
```

### 步骤 6: 构建并部署前端

```bash
# 商家端
cd frontend/merchant-web
npm install
npm run build
cd ../..

# 管理端
cd frontend/admin-web
npm install
npm run build
cd ../..
```

---

## 🔐 配置 HTTPS（推荐）

### 使用 Let's Encrypt 免费证书

```bash
# 安装 Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# 生成证书
sudo certbot certonly --standalone -d ai.puyuanmaoshan.com

# 更新 Nginx 配置支持 HTTPS（见下文）
```

### 更新 Nginx 配置

编辑 `nginx/nginx.conf`：

```nginx
# 重定向 HTTP 到 HTTPS
server {
    listen 80;
    server_name ai.puyuanmaoshan.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 服务
server {
    listen 443 ssl http2;
    server_name ai.puyuanmaoshan.com;

    ssl_certificate /etc/letsencrypt/live/ai.puyuanmaoshan.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ai.puyuanmaoshan.com/privkey.pem;

    # ... 其他配置
}
```

重启 Nginx：

```bash
docker-compose exec nginx nginx -s reload
```

---

## 📊 服务管理

### 查看状态

```bash
# 查看所有容器
docker-compose ps

# 查看某个容器日志
docker-compose logs -f backend
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 常见命令

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重启特定服务
docker-compose restart backend

# 查看 MySQL 数据
docker-compose exec mysql mysql -uroot -p puyuan_ai_mvp

# 查看 Redis 数据
docker-compose exec redis redis-cli -a <REDIS_PASSWORD>
```

### 备份数据库

```bash
# 备份 MySQL
docker-compose exec mysql mysqldump -uroot -p puyuan_ai_mvp > backup_$(date +%Y%m%d).sql

# 恢复 MySQL
docker-compose exec -T mysql mysql -uroot -p puyuan_ai_mvp < backup_20260602.sql
```

### 更新应用

```bash
# 更新代码
git pull

# 重新构建后端
cd backend/java-spring
mvn clean package -Pprod -DskipTests
docker build -t puyuan-api:latest .
cd ../..

# 重新部署
docker-compose up -d --build backend

# 重新构建前端（如需）
cd frontend/merchant-web && npm run build && cd ../..
cd frontend/admin-web && npm run build && cd ../..

# 重启 Nginx
docker-compose restart nginx
```

---

## 🧪 测试

### 检查后端 API

```bash
# 获取 API 文档
curl http://your-server-ip/swagger-ui.html

# 获取健康状态
curl http://your-server-ip/api/v1/system/health
```

### 检查前端

```bash
# 商家端
http://your-server-ip/merchant

# 管理端
http://your-server-ip/admin
```

---

## 🐛 故障排查

### 问题 1: 数据库连接失败

```bash
# 检查 MySQL 容器
docker-compose logs mysql

# 检查 MySQL 健康状态
docker-compose exec mysql mysqladmin ping -h localhost -u root -p<password>
```

### 问题 2: 后端启动失败

```bash
# 查看详细日志
docker-compose logs -f backend

# 检查依赖
docker-compose ps
```

### 问题 3: 前端加载失败

```bash
# 检查 Nginx 容器
docker-compose logs nginx

# 检查前端文件是否存在
docker-compose exec nginx ls -la /usr/share/nginx/html/
```

---

## 📱 监控和告警

### CPU/内存监控

```bash
# 查看容器资源使用
docker stats

# 查看详细信息
docker-compose ps -a
```

### 日志聚合（可选）

考虑使用 ELK Stack、Prometheus 等工具进行日志和监控。

---

## 🔒 安全建议

1. **修改所有默认密码**
   - MySQL root 密码
   - Redis 访问密码
   - 防火墙规则

2. **启用 HTTPS**
   - 使用 Let's Encrypt 免费证书
   - 配置自动续期

3. **配置防火墙**
   ```bash
   sudo ufw enable
   sudo ufw allow 22/tcp
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   ```

4. **定期备份**
   - 数据库备份
   - 应用配置备份
   - 日志文件备份

5. **访问控制**
   - 限制 MySQL/Redis 访问（仅内部网络）
   - 配置 API 认证
   - 使用 VPN 访问管理界面

---

## 📞 支持

如遇问题，请：
1. 查看容器日志：`docker-compose logs -f`
2. 检查配置文件：`.env.prod`、`application-prod.yml`
3. 验证网络连接：`ping`、`telnet`
4. 重启服务：`docker-compose restart`

