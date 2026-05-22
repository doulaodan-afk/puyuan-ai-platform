# 部署验证步骤指南

## 一、系统配置菜单验证

### 访问地址
- **本地开发**: http://localhost:5173/admin
- **生产环境**: https://admin.puyuanmaoshan.com/admin

### 默认登录账号
需要在数据库中创建管理员账号，或者使用测试账号：
- **手机号**: 根据数据库配置（示例：13800138000）
- **验证码**: 测试环境可以使用固定验证码（如：123456）

### 验证步骤
1. 访问管理后台登录页面
2. 使用管理员账号登录（`role_code = 'platform_super_admin'`）
3. 检查导航栏是否有「系统配置」菜单项
4. 点击「系统配置」进入配置管理页面
5. 验证四个标签页是否正常显示：
   - AI 图片生成
   - AI 文本生成
   - AI 翻译
   - 对象存储 (OSS)

### 预期结果
- 菜单显示位置：在「审计」菜单右侧
- 菜单可见性：仅 `platform_super_admin` 角色可见

## 二、Redis 缓存验证

### 1. 确认 Redis 连接
```bash
# 连接 Redis
redis-cli -h 127.0.0.1 -p 6379

# 查看连接信息
INFO server

# 查看数据库
SELECT 0
```

### 2. 查看缓存内容
```bash
# 查看所有系统配置相关的键
redis-cli KEYS "system_config:*"

# 查看具体缓存内容
redis-cli GET "system_config:group:ai_image"
redis-cli GET "system_config:active:ai_text"
redis-cli GET "system_config:map:oss"
```

### 3. 验证缓存更新
```bash
# 1. 清空 Redis 缓存
redis-cli FLUSHDB

# 2. 访问 API 获取配置
curl -H "X-Request-Id: test-001" \
     http://localhost:8080/api/v1/admin/system-config/list?group=ai_image

# 3. 验证缓存已写入
redis-cli KEYS "system_config:*"
```

### 4. 验证缓存 TTL
```bash
# 查看键的剩余生存时间（秒）
redis-cli TTL "system_config:group:ai_image"

# 预期结果：接近 300（5 分钟）
```

### 5. 验证缓存清除
```bash
# 1. 保存新配置（通过管理后台或 API）
curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"config_group":"ai_image","config_key":"test_key","config_value":"test_value","enabled":true,"sort_order":1}' \
     http://localhost:8080/api/v1/admin/system-config/save

# 2. 验证缓存已被清除
redis-cli KEYS "system_config:*"

# 预期结果：缓存键不存在（因为保存操作会清除缓存）
```

## 三、Prometheus 监控指标验证

### 1. 访问 Actuator 健康检查
```bash
curl http://localhost:8080/actuator/health
```

**预期响应**:
```json
{
  "status": "UP",
  "groups": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### 2. 访问 Prometheus 指标端点
```bash
curl http://localhost:8080/actuator/prometheus
```

**预期响应**: 查找以下指标：

```
# AI Key 切换指标
ai_key_switch_total{group="ai_image",provider="OpenAI"} 0.0

# AI 调用成功指标
ai_call_success_total{group="ai_text",provider="OpenAI"} 10.0

# AI 调用失败指标
ai_call_failure_total{group="ai_translate",provider="OpenAI",reason="quota_exceeded"} 2.0

# OSS 切换指标
oss_switch_total{bucket="puyuan-maoshan"} 1.0

# Spring Boot 标准指标
jvm_memory_used_bytes
jvm_gc_pause_seconds_count
http_server_requests_seconds
```

### 3. 触发监控指标

#### 测试 AI Key 切换
```bash
# 1. 在管理后台添加两个 AI 图片生成配置（一个失效 Key，一个有效 Key）
#    配置 1：优先级 1，API Key = "invalid-key"
#    配置 2：优先级 2，API Key = "valid-key"

# 2. 调用 AI 图片生成 API
curl -X POST \
     -H "Content-Type: application/json" \
     -H "X-Request-Id: test-switch-001" \
     -d '{"prompt":"test","size":"1024x1024"}' \
     http://localhost:8080/api/plugin/invoke/ai_image_gen

# 3. 查看指标
curl http://localhost:8080/actuator/prometheus | grep ai_key_switch_total
```

**预期结果**: `ai_key_switch_total` 的值增加（表示发生了切换）

#### 测试 AI 调用成功/失败
```bash
# 1. 调用 AI 服务（正常情况）
curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"prompt":"test","size":"1024x1024"}' \
     http://localhost:8080/api/plugin/invoke/ai_image_gen

# 2. 查看指标
curl http://localhost:8080/actuator/prometheus | grep ai_call_success_total
curl http://localhost:8080/actuator/prometheus | grep ai_call_failure_total
```

**预期结果**:
- `ai_call_success_total` 的值增加
- `ai_call_failure_total` 的值不变

### 4. 验证 Prometheus 抓取

```bash
# 1. 启动 Prometheus（docker-compose）
docker-compose -f monitoring/docker-compose.yml up -d prometheus

# 2. 访问 Prometheus UI
open http://localhost:9090

# 3. 查询指标
# 输入查询：ai_key_switch_total
# 输入查询：rate(ai_call_success_total[5m])
# 输入查询：sum(ai_call_failure_total) by (group, provider)
```

### 5. 验证告警规则

```bash
# 1. 检查 Prometheus 配置
cat monitoring/prometheus.yml

# 2. 检查告警规则
cat monitoring/alerts.yml

