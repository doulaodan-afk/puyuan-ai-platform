# 「濮院毛衫」产业AI赋能平台：首批库表设计与API清单（可开工版）

**版本**：V1.0  
**日期**：2026-05-18  
**数据库建议**：MySQL 8.x（业务）+ Redis（缓存与幂等）+ OSS（对象存储）

---

## 1. 库表设计（MVP最小集合）

## 1.1 租户与用户域

### `tenant`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 租户主键 |
| tenant_code | varchar(64) | UNIQUE | 租户编码 |
| name | varchar(128) | NOT NULL | 租户名称 |
| status | tinyint | NOT NULL | 1启用 0冻结 |
| level | varchar(32) |  | 套餐等级 |
| created_at | datetime | NOT NULL | 创建时间 |
| updated_at | datetime | NOT NULL | 更新时间 |

### `user`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 用户主键 |
| tenant_id | bigint | INDEX | 所属租户 |
| mobile | varchar(32) | INDEX | 手机号 |
| nickname | varchar(64) |  | 昵称 |
| role_code | varchar(32) | INDEX | 角色编码 |
| status | tinyint |  | 1启用 0禁用 |
| created_at | datetime |  | 创建时间 |

## 1.2 插件域

### `plugin`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| plugin_id | varchar(64) | UNIQUE | 插件唯一ID |
| name | varchar(128) | NOT NULL | 名称 |
| version | varchar(32) | NOT NULL | 版本 |
| backend_api | varchar(255) | NOT NULL | 后端接口地址 |
| frontend_entry | varchar(255) |  | 前端入口 |
| billing_type | varchar(16) | NOT NULL | token / times / free |
| default_token_cost | int |  | 预估Token |
| status | tinyint | INDEX | 1上架 0下架 |
| review_status | varchar(16) | INDEX | pending/pass/reject |
| created_at | datetime |  | 创建时间 |

### `tenant_plugin`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| tenant_id | bigint | UK(tenant_id,plugin_id) | 租户 |
| plugin_id | varchar(64) | UK(tenant_id,plugin_id) | 插件ID |
| enabled | tinyint | INDEX | 1启用 0禁用 |
| config_json | json |  | 租户插件配置 |
| created_at | datetime |  | 创建时间 |

## 1.3 计费与账务域（关键）

### `account_wallet`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| tenant_id | bigint | UNIQUE | 租户 |
| token_balance | bigint | NOT NULL | Token余额 |
| cash_balance | decimal(18,2) | NOT NULL | 现金余额 |
| frozen_token | bigint | NOT NULL | 冻结Token |
| status | tinyint |  | 1正常 0冻结 |
| updated_at | datetime |  | 更新时间 |

### `billing_ledger`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| tenant_id | bigint | INDEX | 租户 |
| biz_no | varchar(64) | UNIQUE | 业务单号 |
| request_id | varchar(64) | INDEX | 调用请求ID |
| entry_type | varchar(32) | INDEX | recharge/debit/refund/adjust |
| direction | varchar(8) |  | in/out |
| token_amount | bigint |  | Token变动 |
| cash_amount | decimal(18,2) |  | 金额变动 |
| balance_after | bigint |  | 变动后Token余额 |
| plugin_id | varchar(64) | INDEX | 来源插件 |
| status | varchar(16) | INDEX | init/success/failed/reversed |
| occurred_at | datetime | INDEX | 发生时间 |
| created_at | datetime |  | 创建时间 |

### `idempotency_record`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| idempotency_key | varchar(128) | UNIQUE | 幂等键 |
| scope | varchar(64) | INDEX | 业务域 |
| request_hash | varchar(64) |  | 请求摘要 |
| response_body | json |  | 首次响应快照 |
| status | varchar(16) | INDEX | processing/success/failed |
| expire_at | datetime | INDEX | 过期时间 |
| created_at | datetime |  | 创建时间 |

### `recharge_order`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| order_no | varchar(64) | UNIQUE | 充值单号 |
| tenant_id | bigint | INDEX | 租户 |
| amount | decimal(18,2) | NOT NULL | 充值金额 |
| token_grant | bigint | NOT NULL | 到账Token |
| pay_channel | varchar(32) |  | 渠道 |
| pay_status | varchar(16) | INDEX | created/paid/failed/refunded |
| paid_at | datetime |  | 支付时间 |
| created_at | datetime |  | 创建时间 |

### `billing_statement_daily`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| tenant_id | bigint | UK(tenant_id,stat_date) | 租户 |
| stat_date | date | UK(tenant_id,stat_date) | 统计日 |
| token_in | bigint |  | 当日入账Token |
| token_out | bigint |  | 当日扣费Token |
| call_count | int |  | 调用次数 |
| amount_recharge | decimal(18,2) |  | 当日充值 |
| amount_refund | decimal(18,2) |  | 当日退款 |
| generated_at | datetime |  | 生成时间 |

