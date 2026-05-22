# 微信授权登录实现指南

## 一、小程序端修改

### 1. 修改的文件

#### `/src/pages/login/index.tsx`
- 移除了手机号+验证码登录逻辑
- 实现微信授权登录（`wx.login` + `onGetUserInfo`）
- 点击授权按钮后获取 code，调用后端接口 `/api/v1/auth/wx_login`

#### `/src/pages/login/index.scss`
- 更新为微信授权登录样式
- 绿色按钮（微信主题色）
- 用户协议和隐私政策链接

#### `/src/services/auth.ts`
- 新增 `WxLoginRequest` 接口
- 新增 `wxLogin()` 方法调用后端接口

### 2. 小程序端代码流程

```typescript
// 1. 用户点击微信授权登录
Taro.login() -> 获取 code

// 2. 调用后端接口
authService.wxLogin({ code, userInfo })

// 3. 保存认证信息
authStore.setAuthData(response)

// 4. 跳转首页
Taro.switchTab({ url: '/pages/index/index' })
```

### 3. 注意事项

- 需要在微信小程序后台配置「用户隐私保护指引」
- 需要在 `project.config.json` 中配置正确的 AppID
- 测试时需要使用真机或微信开发者工具

---

## 二、后端修改

### 1. 新增文件

#### `/dto/WxLoginDtos.java`
- `JsCode2SessionResponse` - 微信 jscode2session 接口响应

#### `/service/WxLoginService.java`
- `handleWxLogin()` - 处理微信登录
- `getOpenIdByCode()` - 调用微信接口获取 openId

#### `/service/impl/WxLoginServiceImpl.java`
- 实现微信登录服务
- 创建新用户、租户、钱包
- 启用默认插件（AI 图片生成、AI 脚本生成、AI 跨境翻译）

#### `/controller/WxLoginController.java`
- `POST /api/v1/auth/wx_login` - 微信授权登录接口

### 2. 修改的文件

#### `/dto/ApiModels.java`
- 新增 `WxLoginRequest` 类（包含 code 和 userInfo）
- `WxUserInfo` - 用户信息（昵称、头像等）

#### `/resources/application-dev.yml`
- 新增微信小程序配置：
  ```yaml
  wx:
    miniapp:
      app-id: ${WX_MINIAPP_APPID:your_appid_here}
      secret: ${WX_MINIAPP_SECRET:your_secret_here}
  ```

### 3. 后端代码流程

```java
// 1. 接收小程序 code
POST /api/v1/auth/wx_login { code, userInfo }

// 2. 调用微信接口获取 openId
GET https://api.weixin.qq.com/sns/jscode2session

// 3. 通过 openId 查找用户
// - 存在：直接返回 token
// - 不存在：创建新用户

// 4. 创建新用户流程
// - 创建租户（level: free, type: individual）
// - 创建用户账户（mobile: wx_{openId}, role: boss）
// - 创建租户用户关联
// - 创建账户钱包（默认 1000 Tokens）
// - 启用默认插件（ai_image_gen, ai_script_gen, ai_translate）

// 5. 生成 token 并返回
LoginResponse { accessToken, expiresIn, userId, tenantId, roleCode }
```

---

## 三、配置说明

### 小程序端配置

#### 1. 微信小程序 AppID

编辑 `project.config.json`：
```json
{
  "appid": "your_actual_appid_here"
}
```

#### 2. 后端 API 地址

编辑 `src/utils/config.ts`：
```typescript
export const CONFIG = {
  API_BASE_URL: 'http://localhost:8080' // 或你的生产环境地址
};
```

### 后端配置

#### 1. 微信小程序 AppID 和 Secret

编辑 `application-dev.yml` 或通过环境变量：

```yaml
wx:
  miniapp:
    app-id: ${WX_MINIAPP_APPID:your_appid_here}
    secret: ${WX_MINIAPP_SECRET:your_secret_here}
```

或设置环境变量：
```bash
export WX_MINIAPP_APPID=wx1234567890abcdef
export WX_MINIAPP_SECRET=abcdef1234567890
```

#### 2. 获取微信小程序 AppID 和 Secret

1. 登录微信公众平台
2. 进入「开发」->「开发管理」
3. 找到你的小程序，复制 AppID
4. 进入「开发」->「开发设置」
5. 生成 AppSecret（仅显示一次，请妥善保管）