# 3. 在 Prometheus UI 查看告警状态
open http://localhost:9090/alerts
```

**预期结果**:
- 告警规则已加载（无错误）
- 告警状态显示
- 未触发告警（正常运行时）

### 6. 集成 Grafana（可选）

```bash
# 1. 启动 Grafana
docker-compose -f monitoring/docker-compose.yml up -d grafana

# 2. 访问 Grafana
open http://localhost:3000

# 3. 登录（默认账号：admin / admin，首次登录需要修改密码）

# 4. 添加 Prometheus 数据源
#    URL: http://prometheus:9090

# 5. 导入仪表板
#    可以使用预配置的 JSON 仪表板文件
```

## 四、审计日志验证

### 1. 查看审计日志数据库表
```sql
-- 查询系统配置变更日志
SELECT * FROM audit_log
WHERE target_type = 'system_config'
ORDER BY created_at DESC
LIMIT 10;
```

### 2. 验证审计日志记录
```bash
# 1. 在管理后台添加/修改/删除系统配置
# 2. 查询数据库
mysql -u root -p puyuan_ai_mvp -e \
  "SELECT * FROM audit_log WHERE target_type = 'system_config' ORDER BY created_at DESC LIMIT 5;"
```

**预期结果**:
- `action`: "CREATE" / "UPDATE" / "DELETE"
- `target_type`: "system_config"
- `operator_id`: 当前登录用户的 ID
- `ip`: 请求者的 IP 地址
- `old_value` / `new_value`: 变更前后的值（敏感值已脱敏）
- `created_at`: 操作时间

### 3. 测试敏感值脱敏
```bash
# 1. 添加包含 API Key 的配置
curl -X POST \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{"config_group":"ai_image","config_key":"api_key","config_value":"sk-proj-abc123xyz789","enabled":true,"sort_order":1}' \
     http://localhost:8080/api/v1/admin/system-config/save

# 2. 查询审计日志
mysql -u root -p puyuan_ai_mvp -e \
  "SELECT action, old_value, new_value FROM audit_log WHERE target_type = 'system_config' ORDER BY created_at DESC LIMIT 1;"
```

**预期结果**:
- `new_value`: "sk-****x789"（已脱敏）

## 五、部署完整性检查

### 检查清单
```bash
# 1. 应用服务状态
docker ps | grep platform-api

# 2. 数据库连接
mysql -u root -p -h 127.0.0.1 -e "SELECT 1;"

# 3. Redis 连接
redis-cli ping

# 4. 健康检查
curl -f http://localhost:8080/actuator/health || echo "健康检查失败"

# 5. 日志文件
ls -lh /var/log/platform-api/

# 6. 端口监听
netstat -tuln | grep 8080
netstat -tuln | grep 6379
```

### 预期结果
- 应用容器运行中
- 数据库可连接
- Redis 可连接
- 健康检查返回 200 OK
- 日志文件存在且可写入
- 端口正确监听

## 六、功能测试

### 测试 1：添加配置
```bash
# 使用 API 添加配置
curl -X POST \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
     -d '{
       "config_group": "ai_image",
       "config_key": "api_key",
       "config_value": "sk-test-1234567890",
       "enabled": true,
       "sort_order": 1,
       "description": "测试 API Key"
     }' \
     http://localhost:8080/api/v1/admin/system-config/save
```

### 测试 2：读取配置（验证缓存）
```bash
# 第一次请求（查询数据库）
curl -H "X-Request-Id: test-cache-001" \
     http://localhost:8080/api/v1/admin/system-config/list?group=ai_image

# 第二次请求（读取缓存）
curl -H "X-Request-Id: test-cache-002" \
     http://localhost:8080/api/v1/admin/system-config/list?group=ai_image

# 验证 Redis 缓存
redis-cli GET "system_config:group:ai_image"
```

### 测试 3：删除配置（验证缓存清除）
```bash
# 删除配置
curl -X DELETE \
     -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
     http://localhost:8080/api/v1/admin/system-config/save/1

# 验证缓存已清除
redis-cli KEYS "system_config:group:ai_image"
```

## 七、故障排查

### 问题：缓存不生效
**排查步骤**:
1. 检查 Redis 是否正常: `redis-cli ping`
2. 检查应用日志中是否有 Redis 连接错误
3. 确认 Redis 配置中的 `spring.data.redis` 是否正确

### 问题：审计日志未记录
**排查步骤**:
1. 检查 `RequestContextUtil` 是否正确设置了用户 ID 和 IP
2. 检查数据库表结构是否包含新增字段
3. 查看应用日志中的异常信息

### 问题：Prometheus 抓取不到指标
**排查步骤**:
1. 检查 Actuator 端点是否可访问: `curl http://localhost:8080/actuator/prometheus`
2. 检查 Prometheus 配置中的 `targets` 是否正确
3. 查看 Prometheus 日志中的抓取错误

### 问题：Nginx 限流不生效
**排查步骤**:
1. 检查 Nginx 配置中的 `limit_req_zone` 和 `limit_req` 是否正确
2. 验证 Nginx 配置是否重新加载: `nginx -t && nginx -s reload`
3. 检查是否有其他代理或负载均衡器

## 八、验证完成确认

完成所有验证后，请确认以下项目：

- [ ] 系统配置菜单在管理后台可见
- [ ] 添加/编辑/删除配置功能正常
- [ ] Redis 缓存正常工作（TTL 正确，更新时清除）
- [ ] 审计日志正确记录配置变更
- [ ] Prometheus 指标正常暴露
- [ ] 告警规则加载成功
- [ ] 健康检查端点可访问
- [ ] 所有环境变量已正确配置
- [ ] 日志正常输出且不包含敏感信息
