# 濮院毛衫 AI 平台 - 验证步骤

## 部署后验证

### 1. 基础服务健康检查

#### 1.1 后端服务
```bash
# 健康检查
curl http://localhost:8080/actuator/health
# 预期输出: {"status":"UP"}

# 应用信息
curl http://localhost:8080/actuator/info

# Prometheus 指标
curl http://localhost:8080/actuator/metrics | grep -E "(jvm|system|process)"
```

#### 1.2 数据库连接
```bash
mysql -u puyuan -p -e "SELECT 1;"
# 预期输出: +---+\n| 1 |\n+---+
```

#### 1.3 Redis 连接
```bash
redis-cli -a YOUR_REDIS_PASSWORD ping
# 预期输出: PONG
```

### 2. 系统配置功能验证

#### 2.1 访问系统配置页面
1. 登录管理后台: http://admin.puyuanmaoshan.com/admin/login
2. 使用 super_admin 账号登录
3. 验证「系统配置」菜单可见性
4. 点击进入系统配置页面

#### 2.2 AI 图片生成配置
```
操作步骤:
1. 切换到「AI 图片生成」标签页
2. 验证现有配置显示正确（脱敏后的 API Key）
3. 点击「测试配置」按钮
4. 验证测试结果返回成功

预期结果:
- 配置列表正常显示
- API Key 显示为 sk-**** 格式
- 测试按钮返回成功
```

#### 2.3 更新配置
```
操作步骤:
1. 点击「编辑」按钮
2. 修改配置值（如 endpoint）
3. 点击「保存」
4. 验证审计日志记录

预期结果:
- 配置更新成功
- 数据库中的值已加密
- 审计日志记录了变更（旧值、新值、操作人、IP）
```

#### 2.4 OSS 配置
```
操作步骤:
1. 切换到「对象存储 (OSS)」标签页
2. 验证 Access Key ID 和 Secret 脱敏显示
3. 测试 OSS 连接
4. 添加新 OSS 配置
```

#### 2.5 Redis 缓存验证
```bash
# 更新配置前查看缓存
redis-cli -a YOUR_REDIS_PASSWORD keys "system_config:*"

# 更新配置（通过前端）

# 验证缓存已清除
redis-cli -a YOUR_REDIS_PASSWORD keys "system_config:*"

# 预期结果: 旧缓存已清除，下次读取会重新加载
```

### 3. 审计日志验证

#### 3.1 查看审计日志
```sql
-- 查询最近的配置变更
SELECT
    id,
    operator_id,
    action,
    target_type,
    target_id,
    ip,
    old_value,
    new_value,
    created_at
FROM audit_log
WHERE target_type = 'system_config'
ORDER BY created_at DESC
LIMIT 10;

-- 预期结果:
-- 显示最近10次配置变更记录
-- 包含操作人、操作类型、IP地址
-- 敏感值已脱敏
```

#### 3.2 验证脱敏
```sql
-- 验证 API Key 已脱敏
SELECT old_value, new_value
FROM audit_log
WHERE target_type = 'system_config'
  AND (new_value LIKE 'sk-%' OR old_value LIKE 'sk-%')
LIMIT 5;

-- 预期结果: 显示 sk-**** 而不是完整的 key
```

### 4. Prometheus 指标验证

#### 4.1 查看自定义指标
```bash
# AI Key 切换次数
curl -s 'http://localhost:8080/actuator/metrics/ai_key_switch_total'

# AI 调用成功次数
curl -s 'http://localhost:8080/actuator/metrics/ai_call_success_total'

# AI 调用失败次数
curl -s 'http://localhost:8080/actuator/metrics/ai_call_failure_total'

# OSS 切换次数
curl -s 'http://localhost:8080/actuator/metrics/oss_switch_total'

# 预期结果: 返回指标值和标签
```

#### 4.2 触发指标测试
```bash
# 通过 API 触发配置变更，观察指标变化
# 在 Grafana 中查看指标曲线
```

### 5. 租户端功能验证

#### 5.1 登录和租户切换
```
操作步骤:
1. 访问租户端: http://app.puyuanmaoshan.com
2. 登录
3. 验证能查看所属租户列表
4. 切换租户
5. 验证租户上下文正确
```

#### 5.2 AI 工具功能
```
操作步骤:
1. 进入「AI 商品图生成」
2. 输入提示词
3. 选择尺寸
4. 点击生成
5. 验证 Token 扣费和结果展示

预期结果:
- 图片生成成功（Mock 或真实）
- Token 余额正确扣减
- 显示剩余余额
```

