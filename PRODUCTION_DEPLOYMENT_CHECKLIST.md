# 濮院毛衫 AI 平台 - 生产部署清单

## 环境准备

### 服务器要求
- [ ] CPU: 4核及以上
- [ ] 内存: 8GB 及以上
- [ ] 磁盘: 100GB 及以上 SSD
- [ ] 操作系统: Ubuntu 22.04 LTS / CentOS 8+

### 基础软件
- [ ] Java 21 (OpenJDK 或 Oracle JDK)
- [ ] Node.js 18+
- [ ] MySQL 8.0+
- [ ] Redis 7.0+
- [ ] Nginx 1.24+
- [ ] Docker (可选，用于容器化部署)

## 数据库准备

### MySQL 配置
```ini
[mysqld]
max_connections = 500
innodb_buffer_pool_size = 4G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
```

### 数据库初始化
```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE puyuan_ai_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 创建用户
mysql -u root -p -e "CREATE USER 'puyuan'@'%' IDENTIFIED BY 'STRONG_PASSWORD_HERE';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON puyuan_ai_prod.* TO 'puyuan'@'%';"
mysql -u root -p -e "FLUSH PRIVILEGES;"

# 导入生产数据结构
mysql -u puyuan -p puyuan_ai_prod < backend/sql/schema.sql
mysql -u puyuan -p puyuan_ai_prod < backend/sql/system_config_migration.sql
```

### Redis 配置
```conf
# /etc/redis/redis.conf
bind 0.0.0.0
port 6379
requirepass YOUR_REDIS_PASSWORD
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
appendonly yes
```

## 后端部署

### 1. 构建应用
```bash
cd backend/java-spring

# 设置生产环境变量
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=your-db-host
export DB_PORT=3306
export DB_NAME=puyuan_ai_prod
export DB_USER=puyuan
export DB_PASSWORD=your-db-password

# 构建
mvn clean package -DskipTests -Pprod
```

### 2. 配置文件 (application-prod.yml)
```yaml
server:
  port: 8080
  shutdown: graceful
  tomcat:
    threads:
      max: 200
      min-spare: 10

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-wait: -1ms
          max-idle: 10
          min-idle: 5

spring.ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: ${OPENAI_BASE_URL:https://api.openai.com}
    chat:
      enabled: true
      options:
        model: gpt-4o
        temperature: 0.7
        max-tokens: 2000
    image:
      enabled: true
      options:
        model: dall-e-3
        size: 1024x1024
        n: 1

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: platform-api
      environment: production

logging:
  level:
    com.puyuanmaoshan.platform: INFO
  file:
    name: /var/log/puyuan/platform-api.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
```

### 3. 使用 systemd 管理
```ini
# /etc/systemd/system/platform-api.service
[Unit]
Description=Platform API Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=puyuan
WorkingDirectory=/opt/puyuan/platform
Environment="SPRING_PROFILES_ACTIVE=prod"
EnvironmentFile=/opt/puyuan/platform/.env
ExecStart=/usr/bin/java -jar platform-api-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable platform-api
systemctl start platform-api
systemctl status platform-api
```

## 前端部署

### 1. 构建管理后台 (admin-web)
```bash
cd frontend/admin-web
npm run build
```

### 2. 构建租户端 (merchant-web)
```bash
cd frontend/merchant-web
npm run build
```

### 3. Nginx 配置
```nginx
# /etc/nginx/conf.d/puyuan.conf

# 管理后台
server {
    listen 80;
    server_name admin.puyuanmaoshan.com;

    root /var/www/puyuan/admin-web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator/ {
        # 仅允许内网访问
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        deny all;
        proxy_pass http://127.0.0.1:8080;
    }
}

# 租户端
server {
    listen 80;
    server_name app.puyuanmaoshan.com;

    root /var/www/puyuan/merchant-web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```bash
nginx -t
systemctl reload nginx
```

## SSL 配置 (Let's Encrypt)
```bash
# 安装 certbot
apt install certbot python3-certbot-nginx

# 获取证书
certbot --nginx -d admin.puyuanmaoshan.com -d app.puyuanmaoshan.com

# 自动续期
certbot renew --dry-run
```

## 监控和日志

### Prometheus 配置
```yaml
# /etc/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'platform-api'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
        labels:
          environment: production
```

### Grafana Dashboard
- [ ] 导入 JVM 面板
- [ ] 导入 MySQL 面板
- [ ] 导入 Redis 面板
- [ ] 创建业务指标面板（AI 调用成功率、OSS 切换次数等）

### 日志收集 (可选 - ELK)
```yaml
# Logback 配置输出到 Filebeat
<appender name="FILEBEAT" class="ch.qos.logback.core.FileAppender">
    <file>/var/log/puyuan/platform-api.log</file>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

## 备份策略

### 数据库备份
```bash
# 每日全量备份
0 2 * * * /opt/scripts/backup-db.sh

# /opt/scripts/backup-db.sh
#!/bin/bash
BACKUP_DIR=/backup/mysql
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u puyuan -p${DB_PASSWORD} puyuan_ai_prod | gzip > ${BACKUP_DIR}/puyuan_${DATE}.sql.gz
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -delete
```

### 应用备份
```bash
# 每周备份 JAR 包
0 3 * * 0 /opt/scripts/backup-app.sh

# /opt/scripts/backup-app.sh
#!/bin/bash
cp /opt/puyuan/platform/platform-api-0.0.1-SNAPSHOT.jar /backup/app/platform-api_$(date +%Y%m%d).jar
```

## 安全检查

- [ ] 更改所有默认密码
- [ ] 配置防火墙规则 (ufw / iptables)
- [ ] 限制 MySQL 和 Redis 远程访问
- [ ] 启用 SSL/TLS
- [ ] 配置 rate limiting
- [ ] 启用请求日志审计
- [ ] 定期安全扫描

## 性能优化

- [ ] MySQL 慢查询分析
- [ ] Redis 慢日志配置
- [ ] JVM 参数调优
- [ ] Nginx gzip 压缩
- [ ] CDN 配置 (静态资源)
- [ ] 数据库索引优化

## 上线前最终检查

- [ ] 所有环境变量已配置
- [ ] 数据库迁移已执行
- [ ] Redis 已启动并可连接
- [ ] SSL 证书有效
- [ ] 监控告警已配置
- [ ] 备份任务已启用
- [ ] 日志轮转已配置
- [ ] 健康检查端点正常
- [ ] 关键接口测试通过
- [ ] 回滚方案已准备
