# 生产环境部署检查清单

## 一、后端环境变量

### 必需配置项

| 环境变量 | 说明 | 示例 |
|---------|------|------|
| `SPRING_DATASOURCE_URL` | MySQL 连接地址 | `jdbc:mysql://db.example.com:3306/puyuan_ai_prod` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `puyuan_user` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `your_secure_password` |
| `WX_MINIAPP_APPID` | 微信小程序 AppID | `wx1234567890abcdef` |
| `WX_MINIAPP_SECRET` | 微信小程序 Secret | `abcdef1234567890` |
| `WX_PAYMENT_MCHID` | 微信支付商户号 | `1234567890` |
| `WX_PAYMENT_API_V3_KEY` | 微信支付 API v3 密钥 | `your_api_v3_key_here` |
| `WX_PAYMENT_NOTIFY_URL` | 支付回调地址 | `https://api.puyuan-ai.com/api/v1/payment/wx/notify` |
| `WX_PAYMENT_MOCK_ENABLED` | 是否启用 Mock 支付 | `false`（生产环境必须为 false）|
| `OPENAI_API_KEY` | OpenAI API Key | `sk-xxxxx` |
| `OPENAI_BASE_URL` | OpenAI API 地址 | `https://api.openai.com` |
| `AI_MOCK_ENABLED` | 是否启用 Mock 模式 | `false`（生产环境必须为 false）|

### 可选配置项

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 无 |
| `OSS_ACCESS_KEY_ID` | OSS 访问密钥 ID | 无 |
| `OSS_ACCESS_KEY_SECRET` | OSS 访问密钥 | 无 |
| `OSS_ENDPOINT` | OSS 终点 | 无 |
| `OSS_BUCKET` | OSS 存储桶 | 无 |

---

## 二、Nginx 配置

### 完整配置示例

```nginx
# /etc/nginx/conf.d/puyuan-ai.conf

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name api.puyuan-ai.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 服务器
server {
    listen 443 ssl http2;
    server_name api.puyuan-ai.com;

    # SSL 证书配置
    ssl_certificate /path/to/ssl/cert.pem;
    ssl_certificate_key /path/to/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 日志配置
    access_log /var/log/nginx/puyuan-ai-access.log;
    error_log /var/log/nginx/puyuan-ai-error.log;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 限流配置
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
    limit_req zone=api_limit burst=200 nodelay;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript
               application/json application/javascript application/xml+rss
               application/rss+xml font/truetype font/opentype
               application/vnd.ms-fontobject image/svg+xml;

    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket 支持
    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 静态文件（如果有前端静态文件）
    location / {
        root /var/www/puyuan-ai/web;
        try_files $uri $uri/ /index.html;
    }
}

# 小程序静态文件服务器
server {
    listen 443 ssl http2;
    server_name miniapp.puyuan-ai.com;

    ssl_certificate /path/to/ssl/cert.pem;
    ssl_certificate_key /path/to/ssl/key.pem;

    root /var/www/puyuan-ai/miniapp;

    # 小程序安全校验文件
    location /MP_verify_*.txt {
        root /var/www/puyuan-ai/verify;
    }
}
```

---

## 三、微信小程序后台配置

### 1. 基本信息配置

| 配置项 | 值 |
|--------|-----|
| 小程序名称 | 濮院毛衫 AI 平台 |
| 小程序简介 | 为濮院毛衫商家提供 AI 工具服务 |
| 服务类目 | 工具 > 办公 |
| 小程序头像 | 上传 Logo |

### 2. 服务器域名配置

| 域名类型 | 域名 |
|---------|------|
| request 合法域名 | `https://api.puyuan-ai.com` |
| socket 合法域名 | `wss://api.puyuan-ai.com` |
| uploadFile 合法域名 | `https://api.puyuan-ai.com` |
| downloadFile 合法域名 | `https://api.puyuan-ai.com` |

### 3. 业务域名配置

| 域名 | 说明 |
|------|------|
| `https://api.puyuan-ai.com` | 请求域名 |

### 4. 本地设置

| 配置项 | 值 |
|--------|-----|
| 不校验合法域名 | 关闭（生产环境） |
| 调试基础库 | 关闭（生产环境） |

### 5. 服务器域名白名单

将服务器 IP 添加到白名单，确保能够访问微信接口。

### 6. 支付配置

#### 微信支付商户平台配置

| 配置项 | 说明 |
|--------|------|
| 商户号 | 与环境变量 `WX_PAYMENT_MCHID` 一致 |
| API 证书 | 上传 API 证书（p12 格式） |
| API 密钥 | 与环境变量 `WX_PAYMENT_API_V3_KEY` 一致 |
| 回调地址 | `https://api.puyuan-ai.com/api/v1/payment/wx/notify` |
| 授权目录 | `/api/v1/payment/wx/` |

