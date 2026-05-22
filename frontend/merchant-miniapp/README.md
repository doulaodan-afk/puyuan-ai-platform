# 濮院毛衫 AI 平台 - 租户端微信小程序

基于 Taro 4.x + Vue 3 + Pinia 开发的微信小程序。

## 技术栈

- **框架**: Taro 4.x
- **UI**: 原生微信小程序 UI（简洁设计）
- **状态管理**: Pinia
- **语言**: TypeScript
- **构建工具**: Webpack 5

## 开发指南

### 1. 安装依赖

```bash
cd frontend/merchant-miniapp
npm install
```

### 2. 配置小程序 AppID

编辑 `project.config.json`，将 `appid` 字段改为你自己的小程序 AppID。

### 3. 配置后端 API 地址

编辑 `src/utils/config.ts`，设置正确的 API_BASE_URL。

### 4. 启动开发服务器

```bash
npm run dev:weapp
```

### 5. 导入微信开发者工具

1. 打开微信开发者工具
2. 导入项目，选择 `frontend/merchant-miniapp/dist` 目录
3. AppID 填写你的小程序 AppID

### 6. 构建生产版本

```bash
npm run build:weapp
```

## 项目结构

```
src/
├── app.config.ts       # Taro 应用配置
├── app.ts              # 应用入口
├── pages/              # 页面
│   ├── index/          # 首页/工作台
│   ├── login/          # 登录页
│   ├── plugins/        # 插件列表
│   ├── ai-image/       # AI 图片生成
│   ├── ai-script/      # AI 脚本生成
│   ├── ai-translate/   # 跨境翻译
│   ├── account/        # 账户模块
│   └── billing/        # 账单中心
├── components/         # 组件
├── services/           # API 服务
├── stores/             # Pinia 状态管理
├── utils/              # 工具函数
└── styles/             # 全局样式
```

## 核心功能

- **微信授权登录** - 使用微信账号快速登录
- 工作台（展示已启用的插件）
- AI 图片生成
- AI 脚本生成
- 跨境翻译
- 账户余额
- 充值
- 消费明细
- 账单中心
- 深色模式

## 后端 API 依赖

- POST /api/v1/auth/wx_login - 微信授权登录
- GET /api/v1/tenant/profile
- POST /api/plugin/invoke/{pluginCode}
- GET /api/v1/account/balance
- GET /api/v1/account/ledger
- POST /api/v1/account/recharge/orders
- GET /api/v1/account/recharge/orders

## 微信登录配置

### 后端配置

在 `backend/java-spring/src/main/resources/application-dev.yml` 中配置：

```yaml
wx:
  miniapp:
    app-id: ${WX_MINIAPP_APPID:your_appid_here}
    secret: ${WX_MINIAPP_SECRET:your_secret_here}
```

或通过环境变量设置：

```bash
export WX_MINIAPP_APPID=wx1234567890abcdef
export WX_MINIAPP_SECRET=abcdef1234567890
```

### 小程序端配置

在 `project.config.json` 中配置 AppID：

```json
{
  "appid": "your_actual_appid_here"
}
```

详细配置说明请参考 [WX_LOGIN_GUIDE.md](./WX_LOGIN_GUIDE.md)。

## 新用户默认配置

首次微信授权登录的用户将自动创建：

- **租户**: 免费版个人工作室
- **角色**: 老板（所有权限）
- **Token 余额**: 1000 Tokens
- **启用插件**: AI 图片生成、AI 脚本生成、AI 跨境翻译
