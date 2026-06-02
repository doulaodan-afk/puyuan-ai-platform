# AI MVP 插件部署指南

## 快速启动

### 1. 数据库初始化

```bash
mysql -u root -p puyuan_ai_mvp < backend/sql/ai_plugins_seed.sql
```

### 2. 启动后端服务

```bash
cd backend/java-spring
mvn clean package
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 3. 启动前端服务

```bash
cd frontend/merchant-web
npm run dev
```

### 4. 访问页面

- 租户端：http://localhost:5173
  - AI 图片生成：http://localhost:5173/ai-tools/image-gen
  - AI 脚本生成：http://localhost:5173/ai-tools/script-gen
  - AI 跨境翻译：http://localhost:5173/ai-tools/translate

## 测试 Mock 模式

默认启用 Mock 模式，无需 API Key。

### 测试步骤

1. 登录租户端（手机号：13800000001，验证码任意输入）
2. 进入任一 AI 工具页面
3. 输入测试数据并提交
4. 查看返回结果和 Token 扣费

### 预期结果

- 图片生成：返回占位图片 URL
- 脚本生成：返回预定义脚本模板
- 翻译：返回带语言后缀的原文

## 切换到真实 AI 模式

### 设置环境变量

```bash
export OPENAI_API_KEY=your_actual_api_key
export OPENAI_BASE_URL=https://api.openai.com
export AI_MOCK_ENABLED=false
```

### 重启后端服务

```bash
# 停止当前服务（Ctrl+C）
# 重新启动
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 计费规则

| 插件 | 计费方式 | 费用详情 |
|------|---------|----------|
| AI 图片生成 | 按尺寸 | 512x512: 10 Tokens<br>1024x1024: 20 Tokens<br>1792x1024: 30 Tokens |
| AI 脚本生成 | 固定 | 20 Tokens |
| AI 跨境翻译 | 按长度 | 每 10 字符 1 Token（最少 5 Tokens） |

## API 文档

### 统一插件调用接口

**POST** `/api/plugin/invoke/{pluginCode}`

**请求头：**
```
X-Tenant-Id: {tenant_id}
X-Request-Id: {uuid}
Authorization: Bearer {access_token}
```

**插件 Code：**
- `ai_image_gen`
- `ai_script_gen`
- `ai_translate`

**响应示例：**
```json
{
  "code": 0,
  "message": "ok",
  "request_id": "req-xxx",
  "data": {
    "data": {
      "image_url": "https://...",
      "image_size": "1024x1024"
    },
    "token_used": 20,
    "balance_remaining": 99980
  }
}
```

## 故障排查

### 插件未启用

错误信息：`plugin disabled for tenant`

解决：在插件列表页面启用对应插件

### 余额不足

错误信息：`insufficient token balance`

解决：在充值中心充值

### 插件不存在

错误信息：`plugin not found`

解决：执行数据库种子脚本 `backend/sql/ai_plugins_seed.sql`