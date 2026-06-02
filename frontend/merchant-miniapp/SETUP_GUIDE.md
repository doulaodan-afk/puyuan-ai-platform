# 濮院毛衫 AI 平台 - 微信小程序启动指南

## 一、项目结构

```
frontend/merchant-miniapp/
├── src/
│   ├── pages/              # 页面
│   │   ├── index/         # 工作台首页
│   │   ├── login/         # 登录页
│   │   ├── plugins/       # 插件列表
│   │   ├── ai-image/      # AI 图片生成
│   │   ├── ai-script/     # AI 脚本生成
│   │   ├── ai-translate/  # 跨境翻译
│   │   ├── account/       # 账户管理
│   │   ├── recharge/      # 充值中心
│   │   ├── ledger/        # 消费明细
│   │   └── billing/       # 账单中心
│   ├── stores/            # Pinia 状态管理
│   │   ├── auth.ts        # 认证状态
│   │   ├── theme.ts       # 主题状态
│   │   └── account.ts     # 账户状态
│   ├── services/          # API 服务
│   │   ├── auth.ts        # 认证 API
│   │   ├── plugin.ts      # 插件 API
│   │   ├── account.ts     # 账户 API
│   │   └── billing.ts     # 账单 API
│   ├── utils/             # 工具函数
│   │   ├── config.ts      # 配置
│   │   ├── request.ts     # 网络请求
│   │   └── storage.ts     # 存储
│   ├── styles/            # 全局样式
│   │   ├── global.scss    # 全局样式
│   │   └── variables.scss # 样式变量
│   ├── app.config.ts      # 应用配置
│   └── app.ts             # 应用入口
├── config/
│   └── index.js           # Taro 配置
├── project.config.json    # 微信小程序配置
├── package.json
├── tsconfig.json
└── theme.json            # 主题配置
```

## 二、安装依赖

```bash
cd frontend/merchant-miniapp
npm install
```

## 三、配置

### 1. 配置后端 API 地址

编辑 `src/utils/config.ts`，修改 `API_BASE_URL`：

```typescript
export const CONFIG = {
  // 开发环境
  API_BASE_URL: 'http://localhost:8080',

  // 生产环境
  // API_BASE_URL: 'https://api.puyuan-ai.com',
};
```

### 2. 配置小程序 AppID

编辑 `project.config.json`，将 `appid` 改为你的小程序 AppID：

```json
{
  "appid": "your_appid_here"
}
```

## 四、启动开发

### 1. 编译小程序

```bash
npm run dev:weapp
```

编译后的文件在 `dist/` 目录。

### 2. 导入微信开发者工具

1. 打开微信开发者工具
2. 选择「导入项目」
3. 项目目录选择 `frontend/merchant-miniapp/dist`
4. AppID 填写你的小程序 AppID
5. 点击「导入」

### 3. 开发调试

- 修改代码后，Taro 会自动重新编译
- 在微信开发者工具中可以看到实时更新
- 使用「调试器」查看 console 输出和网络请求

## 五、功能说明

### 已实现的功能

| 功能 | 页面路径 | 说明 |
|------|---------|------|
| 登录/注册 | /pages/login/index | 手机号 + 验证码登录 |
| 工作台 | /pages/index/index | 展示余额、插件、快捷入口 |
| 插件列表 | /pages/plugins/index | 已启用/全部插件 |
| AI 图片生成 | /pages/ai-image/index | 输入提示词、选择尺寸、生成图片 |
| AI 脚本生成 | /pages/ai-script/index | 商品描述、链接、脚本类型 |
| 跨境翻译 | /pages/ai-translate/index | 中文翻译为多国语言 |
| 账户管理 | /pages/account/index | 余额、存储、菜单 |
| 充值中心 | /pages/recharge/index | 选择套餐、创建订单 |
| 消费明细 | /pages/ledger/index | 消费记录列表 |
| 账单中心 | /pages/billing/index | 账单列表、支付 |

### API 接口

小程序复用以下后端接口：

- `POST /api/v1/auth/login` - 登录
- `GET /api/v1/tenant/profile` - 租户信息
- `POST /api/plugin/invoke/{pluginCode}` - 插件调用
- `GET /api/v1/account/balance` - 余额查询
- `GET /api/v1/account/ledger` - 消费明细
- `POST /api/v1/account/recharge/orders` - 创建充值订单
- `GET /api/v1/account/recharge/orders` - 充值订单列表

## 六、深色模式

小程序支持系统深色模式：

1. 在「设置」中切换深色模式
2. 小程序会自动跟随系统设置
3. 主题配置在 `theme.json` 中

## 七、常见问题

### 1. 编译失败

检查 Node.js 版本是否 >= 18.0.0：
```bash
node -v
```

### 2. 网络请求失败

1. 确认后端服务已启动
2. 检查 `API_BASE_URL` 配置是否正确
3. 微信开发者工具需要开启「不校验合法域名」：
   - 点击「详情」
   - 勾选「不校验合法域名...」

### 3. 登录失败

1. 确认后端数据库已初始化
2. 使用测试账号登录（参考后端 SQL 脚本）

### 4. Token 不足

1. 进入「账户」页面
2. 点击「立即充值」
3. 选择套餐并完成支付

## 八、构建生产版本

```bash
npm run build:weapp
```

编译后的文件在 `dist/` 目录，可直接上传到微信小程序后台。

## 九、后端支持补充

以下功能需要后端补充支持：

### 1. 充值订单支付回调

需要添加微信支付回调接口：
```
POST /api/v1/account/recharge/orders/{orderNo}/callback
```

### 2. 验证码接口

当前登录接口支持手机号 + 验证码登录，需要后端实现：
```
POST /api/v1/auth/send-code
```

### 3. 账单支付接口

需要实现账单支付功能，可能需要对接微信小程序支付：
```
POST /api/v1/billing/bills/{billNo}/pay
```

## 十、联系方式

如有问题，请联系开发团队。
