# 濮院毛衫 AI 平台 - 项目记忆文件

## 项目概述
多租户 SaaS 平台，为濮院毛衫商家提供 AI 工具服务（图片生成、脚本生成、跨境翻译）。

## 最新任务记录（2026-05-19）

### 已完成：Spring AI 接入与三个 MVP 插件实现 ✅

#### 后端实现

**1. 添加 Spring AI 依赖**
- 文件：`backend/java-spring/pom.xml`
- 添加了 `spring-ai-openai-spring-boot-starter` 和 `spring-ai-ollama-spring-boot-starter`
- 版本：1.0.0-M5
- 添加 Spring Milestones 仓库

**2. 配置 AI 模型**
- 文件：`backend/java-spring/src/main/resources/application-dev.yml`
- 添加 OpenAI 配置项（API Key、图片模型、聊天模型）
- 支持 Mock 模式（`AI_MOCK_ENABLED=true`）
- 配置 Mock 响应内容（图片 URL、脚本模板）

**3. 创建插件相关 DTO**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/PluginDtos.java`
- 定义了三个插件的请求/响应 DTO：
  - `AiImageGenRequest/Response`：图片生成
  - `AiScriptGenRequest/Response`：脚本生成
  - `AiTranslateRequest/Response`：翻译
  - `PluginInvokeRequest/Response`：统一调用格式

**4. 实现 AI Services**
- `AiImageService` + `AiImageServiceImpl`：图片生成服务
  - 支持多种尺寸（512x512, 1024x1024, 1792x1024, 1024x1792）
  - 根据尺寸计算 Token 费用
- `AiScriptService` + `AiScriptServiceImpl`：脚本生成服务
  - 固定费用 20 Tokens
- `AiTranslateService` + `AiTranslateServiceImpl`：翻译服务
  - 支持英语、泰语、越南语、马来语、印尼语
  - 费用根据文本长度计算（每 10 字符 1 Token，最少 5 Tokens）

**5. 扩展 AccountWalletService**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/AccountWalletService.java`
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/impl/AccountWalletServiceImpl.java`
- 新增方法：
  - `deductToken(tenantId, tokenCost, pluginCode)`：扣费
  - `getBalance(tenantId)`：查询余额

**6. 创建统一插件调用入口**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/controller/PluginInvokeController.java`
- 接口：`POST /api/plugin/invoke/{pluginCode}`
- 支持的 pluginCode：
  - `ai_image_gen`：图片生成
  - `ai_script_gen`：脚本生成
  - `ai_translate`：翻译
- 功能：
  - 验证插件存在且已上架
  - 验证租户已启用该插件
  - 计算费用并扣费
  - 调用对应 AI Service
  - 返回结果 + 消耗 + 剩余余额

**7. 数据库种子脚本**
- 文件：`backend/sql/ai_plugins_seed.sql`
- 插入三个 AI 插件到 `plugin` 表：
  - ai_image_gen (ID: 3010)
  - ai_script_gen (ID: 3011)
  - ai_translate (ID: 3012)
- 为示例租户（2001, 2002）启用所有插件

#### 前端实现（租户端）

**8. API 封装**
- 文件：`frontend/merchant-web/src/api/plugin.ts`
- `invokePlugin(pluginCode, params)`：调用插件接口
- `getBalance()`：查询余额

**9. 创建三个 AI 工具页面**
- `AiImageGen.vue`：AI 商品图生成
  - 输入提示词
  - 选择图片尺寸
  - 显示消耗的 Token
  - 展示生成的图片
- `AiScriptGen.vue`：AI 视频脚本生成
  - 输入商品描述
  - 可选商品链接
  - 选择脚本类型（短视频/直播/详情页）
  - 展示生成的脚本，支持复制
- `AiTranslate.vue`：AI 跨境翻译
  - 输入待翻译文本
  - 选择目标语言
  - 实时显示 Token 消耗
  - 展示翻译结果，支持复制

**10. 更新路由配置**
- 文件：`frontend/merchant-web/src/router/routes.ts`
- 新增 `/ai-tools` 分组：
  - `/ai-tools/image-gen`
  - `/ai-tools/script-gen`
  - `/ai-tools/translate`
- 保留原有 `/plugins/ai-image` 和 `/plugins/ai-script` 路由（兼容性）

## 技术架构