#### 产品配置

| 产品 | 说明 |
|------|------|
| JSAPI 支付 | 小程序支付 |
| Native 支付 | 暂不需要 |
| H5 支付 | 暂不需要 |

---

## 四、数据库配置

### 1. 生产数据库设置

```sql
-- 创建生产数据库
CREATE DATABASE puyuan_ai_prod
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 创建生产数据库用户
CREATE USER 'puyuan_prod'@'%' IDENTIFIED BY 'strong_password_here';

-- 授予权限
GRANT ALL PRIVILEGES ON puyuan_ai_prod.* TO 'puyuan_prod'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

### 2. 数据库备份脚本

```bash
#!/bin/bash
# /opt/scripts/backup-db.sh

# 配置
DB_HOST="db.example.com"
DB_PORT="3306"
DB_NAME="puyuan_ai_prod"
DB_USER="backup_user"
DB_PASS="backup_password"
BACKUP_DIR="/opt/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/puyuan_ai_prod_$DATE.sql.gz"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS \
  --single-transaction \
  --quick \
  --lock-tables=false \
  --routines \
  --triggers \
  --events \
  $DB_NAME | gzip > $BACKUP_FILE

# 删除 7 天前的备份
find $BACKUP_DIR -name "puyuan_ai_prod_*.sql.gz" -mtime +7 -delete

echo "备份完成: $BACKUP_FILE"
```

### 3. 数据库恢复脚本

```bash
#!/bin/bash
# /opt/scripts/restore-db.sh

if [ -z "$1" ]; then
  echo "用法: $0 <备份文件路径>"
  exit 1
fi

BACKUP_FILE=$1
DB_HOST="db.example.com"
DB_PORT="3306"
DB_NAME="puyuan_ai_prod"
DB_USER="puyuan_prod"
DB_PASS="production_password"

# 解压并恢复
gunzip < $BACKUP_FILE | mysql -h$DB_HOST -P$DB_PORT \
  -u$DB_USER -p$DB_PASS $DB_NAME

echo "恢复完成"
```

### 4. 定时备份配置（Cron）

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点执行备份
0 2 * * * /opt/scripts/backup-db.sh >> /var/log/backup.log 2>&1
```

---

## 五、日志收集方案

### 方案一：ELK Stack

#### Elasticsearch 配置

```yaml
# /etc/elasticsearch/elasticsearch.yml
cluster.name: puyuan-ai
network.host: 0.0.0.0
http.port: 9200
path.data: /var/lib/elasticsearch
```

#### Logstash 配置

```conf
# /etc/logstash/conf.d/puyuan-ai.conf
input {
  file {
    path => "/var/log/puyuan-ai/*.log"
    start_position => "beginning"
    sincedb_path => "/dev/null"
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:logger}" }
  }
  date {
    match => [ "timestamp", "ISO8601" ]
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "puyuan-ai-%{+YYYY.MM.dd}"
  }
  stdout { codec => rubydebug }
}
```

#### Kibana 配置

```yaml
# /etc/kibana/kibana.yml
server.port: 5601
elasticsearch.hosts: ["http://localhost:9200"]
```

### 方案二：阿里云日志服务

#### 后端应用集成

```xml
<!-- pom.xml -->
<dependency>
  <groupId>com.aliyun.openservices</groupId>
  <artifactId>aliyun-log</artifactId>
  <version>0.6.16</version>
</dependency>
```

```yaml
# logback-spring.xml
<configuration>
  <appender name="AliyunLogAppender" class="com.aliyun.openservices.log.logback.LoghubAppender">
    <appName>puyuan-ai</appName>
    <endpoint>your-logstore-endpoint</endpoint>
    <accessKeyId>your-access-key-id</accessKeyId>
    <accessKeySecret>your-access-key-secret</accessKeySecret>
  </appender>

  <root level="info">
    <appender-ref ref="AliyunLogAppender" />
  </root>
</configuration>
```

### 方案三：文件日志 + ELK 导出

```bash
#!/bin/bash
# /opt/scripts/export-logs.sh

DATE=$(date +%Y%m%d)
LOG_DIR="/var/log/puyuan-ai"
EXPORT_DIR="/opt/exports"

# 导出前一天日志
cat $LOG_DIR/platform-api-$DATE.log | \
  grep -v "health\|metrics" > \
  $EXPORT_DIR/platform-api-$DATE.log

echo "日志导出完成"
```

---

## 六、安全检查清单

### 1. 基础安全

