# 濮院毛衫 AI 平台 - 生产部署检查清单

## 环境变量配置

### 必须设置的环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `CRYPTO_SECRET_KEY` | AES 加密密钥（至少 32 字符） | `your-super-secret-key-here-min-32-chars` |
| `DB_HOST` | 数据库主机 | `prod-db.example.com` |
| `DB_PORT` | 数据库端口 | `3306` |
| `DB_NAME` | 数据库名称 | `puyuan_ai_prod` |
| `DB_USER` | 数据库用户名 | `platform_user` |
| `DB_PASSWORD` | 数据库密码 | `your-secure-password` |
| `REDIS_HOST` | Redis 主机 | `redis.prod.example.com` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码（如需要） | `your-redis-password` |
| `OPENAI_API_KEY` | OpenAI API Key（备用） | `sk-proj-...` |
| `OPENAI_BASE_URL` | OpenAI API 基础 URL | `https://api.openai.com` |

### 可选环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AI_MOCK_ENABLED` | 是否启用 Mock 模式 | `false`（生产环境） |
| `CRYPTO_SECRET_KEY` | 见上方（必须） | 无 |

## 部署前检查清单

- [ ] 数据库已创建并执行迁移脚本
- [ ] Redis 服务已启动且可连接
- [ ] `CRYPTO_SECRET_KEY` 已设置为强密钥
- [ ] 所有 AI API Key 已通过管理后台配置
- [ ] OSS 配置已通过管理后台配置
- [ ] 日志目录已创建并有写权限
- [ ] 备份策略已配置
- [ ] 监控告警已配置
- [ ] SSL 证书已配置（如使用 HTTPS）
- [ ] 防火墙规则已设置

## Docker 部署

### Dockerfile

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/platform-api-0.0.1-SNAPSHOT.jar app.jar

# 添加运行用户（安全最佳实践）
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JVM 优化参数
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:+UseStringDeduplication"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### docker-compose.yml（生产模板）

```yaml
version: '3.8'

services:
  # 应用服务
  platform-api:
    image: puyuan-maoshan/platform-api:latest
    container_name: platform-api
    restart: always
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=puyuan_ai_prod
      - DB_USER=${DB_USER}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - CRYPTO_SECRET_KEY=${CRYPTO_SECRET_KEY}
      - AI_MOCK_ENABLED=false
    depends_on:
      - mysql
      - redis
    networks:
      - app-network
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"

  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: mysql
    restart: always
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=puyuan_ai_prod
      - MYSQL_USER=${DB_USER}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/sql/init:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    networks:
      - app-network
    command: --default-authentication-plugin=mysql_native_password

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: redis
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    networks:
      - app-network

  # Prometheus 监控
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    restart: always
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - app-network

  # Grafana 可视化
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    restart: always
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - app-network

volumes:
  mysql-data:
  redis-data:
  prometheus-data:
  grafana-data:

networks:
  app-network:
    driver: bridge
```

### .env 文件模板

```bash
# 数据库配置
DB_ROOT_PASSWORD=your-root-password
DB_USER=platform_user
DB_PASSWORD=your-db-password

# Redis 配置
REDIS_PASSWORD=your-redis-password

# 加密密钥（必须修改！）
CRYPTO_SECRET_KEY=your-super-secret-key-here-min-32-chars

# Grafana 密码
GRAFANA_PASSWORD=your-grafana-password
```

## Nginx 限流配置

### 针对 /api/plugin/invoke/* 接口的限流

