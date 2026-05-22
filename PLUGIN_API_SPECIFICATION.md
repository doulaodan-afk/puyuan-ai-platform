# 濮院毛衫 AI 平台 — 插件接口规范

> **版本**：1.0.0
> **最后更新**：2026-05-22
> **适用范围**：所有接入平台的第三方插件

---

## 目录

1. [概述](#1-概述)
2. [插件描述文件 manifestjson](#2-插件描述文件-manifestjson)
3. [后端接口规范](#3-后端接口规范)
4. [前端嵌入规范](#4-前端嵌入规范)
5. [插件部署与注册](#5-插件部署与注册)
6. [计费与权限](#6-计费与权限)
7. [错误处理与日志](#7-错误处理与日志)
8. [附录：Hello World 示例插件](#8-附录hello-world-示例插件)

---

## 1. 概述

### 1.1 平台架构

濮院毛衫 AI 平台采用「主框架 + 插件」分离架构：

```
┌─────────────────────────────────────────────────────┐
│                    主框架                            │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │  管理后台   │  │  商家端 Web  │  │  平台网关  │  │
│  └─────────────┘  └──────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────┘
         ↑ postMessage              ↑ REST API
         │ (iframe)                │
┌─────────────────────────────────────────────────────┐
│                   插件层                             │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │  插件 A     │  │  插件 B      │  │  插件 C   │  │
│  │  iframe     │  │  iframe      │  │  iframe   │  │
│  └─────────────┘  └──────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────┘
```

### 1.2 插件类型

平台支持两种插件接入方式：

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| **前端插件** | 仅包含 iframe 前端，无后端 | AI 对话卡片、数据展示 Widget |
| **全栈插件** | 包含前端 iframe + 后端 API | 需要 AI 推理、数据处理、文件生成 |

本规范覆盖全栈插件的开发标准。

### 1.3 核心概念

- **Tenant（租户）**：平台上的商家/企业主体，每个租户有独立余额。
- **Token（代币）**：平台内部计费单位，1 Token ≈ 1 次 AI API 调用消耗。
- **Plugin manifest**：插件的唯一标识与配置描述文件。

---

## 2. 插件描述文件 manifest.json

### 2.1 字段定义

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `plugin_id` | string | 是 | 插件唯一标识，格式：`{author}.{name}`，如 `acme.hello-world` |
| `name` | string | 是 | 插件显示名称 |
| `version` | string | 是 | 语义化版本，如 `1.0.0` |
| `description` | string | 是 | 插件功能描述，限制 200 字以内 |
| `icon_url` | string | 否 | 插件图标 URL，建议 64×64 PNG |
| `billing_type` | string | 是 | 计费类型：`token` / `per_call` / `free` |
| `default_token_cost` | number | 否 | 默认单次调用消耗 Token 数，`billing_type=token` 时必填 |
| `frontend_entry` | string | 否 | 前端入口 URL，如 `https://plugin.example.com/index.html` |
| `backend_api` | string | 否 | 后端 API 根地址，如 `https://api.plugin.example.com` |
| `need_config` | boolean | 否 | 是否需要租户在激活时提供配置（如 API Key） |
| `visible_to` | string[] | 否 | 可见角色列表，如 `["merchant", "admin"]`，空数组表示公开 |
| `required_role` | string | 否 | 调用插件所需的最小角色，如 `merchant` |
| `config_fields` | object[] | 否 | 若 `need_config=true`，定义需要的配置字段 |
| `health_endpoint` | string | 否 | 健康检查端点路径，默认 `/health` |

### 2.2 config_fields 子对象结构

```json
{
  "key": "api_key",
  "label": "API Key",
  "type": "password",
  "required": true,
  "placeholder": "sk-..."
}
```

### 2.3 完整 JSON 示例

```json
{
  "plugin_id": "acme.hello-world",
  "name": "Hello World 插件",
  "version": "1.0.0",
  "description": "一个简单的 Hello World 示例插件，展示平台插件开发规范。",
  "icon_url": "https://plugin.example.com/icon.png",
  "billing_type": "token",
  "default_token_cost": 100,
  "frontend_entry": "https://plugin.example.com/index.html",
  "backend_api": "https://api.plugin.example.com",
  "need_config": false,
  "visible_to": [],
  "required_role": "merchant",
  "config_fields": [],
  "health_endpoint": "/health"
}
```

```json
{
  "plugin_id": "acme.image-generator",
  "name": "毛衫图片生成器",
  "version": "1.2.0",
  "description": "根据文字描述生成濮院毛衫款式图片，支持多种风格。",
  "icon_url": "https://plugin.example.com/icon.png",
  "billing_type": "token",
  "default_token_cost": 500,
  "frontend_entry": "https://plugin.example.com/index.html",
  "backend_api": "https://api.plugin.example.com",
  "need_config": true,
  "visible_to": ["merchant", "admin"],
  "required_role": "merchant",
  "config_fields": [
    {
      "key": "api_key",
      "label": "AI API Key",
      "type": "password",
      "required": true,
      "placeholder": "sk-..."
    },
    {
      "key": "default_style",
      "label": "默认风格",
      "type": "select",
      "required": false,
      "options": ["简约", "复古", "时尚", "商务"]
    }
  ],
  "health_endpoint": "/health"
}
```

---

## 3. 后端接口规范

### 3.1 统一调用接口

插件后端**必须**实现以下接口，作为平台调用插件的统一入口：

```
POST {backend_api}
```

> **注意**：`{backend_api}` 的值来源于 `manifest.json` 中配置的 `backend_api` 字段。
> 如果插件后端部署在 `/plugin/acme.hello-world`，则 `backend_api = https://api.example.com`，
> 调用路径为 `POST https://api.example.com`。

### 3.2 请求头（Headers）

插件后端必须读取以下请求头来识别请求上下文：

| 请求头 | 说明 | 示例 |
|--------|------|------|
| `X-Tenant-Id` | 租户唯一标识 | `tenant_abc123` |
| `Authorization` | Bearer Token，平台颁发的访问令牌 | `Bearer eyJhbGciOiJIUzI1NiIs...` |
| `Content-Type` | 请求体格式 | `application/json` |

### 3.3 请求体（Request Body）

请求体为任意 JSON，由插件自定义，平台原封不动转发。

```json
{
  "prompt": "生成一件红色毛衫",
  "style": "商务",
  "size": "M"
}
```

### 3.4 响应格式（Response）

插件后端**必须**按以下统一格式返回：

```json
{
  "code": 0,
  "data": {
    // 插件自定义的返回数据
  },
  "token_used": 450,
  "balance_remaining": 12300,
  "message": "操作成功"
}
```

#### 响应字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | integer | 是 | 状态码，`0` 表示成功，其他表示错误 |
| `data` | object/array | 否 | 插件返回的业务数据 |
| `token_used` | integer | 是 | 本次调用消耗的 Token 数量（平台据此扣费） |
| `balance_remaining` | integer | 是 | 调用后租户剩余 Token 余额（供参考） |
| `message` | string | 否 | 人类可读的状态描述 |

#### 正确响应示例

```json
{
  "code": 0,
  "data": {
    "image_url": "https://cdn.example.com/img/abc.png",
    "width": 1024,
    "height": 1024
  },
  "token_used": 500,
  "balance_remaining": 11800,
  "message": "图片生成成功"
}
```

### 3.5 错误码约定

插件应使用以下统一错误码：

| code | 说明 | HTTP 状态码建议 |
|------|------|----------------|
| `0` | 成功 | 200 |
| `40001` | 请求参数错误 | 400 |
| `40002` | 插件配置缺失（如 need_config=true 但租户未配置） | 400 |
| `40101` | 认证失败 / Token 无效 | 401 |
| `40301` | 无权限调用此插件 | 403 |
| `40302` | 余额不足 | 403 |
| `50001` | 插件内部错误 | 500 |
| `50002` | 插件依赖服务不可用 | 503 |
| `50003` | 插件处理超时 | 504 |

错误响应格式：

```json
{
  "code": 40302,
  "data": null,
  "token_used": 0,
  "balance_remaining": 0,
  "message": "余额不足，当前余额 50，需要 100"
}
```

### 3.6 健康检查端点（可选但建议）

建议插件实现健康检查端点，供平台定期检测插件可用性：

```
GET {backend_api}/health
```

响应示例：

```json
{
  "status": "ok",
  "version": "1.0.0",
  "uptime": 86400
}
```

### 3.7 异步处理机制（可选）

如果插件需要异步处理（如批量任务），可采用以下模式：

**方案 A：回调模式**

插件在响应中返回 `task_id`，并立即返回成功。平台可通过前端通知租户任务已提交。

```json
{
  "code": 0,
  "data": {
    "task_id": "task_xyz789",
    "status": "pending"
  },
  "token_used": 100,
  "balance_remaining": 11900,
  "message": "任务已提交"
}
```

插件应提供任务状态查询接口：

```
GET {backend_api}/task/{task_id}
```

**方案 B：WebHook 回调**

插件配置中可指定 `callback_url`，任务完成后插件主动回调平台接口通知结果（具体接口由平台与插件方协商）。

---

## 4. 前端嵌入规范

### 4.1 隔离方式

插件前端通过 **iframe** 嵌入主框架页面，实现样式和行为隔离。

```
┌──────────────────────────────────────────┐
│              主框架页面                    │
│  ┌────────────────────────────────────┐  │
│  │  插件 iframe                        │  │
│  │  ┌──────────────────────────────┐  │  │
│  │  │  插件自己的 HTML/CSS/JS      │  │  │
│  │  └──────────────────────────────┘  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

### 4.2 通信协议（postMessage）

主框架与插件 iframe 之间通过 `window.postMessage` API 进行双向通信。

#### 4.2.1 消息格式约定

所有消息均为 JSON 结构，包含 `type` 和 `payload` 字段：

```javascript
// 发送消息
window.parent.postMessage({ type: 'PLUGIN_READY', payload: {} }, '*');
```

#### 4.2.2 主框架 → 插件 的消息

| type | 说明 | payload 示例 |
|------|------|--------------|
| `INIT` | 主框架向插件发送初始化数据（插件应在收到此消息后完成初始化） | `{ tenantId, token, apiBaseUrl, theme, config }` |
| `THEME_CHANGE` | 主框架通知主题变更 | `{ theme: 'dark' \| 'light' }` |
| `INVOKE` | 主框架要求插件执行特定操作（如调用 AI） | `{ action: 'generate', args: {...} }` |

**INIT 消息示例**：

```javascript
{
  "type": "INIT",
  "payload": {
    "tenantId": "tenant_abc123",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "apiBaseUrl": "https://api.platform.example.com",
    "theme": "dark",
    "config": {
      "api_key": "sk-xxx"
    }
  }
}
```

**THEME_CHANGE 消息示例**：

```javascript
{
  "type": "THEME_CHANGE",
  "payload": {
    "theme": "dark"
  }
}
```

#### 4.2.3 插件 → 主框架 的消息

| type | 说明 | payload 示例 |
|------|------|--------------|
| `PLUGIN_READY` | 插件加载完成，可交互 | `{}` |
| `INVOKE_RESULT` | 插件调用 AI 功能的结果 | `{ success, data, tokenUsed, balanceRemaining, message }` |
| `ERROR` | 插件发生错误 | `{ code, message }` |
| `PLUGIN_STATUS` | 插件状态变更（如加载中、计算中） | `{ status: 'loading' \| 'computing' }` |

**PLUGIN_READY 消息示例**：

```javascript
{
  "type": "PLUGIN_READY",
  "payload": {}
}
```

**INVOKE_RESULT 消息示例**：

```javascript
{
  "type": "INVOKE_RESULT",
  "payload": {
    "success": true,
    "data": {
      "result": "生成的文案内容"
    },
    "tokenUsed": 50,
    "balanceRemaining": 11950,
    "message": "生成成功"
  }
}
```

### 4.3 插件前端开发示例

```javascript
// index.html 中的插件脚本

let currentTheme = 'light';
let platformContext = null;

// 监听主框架发来的消息
window.addEventListener('message', async (event) => {
  const { type, payload } = event.data;

  switch (type) {
    case 'INIT':
      // 保存平台上下文
      platformContext = payload;
      currentTheme = payload.theme || 'light';
      applyTheme(currentTheme);

      // 初始化完成后通知主框架
      window.parent.postMessage({ type: 'PLUGIN_READY', payload: {} }, '*');
      break;

    case 'THEME_CHANGE':
      currentTheme = payload.theme;
      applyTheme(currentTheme);
      break;

    case 'INVOKE':
      await handleInvoke(payload);
      break;
  }
});

// 应用主题
function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
}

// 处理 AI 调用请求
async function handleInvoke(payload) {
  const { action, args } = payload;

  try {
    // 向插件后端发起请求
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-Id': platformContext.tenantId,
        'Authorization': `Bearer ${platformContext.token}`
      },
      body: JSON.stringify({ prompt: args.prompt })
    });

    const result = await response.json();

    // 通过 postMessage 返回结果给主框架
    window.parent.postMessage({
      type: 'INVOKE_RESULT',
      payload: {
        success: result.code === 0,
        data: result.data,
        tokenUsed: result.token_used,
        balanceRemaining: result.balance_remaining,
        message: result.message
      }
    }, '*');
  } catch (error) {
    window.parent.postMessage({
      type: 'ERROR',
      payload: {
        code: 50001,
        message: error.message
      }
    }, '*');
  }
}
```

### 4.4 前端样式约束

#### 4.4.1 CSS 变量（主框架提供）

插件**必须**使用主框架提供的 CSS 变量来保持主题一致性：

```css
/* 浅色模式变量 */
:root {
  --color-bg: #ffffff;
  --color-text: #111111;
  --color-border: #e5e5e5;
  --color-primary: #3b82f6;
  --color-danger: #ef4444;
}

/* 深色模式变量 */
[data-theme="dark"] {
  --color-bg: #0a0a0a;
  --color-text: #f5f5f5;
  --color-border: #2a2a2a;
  --color-primary: #3b82f6;
  --color-danger: #ef4444;
}

/* 使用示例 */
.plugin-container {
  background-color: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
```

#### 4.4.2 样式约束

- **禁止**在插件内使用主框架同名的 CSS 类名，防止样式冲突。
- **建议**为插件根元素添加唯一 class 或 id 前缀，如 `.my-plugin-*`。
- **建议**使用相对单位（`rem`、`em`）而非固定像素，确保在主框架中正常缩放。
- **禁止**使用 `!important` 强制覆盖主框架样式。

---

## 5. 插件部署与注册

### 5.1 部署方式

插件后端可独立部署，以下方式均可：

| 部署方式 | 说明 |
|----------|------|
| **Docker 容器** | 提供 Dockerfile，平台可一键部署 |
| **云函数** | 如阿里云 FC、腾讯云 SCF |
| **独立 HTTP 服务** | 任意可访问的 HTTP/HTTPS 端点 |

### 5.2 插件包结构

上传到管理后台的插件包应包含以下文件：

```
acme-hello-world/
├── manifest.json          # 插件描述文件
├── README.md              # 插件使用说明
├── icon.png               # 插件图标（64×64）
└── backend/               # （可选）后端部署配置
    ├── Dockerfile
    └── docker-compose.yml
```

前端静态资源由插件自行托管（或上传到平台对象存储），在 `manifest.json` 中指定 `frontend_entry` URL。

### 5.3 注册流程

插件上架主平台的标准流程如下：

```
┌─────────────┐
│ 1. 开发     │
│ manifest.json  │
│ 前端 + 后端  │
└──────┬──────┘
       ↓
┌─────────────┐
│ 2. 上传     │
│ 管理员在后台上传 │
│ 插件包      │
└──────┬──────┘
       ↓
┌─────────────┐
│ 3. 解析验证 │
│ 平台解析     │
│ manifest.json │
└──────┬──────┘
       ↓
┌─────────────┐
│ 4. 测试状态 │
│ 插件状态 = │
│ "testing"   │
└──────┬──────┘
       ↓
┌─────────────┐
│ 5. 测试通过 │
│ 管理员验证   │
│ 功能和权限   │
└──────┬──────┘
       ↓
┌─────────────┐
│ 6. 上架发布  │
│ 插件状态 =  │
│ "published"  │
└─────────────┘
```

### 5.4 管理后台操作

在平台管理后台，管理员可以：

- **上传插件**：上传包含 `manifest.json` 的插件包
- **查看插件**：查看插件详情、配置字段、调用统计
- **设置插件状态**：`testing` → `published` → `disabled`
- **配置租户插件**：为特定租户配置插件参数（如 API Key）
- **下线插件**：将插件状态设为 `disabled`，已激活的租户不受影响

---

## 6. 计费与权限

### 6.1 计费流程

平台网关在调用插件前后的完整计费流程：

```
┌──────────────┐
│ 1. 租户发起   │
│   插件调用请求 │
└──────┬───────┘
       ↓
┌──────────────┐
│ 2. 平台网关   │
│  检查租户余额  │
└──────┬───────┘
       ↓
    余额充足？
    ├── 否 → 返回 40302 余额不足
    └── 是 → 继续
       ↓
┌──────────────┐
│ 3. 调用插件   │
│   后端 API   │
└──────┬───────┘
       ↓
┌──────────────┐
│ 4. 插件返回  │
│  token_used  │
└──────┬───────┘
       ↓
┌──────────────┐
│ 5. 平台网关  │
│  扣减余额    │
└──────┬───────┘
       ↓
┌──────────────┐
│ 6. 返回结果  │
│  给租户前端  │
└──────────────┘
```

### 6.2 插件计费职责

插件后端**只需**正确返回 `token_used` 字段，平台网关自动完成余额校验和扣费。

插件不应：
- 直接查询或修改租户余额
- 实现独立的计费逻辑
- 在 `token_used` 中返回虚假数值（平台有审计机制）

### 6.3 权限控制

#### 6.3.1 角色可见性（visible_to）

在 `manifest.json` 中定义 `visible_to` 字段，控制插件对哪些角色可见：

```json
{
  "visible_to": ["merchant", "admin"]
}
```

| 角色 | 说明 |
|------|------|
| `admin` | 平台管理员 |
| `merchant` | 商家/租户 |
| `user` | 普通用户（未来扩展） |

#### 6.3.2 调用权限（required_role）

`required_role` 字段定义调用插件所需的最小角色：

```json
{
  "required_role": "merchant"
}
```

表示只有 `merchant` 及以上角色可以调用此插件。

#### 6.3.3 租户配置权限（need_config）

若插件需要租户提供配置（如 API Key），设置 `need_config: true`：

```json
{
  "need_config": true,
  "config_fields": [
    {
      "key": "api_key",
      "label": "AI API Key",
      "type": "password",
      "required": true
    }
  ]
}
```

平台会在租户激活插件时要求填写配置，未完成配置的租户无法使用该插件。

---

## 7. 错误处理与日志

### 7.1 错误处理原则

- 插件应返回清晰的错误码和 `message`，避免返回 HTML 错误页或空响应。
- 所有错误响应必须符合[响应格式约定](#34-响应格式response)。
- 建议插件在业务逻辑中捕获异常并转换为结构化错误返回。

### 7.2 日志规范

建议插件实现结构化日志，便于平台监控和排查问题：

```json
{
  "timestamp": "2026-05-22T10:30:00.000Z",
  "level": "info",
  "tenant_id": "tenant_abc123",
  "plugin_id": "acme.hello-world",
  "action": "invoke",
  "token_used": 100,
  "latency_ms": 250,
  "message": "Invoke success"
}
```

| 字段 | 说明 |
|------|------|
| `timestamp` | ISO 8601 时间戳 |
| `level` | 日志级别：`debug` / `info` / `warn` / `error` |
| `tenant_id` | 租户标识 |
| `plugin_id` | 插件标识 |
| `action` | 操作类型 |
| `token_used` | 本次消耗 Token |
| `latency_ms` | 处理耗时（毫秒） |
| `message` | 日志消息 |

### 7.3 健康检查

建议插件实现 `/health` 端点，返回服务健康状态：

```json
{
  "status": "ok",
  "version": "1.0.0",
  "uptime": 86400,
  "dependencies": {
    "database": "ok",
    "cache": "ok"
  }
}
```

- `status` 为 `ok` 时表示健康，`error` 时表示异常
- 平台会定期探测此端点，若插件不可用会自动告警

---

## 8. 附录：Hello World 示例插件

### 8.1 插件概述

这是一个最简化的全栈插件示例，包含：

- **前端**：一个输入框 + 发送按钮，通过 iframe 嵌入
- **后端**：一个 `/api/chat` 接口，返回模拟的 AI 响应

### 8.2 插件包目录结构

```
hello-world-plugin/
├── manifest.json
├── frontend/
│   ├── index.html
│   └── style.css
└── backend/
    ├── server.js
    └── package.json
```

### 8.3 manifest.json

```json
{
  "plugin_id": "acme.hello-world",
  "name": "Hello World 插件",
  "version": "1.0.0",
  "description": "濮院毛衫 AI 平台的 Hello World 示例插件，展示平台插件开发规范。",
  "icon_url": "https://plugin.example.com/icon.png",
  "billing_type": "token",
  "default_token_cost": 10,
  "frontend_entry": "https://plugin.example.com/frontend/index.html",
  "backend_api": "https://plugin.example.com/api",
  "need_config": false,
  "visible_to": [],
  "required_role": "merchant",
  "config_fields": [],
  "health_endpoint": "/health"
}
```

### 8.4 前端：index.html

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hello World 插件</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="plugin-root" id="pluginRoot">
    <div class="plugin-header">
      <h2>Hello World</h2>
    </div>

    <div class="plugin-body">
      <div class="output-area" id="outputArea">
        <p class="placeholder">输入内容后点击发送...</p>
      </div>

      <div class="input-row">
        <input
          type="text"
          id="promptInput"
          class="prompt-input"
          placeholder="请输入想说的话..."
          autocomplete="off"
        />
        <button id="sendBtn" class="send-btn">发送</button>
      </div>
    </div>
  </div>

  <script>
    // ============================================
    // 平台通信层
    // ============================================
    let platformContext = null;

    // 监听主框架发来的消息
    window.addEventListener('message', async (event) => {
      const { type, payload } = event.data || {};

      switch (type) {
        case 'INIT':
          platformContext = payload;
          applyTheme(platformContext.theme || 'light');
          // 初始化完成后通知主框架
          window.parent.postMessage({ type: 'PLUGIN_READY', payload: {} }, '*');
          break;

        case 'THEME_CHANGE':
          applyTheme(payload.theme);
          break;

        case 'INVOKE':
          await handleInvoke(payload);
          break;
      }
    });

    // ============================================
    // 业务逻辑层
    // ============================================
    async function handleInvoke(payload) {
      const { action, args } = payload;

      if (action === 'echo') {
        const result = { echo: args.message };
        window.parent.postMessage({
          type: 'INVOKE_RESULT',
          payload: {
            success: true,
            data: result,
            tokenUsed: 1,
            balanceRemaining: platformContext.balance || 0,
            message: 'Echo 成功'
          }
        }, '*');
      }
    }

    async function sendMessage() {
      const input = document.getElementById('promptInput');
      const outputArea = document.getElementById('outputArea');
      const message = input.value.trim();

      if (!message) return;

      // 显示用户输入
      outputArea.innerHTML = `<div class="msg user-msg"><span>${escapeHtml(message)}</span></div>`;

      // 通知主框架开始调用
      window.parent.postMessage({
        type: 'PLUGIN_STATUS',
        payload: { status: 'computing' }
      }, '*');

      try {
        // 调用插件后端
        const response = await fetch('/api/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Tenant-Id': platformContext.tenantId,
            'Authorization': `Bearer ${platformContext.token}`
          },
          body: JSON.stringify({ prompt: message })
        });

        const result = await response.json();

        if (result.code === 0) {
          outputArea.innerHTML += `<div class="msg bot-msg"><span>${escapeHtml(result.data.reply)}</span></div>`;

          // 返回结果给主框架
          window.parent.postMessage({
            type: 'INVOKE_RESULT',
            payload: {
              success: true,
              data: result.data,
              tokenUsed: result.token_used,
              balanceRemaining: result.balance_remaining,
              message: result.message
            }
          }, '*');
        } else {
          throw new Error(result.message);
        }
      } catch (error) {
        window.parent.postMessage({
          type: 'ERROR',
          payload: {
            code: 50001,
            message: error.message
          }
        }, '*');
      }

      input.value = '';
    }

    // ============================================
    // 主题与样式
    // ============================================
    function applyTheme(theme) {
      document.getElementById('pluginRoot').setAttribute('data-theme', theme);
    }

    function escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    }

    // 绑定事件
    document.getElementById('sendBtn').addEventListener('click', sendMessage);
    document.getElementById('promptInput').addEventListener('keypress', (e) => {
      if (e.key === 'Enter') sendMessage();
    });
  </script>
</body>
</html>
```

### 8.5 前端：style.css

```css
/* 使用主框架提供的 CSS 变量 */
:root {
  --color-bg: #ffffff;
  --color-text: #111111;
  --color-border: #e5e5e5;
  --color-primary: #3b82f6;
  --color-primary-hover: #2563eb;
  --color-danger: #ef4444;
  --radius: 8px;
  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

[data-theme="dark"] {
  --color-bg: #0a0a0a;
  --color-text: #f5f5f5;
  --color-border: #2a2a2a;
  --color-primary: #3b82f6;
  --color-primary-hover: #60a5fa;
  --color-danger: #ef4444;
}

.plugin-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 300px;
  background-color: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font-family: var(--font-sans);
  overflow: hidden;
}

.plugin-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}

.plugin-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.plugin-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  gap: 12px;
}

.output-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.placeholder {
  color: #888;
  font-size: 14px;
  text-align: center;
  margin-top: 20px;
}

.msg {
  padding: 8px 12px;
  border-radius: var(--radius);
  font-size: 14px;
  max-width: 80%;
  word-break: break-word;
}

.user-msg {
  align-self: flex-end;
  background-color: var(--color-primary);
  color: #fff;
}

.bot-msg {
  align-self: flex-start;
  background-color: var(--color-border);
  color: var(--color-text);
}

.input-row {
  display: flex;
  gap: 8px;
}

.prompt-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background-color: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.prompt-input:focus {
  border-color: var(--color-primary);
}

.send-btn {
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius);
  background-color: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-btn:hover {
  background-color: var(--color-primary-hover);
}
```

### 8.6 后端：server.js

```javascript
const http = require('http');
const url = require('url');

const PORT = process.env.PORT || 3000;

// 模拟租户余额（生产环境中由平台网关管理）
const tenantBalances = {};

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // CORS 头
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Tenant-Id, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // 健康检查端点
  if (pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', version: '1.0.0', uptime: process.uptime() }));
    return;
  }

  // 主调用接口
  if (pathname === '/api/chat' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const { prompt } = JSON.parse(body);
        const tenantId = req.headers['x-tenant-id'] || 'anonymous';

        // 计算 Token（简单按字符数估算）
        const tokenUsed = Math.max(10, prompt.length);
        const balanceRemaining = (tenantBalances[tenantId] || 10000) - tokenUsed;
        tenantBalances[tenantId] = balanceRemaining;

        // 模拟 AI 响应
        const reply = `你好！你说：「${prompt}」\n这是一条来自 Hello World 插件的模拟回复。`;

        const response = {
          code: 0,
          data: { reply },
          token_used: tokenUsed,
          balance_remaining: balanceRemaining,
          message: 'success'
        };

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(response));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          code: 40001,
          data: null,
          token_used: 0,
          balance_remaining: 0,
          message: '请求格式错误'
        }));
      }
    });
    return;
  }

  // 404
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ code: 40401, message: 'Not Found' }));
});

server.listen(PORT, () => {
  console.log(`Hello World Plugin running on port ${PORT}`);
});
```

### 8.7 后端：package.json

```json
{
  "name": "hello-world-plugin-backend",
  "version": "1.0.0",
  "description": "Hello World 插件后端服务",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "node --watch server.js"
  },
  "engines": {
    "node": ">=18.0.0"
  }
}
```

### 8.8 后端 Dockerfile（可选）

```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY server.js ./

EXPOSE 3000

CMD ["node", "server.js"]
```

---

## 修订记录

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0.0 | 2026-05-22 | 初始版本 |

---

> 本文档最后由濮院毛衫 AI 平台生成。如有疑问，请联系平台技术支持。