### 6. 安全验证

#### 6.1 认证测试
```bash
# 无认证访问应返回 401
curl -I http://localhost:8080/api/v1/admin/system-config/groups
# 预期: HTTP/1.1 401

# 添加认证头
curl -H "X-User-Id: 1" http://localhost:8080/api/v1/admin/system-config/groups
# 预期: 需要有效 token
```

#### 6.2 租户隔离验证
```bash
# 用户 A 尝试访问租户 B 的数据应被拒绝
```

#### 6.3 SQL 注入防护测试
```bash
# 在查询参数中注入 SQL，应被安全处理
```

### 7. 性能验证

#### 7.1 配置缓存命中
```
操作步骤:
1. 第一次调用配置接口（记录响应时间）
2. 第二次调用（应从 Redis 缓存读取）
3. 对比响应时间

预期结果:
- 第二次响应时间显著减少（从 DB -> Redis）
```

#### 7.2 并发测试
```bash
# 使用 Apache Bench 进行并发测试
ab -n 1000 -c 10 http://localhost:8080/api/v1/health

# 预期结果:
- 无错误
- 响应时间稳定
```

### 8. 备份验证

#### 8.1 数据库备份验证
```bash
# 验证备份文件存在
ls -lh /backup/mysql/puyuan_*.sql.gz

# 恢复测试（可选）
gunzip < /backup/mysql/puyuan_YYYYMMDD_HHMMSS.sql.gz | mysql -u puyuan -p puyuan_ai_test
```

#### 8.2 应用备份验证
```bash
# 验证 JAR 备份
ls -lh /backup/app/platform-api_*.jar
```

### 9. 告警验证

#### 9.1 健康检查告警
```bash
# 停止应用
systemctl stop platform-api

# 验证收到告警（Prometheus Alertmanager / Grafana）

# 恢复应用
systemctl start platform-api
```

#### 9.2 指标阈值告警
```
触发条件测试:
- AI 调用失败率 > 10%
- OSS 切换次数异常增长
- JVM 堆内存使用率 > 80%
```

### 10. 回滚验证

#### 10.1 应用回滚
```bash
# 查看当前版本
systemctl status platform-api

# 停止应用
systemctl stop platform-api

# 恢复旧版本 JAR
cp /backup/app/platform-api_YYYYMMDD.jar /opt/puyuan/platform/platform-api-0.0.1-SNAPSHOT.jar

# 启动应用
systemctl start platform-api

# 验证回滚成功
curl http://localhost:8080/actuator/health
```

#### 10.2 数据库回滚
```sql
-- 如果需要回滚到之前的数据库状态
-- 使用备份文件恢复
```

## 验收标准

所有以下项目必须通过才能认为部署成功：

- [ ] 后端服务健康检查通过
- [ ] 数据库和 Redis 连接正常
- [ ] 系统配置菜单对 super_admin 可见
- [ ] 配置 CRUD 操作正常
- [ ] 配置值正确加密存储
- [ ] 敏感值正确脱敏显示
- [ ] 审计日志完整记录所有变更
- [ ] Redis 缓存正常工作（命中、失效）
- [ ] Prometheus 指标正常收集
- [ ] 租户端 AI 工具功能正常
- [ ] Token 扣费逻辑正确
- [ ] 租户隔离验证通过
- [ ] 安全认证验证通过
- [ ] 性能指标符合预期
- [ ] 备份任务正常运行
- [ ] 告警通知正常
- [ ] 回滚方案验证通过

## 常见问题排查

### 问题 1: 系统配置菜单不可见
```
检查项:
1. 用户角色是否为 platform_super_admin
2. Token 是否有效
3. 前端路由配置是否正确

解决方法:
- 使用正确的 super_admin 账号登录
- 清除浏览器缓存重新登录
```

### 问题 2: Redis 连接失败
```
检查项:
1. Redis 服务是否启动
2. 防火墙规则
3. 密码配置

解决方法:
systemctl status redis-server
redis-cli ping
```

### 问题 3: 配置保存失败
```
检查项:
1. 数据库连接
2. 加密密钥配置
3. 审计日志表结构

解决方法:
- 检查 application.yml 配置
- 验证数据库表结构
- 查看后端日志
```

### 问题 4: 指标未显示
```
检查项:
1. Prometheus 是否配置了该目标
2. metrics endpoint 是否可访问

解决方法:
curl http://localhost:8080/actuator/metrics
检查 Prometheus scrape 配置
```
