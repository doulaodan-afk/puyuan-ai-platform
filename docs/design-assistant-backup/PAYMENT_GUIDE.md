# 微信支付充值功能实现指南

## 一、功能概述

实现微信小程序支付充值 Token 功能，包括：

1. 后端创建微信支付预下单接口
2. 小程序端集成微信支付
3. 支付回调处理和 Token 到账
4. 开发环境 Mock 支付模式

---

## 二、后端实现

### 1. 新增文件

| 文件 | 说明 |
|------|------|
| `dto/WxPaymentDtos.java` | 微信支付相关 DTO |
| `service/WxPaymentService.java` | 微信支付服务接口 |
| `service/impl/WxPaymentServiceImpl.java` | 微信支付服务实现 |
| `controller/WxPaymentController.java` | 微信支付控制器 |

### 2. 修改的文件

| 文件 | 修改内容 |
|------|---------|
| `resources/application-dev.yml` | 新增微信支付配置 |

### 3. 新增接口

#### POST /api/v1/payment/wx/prepay

创建微信支付预下单

**请求**:
```json
{
  "amount": 99,
  "packageName": "专业包"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "appId": "wx1234567890",
    "timeStamp": "1715000000",
    "nonceStr": "abc123",
    "package": "prepay_id=wx1234567890",
    "signType": "RSA",
    "paySign": "signature",
    "packageId": "wx1234567890"
  }
}
```

#### POST /api/v1/payment/wx/notify

微信支付回调通知

**请求**: 微信支付平台发送加密数据

**响应**:
```json
{
  "code": "SUCCESS",
  "message": "success"
}
```

#### POST /api/v1/payment/wx/mock/success

Mock 支付成功接口（开发环境使用）

**请求**:
```json
{
  "orderNo": "RC2026052000001"
}
```

#### GET /api/v1/payment/wx/order/{orderNo}/status

