# AI 商品图生成插件

> 遵循濮院毛衫 AI 平台 `PLUGIN_API_SPECIFICATION.md` 规范

## 插件信息

| 字段 | 值 |
|------|-----|
| plugin_id | `acme.ai-image-gen` |
| name | AI 商品图生成 |
| version | 1.0.0 |
| billing_type | token |
| default_token_cost | 20 |

## 目录结构

```
ai-image-gen-plugin/
├── manifest.json           # 插件描述文件
├── README.md               # 本文件
├── backend/
│   ├── server.js           # 独立后端服务（Node.js）
│   ├── package.json        # Node.js 依赖
│   └── Dockerfile          # 容器化部署
└── frontend/
    ├── index.html         # 前端入口（iframe 嵌入）
    └── style.css          # 样式（含 CSS 变量）
```

## 快速开始

### 前端独立预览

直接在浏览器打开 `frontend/index.html`（无主框架时显示静态演示）。

### 后端独立运行

```bash
cd backend
npm install
npm start
# 访问 http://localhost:3001/health 确认服务健康
```

### 平台集成

1. 上传整个目录（打包为 zip）到管理后台
2. 系统自动解析 `manifest.json`，校验字段
3. 在「沙箱测试」中验证功能
4. 通过后「发布全量」上架

## API 规范

### 调用接口

```
POST /
```

**请求头**

| Header | 说明 |
|--------|------|
| `X-Tenant-Id` | 租户 ID |
| `Authorization` | `Bearer <token>`（可选） |
| `Content-Type` | `application/json` |

**请求体**

```json
{
  "prompt": "红色濮院毛衫，圆领",
  "image_size": "1024x1024"
}
```

**响应格式**

```json
{
  "code": 0,
  "data": {
    "image_url": "https://picsum.photos/seed/123/1024/1024",
    "image_size": "1024x1024",
    "prompt_hash": "aGVsbG8gd29yb"
  },
  "token_used": 20,
  "balance_remaining": 9980,
  "message": "Image generated successfully"
}
```

### 健康检查

```
GET /health

Response: { "status": "ok", "version": "1.0.0", "uptime": 86400 }
```

## 平台通信协议

主框架通过 `postMessage` 与插件 iframe 通信：

| 主框架 → 插件 | 说明 |
|-------------|------|
| `INIT` | 初始化数据（tenantId, token, apiBaseUrl, theme） |
| `THEME_CHANGE` | 主题变更 |
| `INVOKE` | 触发特定操作 |

| 插件 → 主框架 | 说明 |
|-------------|------|
| `PLUGIN_READY` | 插件加载完成 |
| `INVOKE_RESULT` | 操作结果 |
| `ERROR` | 错误信息 |
| `PLUGIN_STATUS` | 状态变更 |

## 错误码

| code | 说明 |
|------|------|
| 0 | 成功 |
| 40001 | 请求参数错误 |
| 50001 | 插件内部错误 |

## 部署

### Docker

```bash
cd backend
docker build -t ai-image-gen-plugin .
docker run -d -p 3001:3001 ai-image-gen-plugin
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PORT` | 3001 | 服务端口 |
| `MOCK_IMAGE_URL` | https://picsum.photos | 图片源基础 URL |

## 计费说明

插件只负责返回 `token_used`，平台网关自动完成余额校验和扣费。

| 尺寸 | Token 消耗 |
|------|-----------|
| 512×512 | 10 |
| 1024×1024 | 20 |
| 1792×1024 / 1024×1792 | 30 |