---

## 四、新用户默认配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 租户级别 | free | 免费版 |
| 租户类型 | individual | 个人 |
| 默认角色 | boss | 老板 |
| Token 余额 | 1000 | 默认免费配额 |
| 启用插件 | ai_image_gen, ai_script_gen, ai_translate | 三个 AI 插件 |
| 状态 | active | 活跃 |

---

## 五、API 接口定义

### POST /api/v1/auth/wx_login

微信授权登录接口

#### 请求
```json
{
  "code": "071abc123def456gh7",
  "user_info": {
    "nick_name": "张三",
    "avatar_url": "https://...",
    "gender": 1,
    "language": "zh_CN",
    "city": "杭州",
    "province": "浙江",
    "country": "中国"
  }
}
```

#### 响应
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "token-123",
    "expiresIn": 7200,
    "userId": 1001,
    "tenantId": 2001,
    "roleCode": "boss"
  },
  "requestId": "req-wx-login-xxx"
}
```

#### 错误响应
```json
{
  "code": 400,
  "message": "获取微信 openId 失败",
  "data": null,
  "requestId": "req-wx-login-xxx"
}
```

---

## 六、测试步骤

### 1. 启动后端

```bash
cd backend/java-spring
# 配置环境变量（可选）
export WX_MINIAPP_APPID=your_appid
export WX_MINIAPP_SECRET=your_secret

# 启动服务
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 2. 启动小程序

```bash
cd frontend/merchant-miniapp
npm install
npm run dev:weapp
```

### 3. 微信开发者工具测试

1. 打开微信开发者工具
2. 导入项目（选择 `dist` 目录）
3. 填写小程序 AppID
4. 点击「编译」
5. 点击「微信授权登录」按钮
6. 授权后自动登录并跳转到首页

---

## 七、常见问题

### Q1: 获取 openId 失败

**原因**: AppID 或 Secret 配置错误

**解决**:
1. 检查 `application-dev.yml` 中的配置
2. 确认 AppID 和 Secret 正确
3. 检查网络是否能访问微信接口

### Q2: 登录后显示"您没有权限使用此功能"

**原因**: 角色权限检查

**解决**: 新用户默认角色为 `boss`，应拥有所有权限。检查代码中的角色判断逻辑。

### Q3: 小程序端报错 "请求失败"

**原因**:
- 后端服务未启动
- API 地址配置错误
- 微信开发者工具未开启「不校验合法域名」

**解决**:
1. 确认后端服务已启动
2. 检查 `src/utils/config.ts` 中的 API 地址
3. 在微信开发者工具中勾选「不校验合法域名...」

### Q4: Token 余额显示为 0

**原因**: 账户钱包创建失败

**解决**: 检查后端日志，确认 `AccountWallet` 创建成功。

---

## 八、后续优化建议

### 1. 绑定手机号
- 微信授权登录后，引导用户绑定手机号
- 使用 `open-type="getPhoneNumber"` 获取手机号
- 更新 `UserAccount.mobile` 字段

### 2. 用户信息更新
- 首次登录后，可以引导用户完善昵称、头像
- 提供个人中心编辑功能

### 3. 登录态管理
- 使用微信 `checkSession()` 检查登录态
- Token 过期后自动重新登录

### 4. 安全加固
- 添加签名验证
- 限制登录频率
- 添加设备指纹

---

## 九、文件清单

### 小程序端（frontend/merchant-miniapp）
- ✏️ `src/pages/login/index.tsx` - 登录页面
- ✏️ `src/pages/login/index.scss` - 登录样式
- ✏️ `src/services/auth.ts` - API 服务

### 后端（backend/java-spring）
- ✨ `dto/WxLoginDtos.java` - 微信登录 DTO
- ✨ `service/WxLoginService.java` - 微信登录服务接口
- ✨ `service/impl/WxLoginServiceImpl.java` - 微信登录服务实现
- ✨ `controller/WxLoginController.java` - 微信登录控制器
- ✏️ `dto/ApiModels.java` - 新增 WxLoginRequest
- ✏️ `resources/application-dev.yml` - 新增微信配置

---

## 十、参考文档

- [微信小程序登录流程](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html)
- [code2Session 接口](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-info/code2Session.html)
- [Taro 登录 API](https://docs.taro.zone/docs/apis/login/wx-login)