查询订单状态

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": "SUCCESS"
}
```

### 4. 配置说明

#### application-dev.yml

```yaml
wx:
  miniapp:
    app-id: ${WX_MINIAPP_APPID:your_appid_here}
    secret: ${WX_MINIAPP_SECRET:your_secret_here}
  payment:
    mchid: ${WX_PAYMENT_MCHID:your_mchid_here}
    api-v3-key: ${WX_PAYMENT_API_V3_KEY:your_api_v3_key_here}
    notify-url: ${WX_PAYMENT_NOTIFY_URL:http://your-domain.com/api/v1/payment/wx/notify}
    mock-enabled: ${WX_PAYMENT_MOCK_ENABLED:true}
```

#### 环境变量

```bash
# 微信小程序
export WX_MINIAPP_APPID=wx1234567890abcdef
export WX_MINIAPP_SECRET=abcdef1234567890

# 微信支付
export WX_PAYMENT_MCHID=1234567890
export WX_PAYMENT_API_V3_KEY=your_api_v3_key_here
export WX_PAYMENT_NOTIFY_URL=https://api.puyuan-ai.com/api/v1/payment/wx/notify
export WX_PAYMENT_MOCK_ENABLED=false  # 生产环境必须为 false
```

### 5. 支付流程

```
1. 小程序端选择充值套餐
2. 调用 /api/v1/payment/wx/prepay 创建预支付
3. 后端创建充值订单，调用微信统一下单接口
4. 后端返回小程序支付参数
5. 小程序调用 wx.requestPayment 发起支付
6. 用户完成支付
7. 微信支付平台调用 /api/v1/payment/wx/notify 回调接口
8. 后端验证签名，更新订单状态
9. 后端增加用户 Token 余额
```

---

## 三、小程序端实现

### 1. 修改的文件

| 文件 | 修改内容 |
|------|---------|
| `services/account.ts` | 新增微信支付相关接口和类型 |
| `pages/recharge/index.tsx` | 集成微信支付功能 |
| `pages/recharge/index.scss` | 更新样式 |

### 2. 新增接口

```typescript
// 微信支付预下单
async wxPrepay(data: WxPrepayRequest): Promise<WxPrepayResponse>

// Mock 支付成功
async mockPaymentSuccess(orderNo: string): Promise<void>

// 查询订单状态
async queryOrderStatus(orderNo: string): Promise<{ status: string }>
```

### 3. 支付流程

#### 开发环境（Mock 模式）

```typescript
// 1. 用户选择套餐
handleSelectPackage(pkg)

// 2. 点击充值
handleRecharge()
  ├─ 创建充值订单
  ├─ 调用 mockPaymentSuccess()
  ├─ 刷新余额
  └─ 显示充值成功弹窗
```

#### 生产环境（真实支付）

```typescript
// 1. 用户选择套餐
handleSelectPackage(pkg)

// 2. 点击充值
handleRecharge()
  ├─ 调用 wxPrepay() 获取支付参数
  ├─ 调用 wx.requestPayment() 发起支付
  ├─ 支付成功后跳转到订单列表
  └─ 支付失败显示错误提示
```

---

## 四、测试步骤

### 1. 开发环境测试（Mock 模式）

```bash
# 1. 确保环境变量配置
export WX_PAYMENT_MOCK_ENABLED=true

# 2. 启动后端
cd backend/java-spring
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 3. 启动小程序
cd frontend/merchant-miniapp
npm run dev:weapp

# 4. 在微信开发者工具中测试
# - 进入充值页面
# - 选择充值套餐
# - 点击立即充值
# - 应该看到"充值成功"提示
# - Token 余额应该增加
```

### 2. 生产环境测试（真实支付）

```bash
# 1. 确保环境变量配置
export WX_PAYMENT_MOCK_ENABLED=false
export WX_PAYMENT_MCHID=your_mchid
export WX_PAYMENT_API_V3_KEY=your_api_v3_key
export WX_PAYMENT_NOTIFY_URL=https://api.puyuan-ai.com/api/v1/payment/wx/notify

# 2. 启动后端（生产环境）
java -jar platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 3. 构建小程序生产版本
cd frontend/merchant-miniapp
npm run build:weapp

# 4. 上传小程序代码到微信后台
# - 预览版本测试支付流程
# - 确认支付回调正常
# - 正式发布
```

### 3. 支付回调测试

使用微信支付沙箱环境进行测试：

1. 登录微信支付商户平台
2. 进入沙箱环境
3. 查看沙箱日志，确认回调地址正确
4. 模拟支付成功回调
5. 检查后端日志，确认订单状态更新

---

## 五、常见问题

### Q1: Mock 模式下充值失败

**检查项**:
- [ ] 后端 `WX_PAYMENT_MOCK_ENABLED=true`
- [ ] 充值订单创建成功
- [ ] Mock 回调接口调用成功
- [ ] Token 余额增加

**解决**: 检查后端日志，确认每一步都成功执行。

### Q2: 真实支付时发起失败

**检查项**:
- [ ] 小程序 AppID 正确
- [ ] 商户号正确
- [ ] API v3 密钥正确
- [ ] 回调地址可访问（需公网）

**解决**: 
1. 检查微信商户平台配置
2. 确认回调地址已在商户平台配置
3. 使用微信支付沙箱环境测试

### Q3: 支付回调未触发

**检查项**:
- [ ] 回调地址配置正确
- [ ] 服务器防火墙允许微信访问
- [ ] HTTPS 证书有效
- [ ] Nginx 正确转发请求

**解决**:
1. 在商户平台查看回调日志
2. 检查服务器 Nginx 日志
3. 确认 API 路由配置正确

### Q4: Token 余额未增加

**检查项**:
- [ ] 支付回调成功执行
- [ ] 订单状态更新为 SUCCESS
- [ ] 钱包更新成功
- [ ] 事务未回滚

**解决**: 检查后端日志，确认回调处理逻辑正确。

---

## 六、安全注意事项

### 1. 支付安全

- [ ] 必须验证微信签名
- [ ] 支付金额必须与订单金额一致
- [ ] 订单号必须唯一
- [ ] 回调接口必须幂等处理
- [ ] 生产环境禁用 Mock 模式

### 2. 数据安全

- [ ] API 密钥安全存储
- [ ] 支付金额后端计算
- [ ] 订单状态只能单向流转
- [ ] 敏感数据不记录日志

### 3. 接口安全

- [ ] 支付接口需要认证
- [ ] 预下单接口限流保护
- [ ] 回调接口添加签名验证
- [ ] 订单查询接口权限控制

---

## 七、后续优化建议

1. **退款功能**
   - 实现微信退款接口
   - 支持部分退款和全额退款
   - 扣除相应 Token

2. **对账功能**
   - 每日自动对账
   - 异常订单报警
   - 对账报表导出

3. **优惠券功能**
   - 支持充值优惠
   - 首充优惠
   - 活动优惠码

4. **支付失败补偿**
   - 定时查询未支付订单
   - 自动超时关闭订单
   - 异常订单人工处理

---

## 八、文件清单

### 后端新增/修改文件

| 文件路径 | 操作 |
|----------|------|
| `dto/WxPaymentDtos.java` | 新增 |
| `service/WxPaymentService.java` | 新增 |
| `service/impl/WxPaymentServiceImpl.java` | 新增 |
| `controller/WxPaymentController.java` | 新增 |
| `resources/application-dev.yml` | 修改 |

### 小程序端新增/修改文件

| 文件路径 | 操作 |
|----------|------|
| `services/account.ts` | 修改（新增支付接口） |
| `pages/recharge/index.tsx` | 修改（集成微信支付） |
| `pages/recharge/index.scss` | 修改（更新样式） |

---

## 九、参考文档

- [微信支付 API 文档](https://pay.weixin.qq.com/wiki/doc/apiv3/index.shtml)
- [微信小程序支付接入](https://developers.weixin.qq.com/miniprogram/dev/platform/capability/payment)
- [Taro 支付 API](https://docs.taro.zone/docs/apis/payment/wx-requestPayment)