```nginx
# 限流配置
limit_req_zone $binary_remote_addr zone=plugin_invoke_limit:10m rate=10r/s;
limit_conn_zone $binary_remote_addr zone=plugin_conn_limit:10m rate=20;

upstream backend {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name api.puyuanmaoshan.com;

    # 插件调用接口限流
    location /api/plugin/invoke/ {
        # 限制每个 IP 每秒最多 10 个请求
        limit_req zone=plugin_invoke_limit burst=20 nodelay;

        # 限制每个 IP 最多 20 个并发连接
        limit_conn zone=plugin_conn_limit 20;

        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 限流返回状态
        limit_req_status 429;
        limit_conn_status 429;
    }

    # 其他接口常规限流
    location /api/ {
        limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
        limit_req zone=api_limit burst=50 nodelay;

        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 健康检查不限制
    location /actuator/health {
        proxy_pass http://backend;
        access_log off;
    }

    # Prometheus 端点（仅内网访问）
    location /actuator/prometheus {
        allow 10.0.0.0/8;
        deny all;
        proxy_pass http://backend;
    }
}
```

## Logback 日志配置

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="/var/log/platform-api"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 文件输出 - 应用日志 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 文件输出 - 错误日志 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>90</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 异步日志 -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <appender name="ASYNC_ERROR_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="ERROR_FILE"/>
        <queueSize>256</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
        <appender-ref ref="ASYNC_ERROR_FILE"/>
    </root>

    <!-- 特定包的日志级别 -->
    <logger name="com.puyuanmaoshan.platform" level="INFO"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="com.baomidou.mybatisplus" level="WARN"/>
    <logger name="org.springframework.cache" level="INFO"/>
</configuration>
```

## 数据库备份脚本

### backup.sh

```bash
#!/bin/bash

# 数据库备份脚本
# 添加到 crontab: 0 2 * * * /path/to/backup.sh

BACKUP_DIR="/var/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="puyuan_ai_prod"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
echo "开始备份数据库: $DB_NAME"
mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASSWORD \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  $DB_NAME | gzip > $BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz

# 验证备份
if [ -f "$BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz" ]; then
    echo "备份成功: ${DB_NAME}_${DATE}.sql.gz"
else
    echo "备份失败！" >&2
    exit 1
fi

# 删除旧备份
find $BACKUP_DIR -name "${DB_NAME}_*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "已删除 $RETENTION_DAYS 天前的旧备份"

# 上传到云存储（可选）
# aws s3 cp $BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz s3://backups/puyuan-maoshan/
```

## 监控配置

### Prometheus 配置 (monitoring/prometheus.yml)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'puyuan-production'
    env: 'prod'

scrape_configs:
  - job_name: 'platform-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['platform-api:8080']
        labels:
          service: 'platform-api'
          instance: 'prod-1'
```

### Prometheus 抓取配置

Prometheus 会通过以下配置自动抓取指标：

- **端点**: `http://platform-api:8080/actuator/prometheus`
- **频率**: 每 15 秒
- **可用指标**:
  - `ai_key_switch_total{group, provider}` - AI Key 切换次数
  - `ai_call_success_total{group, provider}` - AI 调用成功次数
  - `ai_call_failure_total{group, provider, reason}` - AI 调用失败次数
  - `oss_switch_total{bucket}` - OSS 切换次数
  - Spring Boot 标准指标（JVM、HTTP、缓存等）

### 告警规则 (monitoring/alerts.yml)

```yaml
groups:
  - name: platform_alerts
    interval: 30s
    rules:
      # AI 调用失败率过高
      - alert: HighAiFailureRate
        expr: |
          (
            sum(rate(ai_call_failure_total[5m])) by (group, provider)
            /
            sum(rate(ai_call_success_total[5m]) + rate(ai_call_failure_total[5m])) by (group, provider)
          ) > 0.5
        for: 5m
        labels:
          severity: critical
          service: ai_service
        annotations:
          summary: "AI 服务失败率过高"
          description: "{{ $labels.group }}/{{ $labels.provider }} 失败率超过 50%"

      # OSS 切换频繁
      - alert: FrequentOssSwitch
        expr: sum(rate(oss_switch_total[10m])) by (bucket) > 1
        for: 10m
        labels:
          severity: warning
          service: oss_service
        annotations:
          summary: "OSS 切换频繁"
          description: "Bucket {{ $labels.bucket }} 在 10 分钟内切换超过 10 次"

      # 应用实例不健康
      - alert: ApplicationUnhealthy
        expr: up{job="platform-api"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "应用实例不健康"
          description: "平台 API 已停止响应"
```