- [ ] 所有密码使用强密码（至少 12 位，包含大小写字母、数字、特殊字符）
- [ ] 禁用 root 账户远程登录
- [ ] 更新系统到最新版本
- [ ] 安装防火墙（如 firewalld）
- [ ] 只开放必要端口（80, 443, 22）

### 2. 应用安全

- [ ] 启用 HTTPS（强制跳转）
- [ ] 配置安全 HTTP 头
- [ ] 启用 CORS 白名单
- [ ] 配置 SQL 注入防护
- [ ] 配置 XSS 防护
- [ ] 配置 CSRF 防护

### 3. 数据安全

- [ ] 数据库密码定期更换（每 90 天）
- [ ] 敏感数据加密存储
- [ ] 定期数据备份
- [ ] 备份数据异地存储
- [ ] 数据库访问权限最小化

### 4. API 安全

- [ ] 启用 API 限流
- [ ] 配置请求签名验证
- [ ] 启用 API 日志记录
- [ ] 配置 API 密钥管理
- [ ] 禁用调试接口

### 5. 小程序安全

- [ ] 关闭调试模式
- [ ] 配置合法域名白名单
- [ ] 启用用户隐私保护指引
- [ ] 配置服务器域名白名单
- [ ] 上传 MP_verify_*.txt 文件

---

## 七、监控配置

### 1. 应用监控

```yaml
# Prometheus 配置
scrape_configs:
  - job_name: 'puyuan-ai'
    scrape_interval: 30s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 2. 服务器监控

- [ ] CPU 监控（报警阈值：> 80%）
- [ ] 内存监控（报警阈值：> 85%）
- [ ] 磁盘监控（报警阈值：> 85%）
- [ ] 网络流量监控

### 3. 数据库监控

- [ ] 连接数监控
- [ ] 查询性能监控
- [ ] 慢查询报警
- [ ] 主从复制状态

### 4. 业务监控

- [ ] 接口成功率（报警阈值：< 99%）
- [ ] 接口响应时间（报警阈值：> 1s）
- [ ] 支付成功率（报警阈值：< 99%）
- [ ] Token 充值量监控

---

## 八、部署流程

### 1. 预部署检查

- [ ] 所有环境变量已配置
- [ ] 数据库已创建并初始化
- [ ] SSL 证书已准备
- [ ] DNS 域名已解析
- [ ] Nginx 配置已更新
- [ ] 防火墙规则已配置
- [ ] 监控系统已部署

### 2. 部署步骤

```bash
# 1. 备份当前版本
ssh user@server "cd /opt/puyuan-ai && ./scripts/backup-current.sh"

# 2. 构建新版本
cd backend/java-spring
mvn clean package -DskipTests

# 3. 上传部署包
scp target/platform-api-0.0.1-SNAPSHOT.jar user@server:/opt/puyuan-ai/

# 4. 上传并部署到服务器
ssh user@server
cd /opt/puyuan-ai

# 5. 停止当前服务
systemctl stop puyuan-ai

# 6. 备份旧版本
mv platform-api-0.0.1-SNAPSHOT.jar platform-api-backup.jar

# 7. 启动新服务
systemctl start puyuan-ai

# 8. 检查服务状态
systemctl status puyuan-ai

# 9. 检查日志
tail -f /var/log/puyuan-ai/platform-api.log
```

### 3. 部署后验证

- [ ] 访问健康检查接口：`https://api.puyuan-ai.com/actuator/health`
- [ ] 访问接口文档：`https://api.puyuan-ai.com/swagger-ui.html`
- [ ] 测试登录接口
- [ ] 测试充值接口
- [ ] 测试 AI 工具接口
- [ ] 检查日志无 ERROR
- [ ] 检查监控无告警

---

## 九、回滚计划

### 回滚步骤

```bash
# 1. 停止当前服务
systemctl stop puyuan-ai

# 2. 恢复备份版本
cd /opt/puyuan-ai
mv platform-api-backup.jar platform-api-0.0.1-SNAPSHOT.jar

# 3. 启动服务
systemctl start puyuan-ai

# 4. 检查服务状态
systemctl status puyuan-ai
```

### 数据库回滚

```bash
# 恢复到指定备份
/opt/scripts/restore-db.sh /opt/backups/puyuan_ai_prod_20250520_020000.sql.gz
```

---

## 十、联系方式

### 应急联系人

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 技术负责人 | xxx | 138xxxxxxx | tech@example.com |
| 运维负责人 | xxx | 139xxxxxxx | ops@example.com |
| 产品负责人 | xxx | 137xxxxxxx | product@example.com |

### 服务商联系

| 服务商 | 联系方式 |
|--------|----------|
| 云服务提供商 | xxx@example.com |
| 域名提供商 | xxx@example.com |
| 微信支付客服 | 微信商户平台 |
