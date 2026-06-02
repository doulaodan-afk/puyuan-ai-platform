# 微信授权登录实现总结

## 修改的文件清单

### 小程序端（frontend/merchant-miniapp）

| 文件路径 | 修改内容 |
|---------|---------|
| `src/pages/login/index.tsx` | 移除手机号登录，实现微信授权登录 |
| `src/pages/login/index.scss` | 更新为微信授权登录样式 |
| `src/services/auth.ts` | 新增 WxLoginRequest 接口和 wxLogin() 方法 |
| `README.md` | 添加微信登录配置说明 |

### 后端（backend/java-spring）

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `dto/ApiModels.java` | 修改 | 新增 WxLoginRequest 和 WxUserInfo |
| `dto/WxLoginDtos.java` | 新增 | JsCode2SessionResponse DTO |
| `service/WxLoginService.java` | 新增 | 微信登录服务接口 |
| `service/impl/WxLoginServiceImpl.java` | 新增 | 微信登录服务实现 |
| `controller/WxLoginController.java` | 新增 | 微信登录控制器 |
| `resources/application-dev.yml` | 修改 | 新增微信小程序配置 |

### 文档

| 文件路径 | 说明 |
|---------|------|
| `frontend/merchant-miniapp/WX_LOGIN_GUIDE.md` | 微信登录详细配置和测试指南 |

---

## API 接口

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

---

## 配置说明

### 1. 小程序端配置

**project.config.json**
```json
{
  "appid": "your_actual_appid_here"
}
```

**src/utils/config.ts**
```typescript
export const CONFIG = {
  API_BASE_URL: 'http://localhost:8080'
};
```

### 2. 后端配置

**application-dev.yml**
```yaml
wx:
  miniapp:
    app-id: ${WX_MINIAPP_APPID:your_appid_here}
    secret: ${WX_MINIAPP_SECRET:your_secret_here}
```

或使用环境变量：
```bash
export WX_MINIAPP_APPID=wx1234567890abcdef
export WX_MINIAPP_SECRET=abcdef1234567890
```

---

## 新用户创建流程

```
1. 小程序调用 wx.login() 获取 code
2. 调用后端 /api/v1/auth/wx_login
3. 后端调用微信 jscode2session 获取 openId
4. 通过 openId 查找用户
5. 如果用户不存在：
   - 创建租户（free + individual）
   - 创建用户账户（mobile: wx_{openId}, role: boss）
   - 创建租户用户关联
   - 创建账户钱包（1000 Tokens）
   - 启用默认插件（ai_image_gen, ai_script_gen, ai_translate）
6. 生成 accessToken
7. 返回登录响应
```

---

## 测试步骤

### 1. 配置微信小程序

1. 登录微信公众平台
2. 获取 AppID 和 AppSecret
3. 配置到后端 `application-dev.yml`

### 2. 启动后端

```bash
cd backend/java-spring
export WX_MINIAPP_APPID=your_appid
export WX_MINIAPP_SECRET=your_secret
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 3. 启动小程序

```bash
cd frontend/merchant-miniapp
npm run dev:weapp
```

### 4. 微信开发者工具测试

1. 打开微信开发者工具
2. 导入项目（选择 `dist` 目录）
3. 填写小程序 AppID
4. 点击「微信授权登录」按钮
5. 授权后自动登录并跳转到首页

---

## 常见问题

### Q1: 获取 openId 失败

**原因**: AppID 或 Secret 配置错误

**解决**:
- 检查后端配置文件
- 确认 AppID 和 Secret 正确
- 检查网络是否能访问微信接口

### Q2: 小程序端报错 "请求失败"

**原因**:
- 后端服务未启动
- API 地址配置错误
- 微信开发者工具未开启「不校验合法域名」

**解决**:
- 确认后端服务已启动
- 检查 API 地址配置
- 在微信开发者工具中勾选「不校验合法域名...」

### Q3: Token 余额显示为 0

**原因**: 账户钱包创建失败

**解决**: 检查后端日志，确认 AccountWallet 创建成功

---

## 后续优化建议

1. **绑定手机号** - 使用 `open-type="getPhoneNumber"` 获取手机号
2. **用户信息更新** - 提供个人中心编辑功能
3. **登录态管理** - 使用 `checkSession()` 检查登录态
4. **安全加固** - 添加签名验证、限制登录频率

---

## 参考文档

- [微信小程序登录流程](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html)
- [code2Session 接口](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-info/code2Session.html)
- [Taro 登录 API](https://docs.taro.zone/docs/apis/login/wx-login)