### 后端
- Java 21 + Spring Boot 3.3 + MyBatis Plus
- Spring AI 1.0.0-M5（Mock 模式）
- MySQL 8.0

### 前端
- Vue 3 + TypeScript + Vite
- Pinia（状态管理）
- 无 UI 框架（原生 CSS）

## 启动和测试

### 启动后端
```bash
cd backend/java-spring
mvn clean package
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 初始化数据库
```bash
mysql -u root -p puyuan_ai_mvp < backend/sql/ai_plugins_seed.sql
```

### 启动前端
```bash
cd frontend/merchant-web
npm run dev
```

### 测试 Mock 模式
- 默认启用 Mock 模式（`AI_MOCK_ENABLED=true`）
- 图片生成返回占位图 URL
- 脚本生成返回预定义模板
- 翻译返回带语言后缀的文本

### 切换到真实 AI 模式
1. 在环境变量中设置：
   ```bash
   export OPENAI_API_KEY=your_api_key
   export OPENAI_BASE_URL=https://api.openai.com
   export AI_MOCK_ENABLED=false
   ```
2. 重启后端服务

## 计费规则

| 插件 | 计费方式 | 费用 |
|------|---------|------|
| AI 图片生成 | 按尺寸 | 10-30 Tokens |
| AI 脚本生成 | 固定 | 20 Tokens |
| AI 跨境翻译 | 按长度 | 每 10 字符 1 Token（最少 5） |

## 已知限制

1. Mock 模式下，所有 AI 返回内容是固定的占位数据
2. 真实 AI 模式需要 OpenAI API Key
3. 前端未实现错误码详细映射，仅显示通用错误信息

## 后续优化方向

1. 集成真实 OpenAI API
2. 添加请求日志和监控
3. 实现异步任务处理（图片生成可能耗时较长）
4. 添加请求重试机制
5. 优化前端 UI/UX

---

## 最新任务记录（2026-05-20）

### 已完成：微信授权登录功能 ✅

#### 小程序端实现

**1. 修改登录页面**
- 文件：`frontend/merchant-miniapp/src/pages/login/index.tsx`
- 移除手机号+验证码登录
- 实现微信授权登录（`Taro.login()` + `onGetUserInfo`）
- 绿色按钮样式（微信主题色）

**2. 更新 API 服务**
- 文件：`frontend/merchant-miniapp/src/services/auth.ts`
- 新增 `WxLoginRequest` 接口（code + userInfo）
- 新增 `wxLogin()` 方法调用后端接口

#### 后端实现

**3. 创建微信登录 DTO**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/WxLoginDtos.java`
- `JsCode2SessionResponse` - 微信 jscode2session 响应

**4. 扩展 ApiModels**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/ApiModels.java`
- 新增 `WxLoginRequest` 类（code + userInfo）
- `WxUserInfo` - 用户信息（昵称、头像等）

**5. 创建微信登录服务**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/WxLoginService.java`
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/impl/WxLoginServiceImpl.java`
- `handleWxLogin()` - 处理微信登录逻辑
- `getOpenIdByCode()` - 调用微信接口获取 openId
- 自动创建新用户和租户
- 分配默认免费配额（1000 Tokens）
- 启用默认插件（AI 图片生成、AI 脚本生成、AI 跨境翻译）

**6. 创建微信登录控制器**
- 文件：`backend/java-spring/src/main/java/com/puyuanmaoshan/platform/controller/WxLoginController.java`
- 接口：`POST /api/v1/auth/wx_login`
- 返回 LoginResponse（accessToken, userId, tenantId, roleCode）

**7. 配置微信小程序参数**
- 文件：`backend/java-spring/src/main/resources/application-dev.yml`
- 新增配置项：
  ```yaml
  wx:
    miniapp:
      app-id: ${WX_MINIAPP_APPID:your_appid_here}
      secret: ${WX_MINIAPP_SECRET:your_secret_here}
  ```

#### 新用户默认配置

| 配置项 | 默认值 |
|--------|--------|
| 租户级别 | free（免费版） |
| 租户类型 | individual（个人） |
| 默认角色 | boss |
| Token 余额 | 1000 |
| 启用插件 | ai_image_gen, ai_script_gen, ai_translate |

#### 文档

- `frontend/merchant-miniapp/WX_LOGIN_GUIDE.md` - 微信登录配置和测试指南