## 1.4 调用与审计域

### `plugin_invoke_log`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| request_id | varchar(64) | UNIQUE | 请求ID |
| tenant_id | bigint | INDEX | 租户 |
| plugin_id | varchar(64) | INDEX | 插件 |
| model_vendor | varchar(32) |  | 模型厂商 |
| token_used | int |  | 实际消耗 |
| latency_ms | int |  | 耗时 |
| result_code | int | INDEX | 业务码 |
| risk_level | varchar(16) |  | 安全等级 |
| created_at | datetime | INDEX | 创建时间 |

### `audit_log`
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint | PK | 主键 |
| tenant_id | bigint | INDEX | 租户 |
| operator_id | bigint | INDEX | 操作人 |
| action | varchar(64) | INDEX | 操作类型 |
| target_type | varchar(32) |  | 资源类型 |
| target_id | varchar(64) |  | 资源ID |
| detail_json | json |  | 变更明细 |
| created_at | datetime | INDEX | 创建时间 |

---

## 2. 索引与一致性规则

- 强制唯一：`tenant_code`、`plugin.plugin_id`、`recharge_order.order_no`、`billing_ledger.biz_no`。
- 幂等主键：`idempotency_key` 全局唯一，至少保留24小时。
- 计费一致性：账本写入与余额更新采用同事务；失败时回滚。
- 对账机制：按 `tenant_id + stat_date` 生成日账单，支持重跑。

---

## 3. API清单（MVP）

## 3.1 认证与租户

| 方法 | 路径 | 描述 | 鉴权 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 登录（手机号/验证码） | 否 |
| POST | `/api/v1/auth/logout` | 登出 | 是 |
| GET | `/api/v1/tenant/profile` | 当前租户信息 | 是 |

## 3.2 账户与充值

| 方法 | 路径 | 描述 | 关键要求 |
|---|---|---|---|
| GET | `/api/v1/account/balance` | 查询余额 | 返回token与存储 |
| GET | `/api/v1/account/ledger` | 消费明细 | 支持分页+时间筛选 |
| POST | `/api/v1/account/recharge/orders` | 创建充值单 | 支持幂等 |
| POST | `/api/v1/account/recharge/orders/{order_no}/confirm` | 支付确认 | 防重复入账 |

## 3.3 插件市场与调用

| 方法 | 路径 | 描述 | 关键要求 |
|---|---|---|---|
| GET | `/api/v1/plugins` | 查询可用插件列表 | 按租户可见范围过滤 |
| POST | `/api/v1/plugins/{plugin_id}/enable` | 启用插件 | 写审计日志 |
| POST | `/api/v1/plugins/{plugin_id}/disable` | 禁用插件 | 写审计日志 |
| POST | `/api/v1/plugins/{plugin_id}/invoke` | 调用插件 | 扣费、幂等、审计 |

## 3.4 账单与对账

| 方法 | 路径 | 描述 | 关键要求 |
|---|---|---|---|
| GET | `/api/v1/billing/statements/daily` | 日账单查询 | T+1可查 |
| GET | `/api/v1/billing/statements/monthly` | 月账单查询 | 支持导出 |
| POST | `/api/v1/billing/reconcile/replay` | 对账重跑 | 管理员权限 |

## 3.5 管理后台

| 方法 | 路径 | 描述 | 权限 |
|---|---|---|---|
| POST | `/api/v1/admin/plugins` | 上架插件 | 超管/运营 |
| PATCH | `/api/v1/admin/plugins/{plugin_id}` | 更新插件 | 超管/运营 |
| POST | `/api/v1/admin/plugins/{plugin_id}/publish` | 灰度或全量发布 | 超管 |
| GET | `/api/v1/admin/tenants` | 租户列表 | 超管/运营 |
| POST | `/api/v1/admin/tenants/{tenant_id}/freeze` | 冻结租户 | 超管 |
| GET | `/api/v1/admin/billing/dashboard` | 平台计费看板 | 超管/财务 |

---

## 4. 接口约定（必须统一）

- 统一Header：`X-Tenant-Id`、`X-Request-Id`、`Idempotency-Key`。
- 统一响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "request_id": "req_xxx"
}
```
- 统一错误码：`4xxxx` 业务错误，`5xxxx` 系统错误。
- 可追踪性：所有写操作必须落审计日志与业务日志。

---

## 5. 第一版DDL优先顺序（建议）

1. `tenant`、`user`、`plugin`、`tenant_plugin`  
2. `account_wallet`、`billing_ledger`、`idempotency_record`  
3. `recharge_order`、`billing_statement_daily`  
4. `plugin_invoke_log`、`audit_log`  

以上顺序可保障先跑通“登录-调用-扣费-查账”主路径，再补报表与审计能力。  