## 安全检查清单

- [ ] 数据库密码强度检查（至少 16 字符，包含大小写字母、数字、特殊字符）
- [ ] `CRYPTO_SECRET_KEY` 已设置为强密钥
- [ ] Redis 密码已配置
- [ ] 生产环境禁用了 Mock 模式（`AI_MOCK_ENABLED=false`）
- [ ] 数据库仅允许应用服务器 IP 访问
- [ ] Redis 仅允许应用服务器 IP 访问
- [ ] SSL/TLS 已启用
- [ ] HTTP 响应头已配置安全参数（CORS、CSP、X-Frame-Options）
- [ ] 日志中不记录敏感信息（完整密码、Token）

## 部署步骤

1. **准备服务器**
   ```bash
   # 创建用户
   sudo useradd -m -s /bin/bash platform

   # 创建目录
   sudo mkdir -p /opt/platform-api /var/log/platform-api /var/backups/mysql
   sudo chown -R platform:platform /opt/platform-api /var/log/platform-api

   # 克隆代码
   cd /opt/platform-api
   git clone https://github.com/your-org/puyuan-maoshan.git .
   ```

2. **配置环境变量**
   ```bash
   # 创建 .env 文件
   cd /opt/platform-api
   nano .env
   # 填写环境变量（见上方模板）
   ```

3. **启动服务**
   ```bash
   # 使用 docker-compose 启动
   sudo docker-compose up -d

   # 或使用 systemd 启动（传统部署）
   sudo systemctl enable platform-api
   sudo systemctl start platform-api
   ```

4. **验证部署**
   ```bash
   # 健康检查
   curl http://localhost:8080/actuator/health

   # Prometheus 指标
   curl http://localhost:8080/actuator/prometheus

   # 访问管理后台
   curl https://admin.puyuanmaoshan.com/admin/login
   ```

5. **配置系统配置**
   - 登录管理后台
   - 进入「系统配置」
   - 添加 AI 配置
   - 添加 OSS 配置
   - 测试配置

## 回滚计划

1. **数据库回滚**
   ```bash
   # 恢复最近的备份
   gunzip < /var/backups/mysql/puyuan_ai_prod_YYYYMMDD_HHMMSS.sql.gz | mysql -u root -p puyuan_ai_prod
   ```

2. **应用回滚**
   ```bash
   # 切换到之前的版本
   cd /opt/platform-api
   git checkout <previous-tag>

   # 重新构建和部署
   sudo docker-compose up -d --build
   ```

## 监控和告警联系方式

| 严重级别 | 告警方式 | 联系人 |
|---------|---------|--------|
| Critical | 短信、电话、邮件 | on-call@example.com |
| Warning | 邮件、企业微信 | team@example.com |
| Info | 邮件 | dev-team@example.com |

## 常见问题

### Q: Redis 连接失败
A: 检查防火墙规则，确保应用服务器可以访问 Redis 的 6379 端口。

### Q: 数据库连接超时
A: 检查 `application.yml` 中的连接池配置，适当增加 `hikari.maximum-pool-size`。

### Q: 加密密钥错误
A: 确保所有服务器使用相同的 `CRYPTO_SECRET_KEY`，否则加密的数据无法解密。

### Q: Prometheus 抓取不到指标
A: 检查 `/actuator/prometheus` 端点是否可访问，以及网络配置。

## 后续优化建议

1. 添加 Elasticsearch + Kibana 用于日志聚合和搜索
2. 添加 Jaeger 或 Zipkin 用于分布式追踪
3. 添加负载均衡器（如 Nginx 或 HAProxy）
4. 配置自动扩缩容（如 K8s HPA）
5. 添加灰度发布能力
6. 实现配置版本控制和回滚
7. 添加数据库读写分离
8. 实现跨区域高可用部署
