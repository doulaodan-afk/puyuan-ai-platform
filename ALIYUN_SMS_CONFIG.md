# 阿里云短信配置指南

## 📋 配置信息

已配置的短信参数：

| 参数 | 值 |
|-----|-----|
| 短信签名 | 濮院毛衫 |
| 登录模板 ID | SMS_474785772 |
| 注册模板 ID | SMS_474980747 |
| API 端点 | dysmsapi.aliyuncs.com |

## 🔑 获取 Access Key

### 步骤 1: 登录阿里云控制台
访问 [阿里云控制台](https://www.aliyun.com/)，使用你的阿里云账号登录。

### 步骤 2: 进入 RAM 访问控制
1. 点击右上角账户 → **访问控制**
2. 左侧菜单 → **人员管理** → **用户**

### 步骤 3: 创建用户（可选）
如果想为短信服务创建独立的用户：
1. 点击 **创建用户**
2. 输入用户名：`sms-service`
3. 访问方式：勾选 **Open API 调用访问**
4. 点击 **创建**

### 步骤 4: 获取 Access Key
1. 点击你的用户名（或新创建的用户）
2. 左侧 **用户AccessKey**
3. 点击 **创建 AccessKey**
4. 选择 **学习和测试** 或 **开发和测试**（根据需要）
5. 点击 **创建**
6. 复制 **AccessKeyId** 和 **AccessKeySecret**

### 步骤 5: 授予短信权限（如使用独立用户）
1. 回到 RAM 用户列表
2. 选择新创建的用户
3. 点击 **添加权限**
4. 搜索并选择 **AliyunDysmsFullAccess**（短信完全访问权限）
5. 点击 **确定**

## ⚙️ 配置到项目

### 生产环境配置

编辑 `backend/java-spring/src/main/resources/application-prod.yml`：

```yaml
aliyun:
  sms:
    enabled: true
    access-key-id: your_access_key_id      # 替换为你的 AccessKeyId
    access-key-secret: your_access_key_secret  # 替换为你的 AccessKeySecret
    endpoint: dysmsapi.aliyuncs.com
    sign-name: 濮院毛衫
    login-template-id: SMS_474785772
    register-template-id: SMS_474980747
```

### 本地开发环境配置

编辑 `backend/java-spring/src/main/resources/application-dev.yml`：

```yaml
aliyun:
  sms:
    enabled: false  # 本地开发环境禁用，使用 mock 短信
    access-key-id: your_access_key_id
    access-key-secret: your_access_key_secret
    endpoint: dysmsapi.aliyuncs.com
    sign-name: 濮院毛衫
    login-template-id: SMS_474785772
    register-template-id: SMS_474980747
```

## 📱 API 端点

### 发送登录验证码

```bash
POST /api/v1/sms/send-login-code
Content-Type: application/x-www-form-urlencoded

mobile=13800000001
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 发送注册验证码

```bash
POST /api/v1/sms/send-register-code
Content-Type: application/x-www-form-urlencoded

mobile=13800000001
```

### 登录接口（已集成短信验证）

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "mobile": "13800000001",
  "verify_code": "123456"  # 从短信获得的验证码
}
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "access_token": "token-123-456",
    "token_expire_seconds": 7200,
    "user_id": 123,
    "tenant_id": 456,
    "role_code": "merchant_owner"
  }
}
```

## 🧪 本地测试

如果本地开发环境禁用了短信（`aliyun.sms.enabled: false`），可以：

1. **模拟短信验证码**：直接在 Redis 中设置
```bash
# 连接 Redis
redis-cli

# 设置登录验证码（有效期 5 分钟）
SET sms:code:login:13800000001 "123456" EX 300

# 验证
GET sms:code:login:13800000001
```

2. **调用登录接口**：
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13800000001","verify_code":"123456"}'
```

## 🔒 安全建议

1. **不要硬编码 Access Key**：使用环境变量或密钥管理服务
   ```bash
   export ALIYUN_SMS_ACCESS_KEY_ID=your_key_id
   export ALIYUN_SMS_ACCESS_KEY_SECRET=your_key_secret
   ```

2. **限制 IP 访问**：在 RAM 中为用户设置 IP 白名单

3. **定期轮换 Key**：定期生成新的 Access Key 并删除旧的

4. **监控短信发送**：在阿里云控制台查看短信发送日志和成本

## 📊 成本说明

- 阿里云短信按**条**收费
- 国内短信：通常 0.05 元/条 ~ 0.1 元/条（根据签名类型和发送量）
- 验证码短信优惠最多

## 📖 参考文档

- [阿里云短信产品文档](https://help.aliyun.com/product/44282.html)
- [Java SDK 使用示例](https://github.com/aliyun/dysmsapi-java-sdk)
- [常见问题](https://help.aliyun.com/knowledge_detail/55357.html)

## 🐛 常见问题

### Q: 发送短信失败，错误：SignatureDoesNotMatch

**A:** Access Key Secret 不正确，请检查是否复制完整。

### Q: 验证码无法接收

**A:** 
1. 检查手机号是否正确
2. 检查短信签名是否与申请时一致
3. 检查阿里云账户余额是否充足
4. 在阿里云控制台查看发送日志

### Q: 如何查看发送日志？

**A:** 
1. 登录阿里云控制台
2. 进入 **消息服务** → **短信** → **消息详情**
3. 查看发送状态和错误原因

## ✅ 部署检查清单

- [ ] 获取了阿里云 Access Key
- [ ] 更新了 `application-prod.yml` 中的 Access Key
- [ ] 验证了短信签名和模板 ID
- [ ] 测试了短信发送接口
- [ ] 阿里云账户余额充足
- [ ] 检查了发送日志

