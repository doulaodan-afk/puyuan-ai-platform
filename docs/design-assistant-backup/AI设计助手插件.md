# 「濮院毛衫」产业AI赋能平台 — AI设计助手插件 完整PRD（V2.0）

**文档版本**：2.0
**日期**：2026年5月
**状态**：可交付开发

---

## 1. 项目背景与产品定位

### 1.1 项目回顾

本项目已完成了「濮院毛衫」产业AI赋能平台的基础设施建设：
- **后端**：Spring Boot 3.3 + MyBatis Plus，提供认证、租户管理、插件管理、计费配置、充值订单、账单流水、审计日志、基于角色的权限控制。
- **前端**：管理端（5174端口）和租户端（5173端口），基于Vue3 + TypeScript + Vite + Pinia。
- **部署**：GitHub仓库已建立，后端运行于8080端口，前后端均可独立启动。

本次开发的目标是在现有平台上新增 **「AI设计助手」插件**。

### 1.2 产品定位

「AI设计助手」是一个面向服装设计协作场景的智能插件，负责：
- 设计师通过**多模态输入**（图片、视频、文本、语音）创建设计需求
- 通过**AI对话**帮助设计师完善需求并自动生成结构化总结
- 支持设计师**确认**需求（直接发布任务）或**转给设计助理**（租户专属助理复核）
- 设计助理在完成复核与编辑后，将需求拆解为**面料任务**和**打版任务**
- 面料商接收面料任务，可上传面料库、更新物流信息
- 板师（版师服务商）接收打版任务，但**打版任务开工条件 = 关联的面料任务已完成**

### 1.3 商业模式适配

本插件依托平台现有的**Token预充值计费体系**。AI对话、需求解析等AI能力调用均消耗Token，从租户余额中扣除。面料商和版师服务商无需消耗Token，但需要通过平台认证。

**关于Token定价的提示**：建议参考主流AI服务商定价水平，可预充值采购第三方API Token（如阿里通义千问、智谱等），向租户零售时设置合理毛利。平台计费系统建议初期使用自研方案（Redis计数+MySQL落库），租户规模超过1000家后切换至Lago开源计费系统。

### 1.4 本PRD的核心产出

本PRD覆盖以下内容：
- 角色体系规划
- 数据库设计（DDL）
- 后端API设计
- 前端页面规划
- 开源技术组件选型
- 开发顺序与工作量估算
- 风险提示

**本PRD可直接交付给开发团队作为完整开发指令。** 所有开源组件均有明确的选型理由和集成方式。如开发过程中有任何疑问，可参考各开源项目的官方文档。

---

## 2. 角色体系规划

### 2.1 现有角色

平台已具备以下角色：
| 角色 | 说明 |
|------|------|
| **管理员** | 管理租户、插件、计费配置、查看全局数据 |

### 2.2 需要扩展的角色

以下角色为本次开发新增：

**平台管理层**
| 角色 | 说明 | 归属 |
|------|------|------|
| **平台管理员** | 分配规则配置、供应商/版师审核、数据总览 | 平台方 |

**商家租户层（一个商家租户为一个法律主体）**
| 角色 | 说明 | 归属 |
|------|------|------|
| **老板** | 租户主账号，查看团队内所有需求统计、消耗Token情况 | 租户内 |
| **设计师** | 创建设计需求、跟进任务进度 | 租户内 |
| **内部版师** | 接收打版任务、上传纸样文件（可选，若企业有内部版师） | 租户内 |
| **设计助理** | 接收租户内部转派的需求，复核并编辑子任务、发布任务 | 租户内 |

**外部服务商层（独立租户）**
| 角色 | 说明 | 归属 |
|------|------|------|
| **面料商** | 接收面料任务、管理面料库、更新物流信息 | 独立租户（tenant_type='supplier'） |
| **版师服务商** | 接收打版任务、上传纸样文件 | 独立租户（tenant_type='pattern_service'） |

### 2.3 租户绑定专属助理的设计

根据需求确认，设计助理采用「**每个租户绑定自己的专属助理**」模式，而非平台公共助理池。这意味着：

- 每个商家租户可在后台设置一名或多名专属设计助理（从本租户员工中选择）
- 需求自动推送到这些专属助理的个人待办列表，其他租户的助理不可见
- 助理的认证走本租户的登录体系（可复用现有 `user.tenant_id` + `tenant_role` 机制）

### 2.4 角色架构图

```
平台管理员（管理端）
    └── 全局配置（分配规则、供应商/版师审核、插件管理）

商家租户A
    ├── 老板（主账号，查看统计、邀请/设置角色成员）
    ├── 设计师（创建需求）
    ├── 设计助理1（复核&发布）
    ├── 设计助理2（复核&发布）
    └── 内部版师（可选）

商家租户B
    └── （同上结构）

外部服务商
    ├── 面料商X（独立租户，为多个商家服务）
    ├── 面料商Y（独立租户）
    ├── 版师服务商M（独立租户）
    └── 版师服务商N（独立租户）
```

### 2.5 权限控制说明

现有平台的基于Spring Security的角色体系可自然扩展：
- 在现有的 `ROLE_ADMIN`、`ROLE_TENANT` 之外，新增 `ROLE_DESIGNER`、`ROLE_DESIGN_ASSISTANT`、`ROLE_BOSS`、`ROLE_PATTERN_MAKER`、`ROLE_SUPPLIER`、`ROLE_PATTERN_SERVICE`
- `tenant_role` 字段用于区分租户内具体角色，便于前端动态渲染菜单
- 供应商租户的 `tenant_type='supplier'`，可关联多个 `user`（如不同联系人），共用 `ROLE_SUPPLIER` 权限


## 3. 完整业务流程

### 3.1 流程总览

```
[设计师] 创建需求
    └── 多模态输入（图片/视频/文本/实时语音识别）
        └── 与AI实时对话，完善需求
            └── AI生成结构化总结
                ├── 设计师确认 → 系统自动发布任务（面料任务→面料商；打版任务→版师）
                └── 设计师不认可 → 转给本租户专属设计助理
                    └── 设计助理打开待处理请求
                        └── 系统自动预拆分为面料+打版任务（基于AI总结）
                            └── 助理复核/修改子任务（新增/删除/修改分配对象/修改规格）
                                └── 助理点击"发布" → 子任务推送给面料商和版师
                                    ├── 面料商接收 → 确认/拒绝/完成 → 更新物流信息
                                    └── 版师接收 → 等待面料完成 → 确认/拒绝/完成
```

### 3.2 详细环节说明

**环节1：设计师创建需求**
- 支持上传多张图片、多个视频
- 支持文本描述（必填，核心输入源）
- 支持实时语音识别：使用浏览器原生 `SpeechRecognition` API，用户点击麦克风按钮后实时转录文字，填充到对话输入框中。同时支持上传音频文件（M4A/MP3等）作为留证，保存至需求 `raw_audio_url` 字段。
- 对话界面：类ChatGPT风格，设计师与AI一问一答，AI逐步追问以完善需求细节（如面料类型、版型细节、数量预期等）。
- AI生成总结：用户可在任意时刻点击「生成总结」，AI将整个对话历史整合输出为结构化 JSON（包含面料规格、版型参数）。总结在右侧面板实时展示，设计师可手动编辑修正。

**环节2：确认 vs 转助理**
- 若设计师认可AI总结，点击「确认发布」，系统根据AI总结和分配规则创建子任务（状态为 `pending`），并直接推送给匹配到的面料商和版师。
- 若设计师不认可AI总结（如AI解析有误），点击「转给助理」，系统将该需求状态改为 `assistant_processing`，并自动根据AI总结预拆分子任务（状态为 `draft`），推送给本租户绑定的设计助理。

**环节3：设计助理复核**
- 助理在待处理列表中打开需求
- 看到系统预拆分的面料任务和打版任务，可：
  - **修改任何字段**：分配对象、截止时间、任务内容（面料规格/版型要求JSON）
  - **删除子任务**：如某个面料任务并非真实需求
  - **新增子任务**：如额外增加一种辅料需求或增加第二种面料
  - 子任务在助理保存前始终处于 `draft` 状态
- 助理确认无误后，点击「发布」，系统将所有子任务状态从 `draft` 改为 `pending`，并分别发送站内信给面料商和版师。

**环节4：面料商处理**
- 面料商在「我的任务」中看到分配的任务，可：
  - **接受** → 任务状态变为 `accepted`
  - **拒绝** → 任务状态变为 `rejected`（需填写原因，系统自动通知设计师/助理）
  - **发货**（仅接受后可操作）：提供线上发货（物流公司+运单号）或线下发货（填写备注，如「自提」）。发货后任务状态变为 `shipped`
  - **完成**（仅发货后可操作）：任务状态变为 `delivered` / `done`
- 物料上传：面料商可管理自己的「面料库」，上传面料图片/视频/规格参数，供设计师在创建需求时参考。

**环节5：板师处理（含开工条件约束）**
- 打版任务增加了一个关键业务约束：**板师只有在关联的面料任务完成后才能接受任务**。这是本次需求的核心——板师需要同时拿到打版需求和对应的面料信息（规格、到货预期等）才能开工。
- 板师在待办列表中看到任务时：
  - 若 `fabric_task_id` 对应的面料任务状态 ≠ `delivered/done`，「接受」按钮禁用，并显示「等待面料准备就绪」
  - 若面料任务已完成，按钮启用，可正常接受/拒绝/完成
- 在子任务详情页，板师可查看关联面料任务的物流进度、面料规格等，便于提前准备。

**环节6：防遗漏机制**
- 定时扫描：每日 09:00、14:00、18:00 执行，扫描所有 `pending` 且 `deadline < now()` 且 `notified_at` 超过 24 小时的任务，发送站内信催办。
- 催办记录表 `task_remind_log` 防止重复骚扰。


## 4. 技术架构与开源组件选型

### 4.1 整体技术栈（基于现有系统扩展）

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.3 + MyBatis Plus | 已有 |
| 服务网关 | Apache APISIX | 建议新增。支持动态路由、限流、鉴权，插件上架时可通过Admin API自动创建路由。推荐使用Docker部署，在插件化场景下灵活度高于Kong。 |
| 服务发现与配置 | Nacos | 建议新增。插件微服务动态注册与配置管理。 |
| 前端框架 | Vue3 + TypeScript + Vite + Pinia | 已有 |
| 微前端 | qiankun | 建议新增。用于插件独立加载，实现需求文档要求的「动态插件应用管理」。qiankun是基于single-spa的微前端实现库，支持多技术栈混合、沙箱隔离，经过阿里等企业大规模验证。 |
| 数据库 | MySQL 8.0 | 已有 |
| 缓存 | Redis | 已有（预留） |
| 对象存储 | 阿里云OSS/腾讯云COS | 已有 |
| 任务调度 | Spring @Scheduled | 轻量级定时任务，用于催办、物流状态更新 |
| 消息通知 | 自研Message表 + 可选邮件集成 | 站内信优先，邮件作为扩展 |
| 工作流状态机 | Spring状态机 或 自研 | 初期采用数据库状态字段+业务逻辑判断即可，无需引入Camunda等重型框架 |
| 开源计费系统 | Lago | 推荐（租户数>1000时切换）。开源事件驱动计费，支持按量计费、订阅+按量混合模式、预充值积分/钱包，Docker一键部署。 |


### 4.2 插件化架构

**为什么需要插件化？**
平台在V1.0已实现插件管理的基本能力（`plugin`表、`manifest.json`概念、Token计费），AI设计助手应作为独立插件接入，而非硬编码为核心模块。插件化带来的好处：
- 独立部署、独立迭代
- 可按需上架/下架（平台管理员可控制）
- 复用统一的鉴权、计费、租户隔离机制
- 后续其他AI工具（如AI商品图生成）可作为独立插件快速接入

**实现方式**：
- 前端：主应用（merchant-web）通过qiankun动态加载插件的独立前端入口（`frontend_entry`），插件与主应用通过`postMessage`通信。
- 后端：每个插件独立部署为微服务（或Spring Boot模块），通过Nacos注册，API网关根据`plugin_id`动态路由。插件后端沿用已有的Spring Boot标准结构。

### 4.3 物流API选型

推荐使用**快递鸟 API**，理由：
- 覆盖国内外1200+物流服务商，顺丰、中通、DHL等主流快递公司统一接口接入
- 提供即时查询和Webhook推送两种模式，支持物流轨迹实时同步
- 接入简单，支持Java等12种开发语言的SDK，与Spring Boot无缝集成
- 快递鸟官方文档提供MD5签名验证机制，可保障API调用安全性

**实施方式**：面料商发货时提交物流公司编码+运单号，系统调用快递鸟即时查询API获取首次轨迹；设置定时任务（每小时一次）查询在途单号的最新状态；Webhook回调可选。

### 4.4 AI对话与模型调用

AI能力是本次插件的核心引擎，涵盖：对话追问、需求总结生成、分配规则匹配等环节。

**AI模型选型建议**：
- 轻量级对话任务（追问、总结生成）：可使用阿里通义千问或智谱GLM系列。两者均提供稳定的API服务，支持高并发，且价格透明。建议平台预充值批量采购Token（如通义千问企业版批量价约￥0.6-0.8/千tokens），零售价设为￥1.2-1.5/千tokens，保留合理毛利。
- 多模态识别（图片/视频分析）：如有上传图片进行面料识别、版型分析的场景，可使用通义千问VL系列或多模态GPT-4o。

**AI Prompt设计示例**（用于需求总结生成）：
```
你是一个专业的服装设计专家。请根据以下对话记录，提取出完整的设计需求，包括面料要求和版型要求。
对话记录：{conversation_history}

请输出以下JSON格式，不要输出其他内容：
{
  "fabric": {
    "type": "面料类型",
    "weight": "克重",
    "color": "颜色",
    "special_requirements": "特殊要求"
  },
  "pattern": {
    "collar": "领型",
    "sleeve": "袖型",
    "waist": "腰型",
    "silhouette": "廓形",
    "other_details": "其他细节"
  }
}
```

**对话状态管理**：AI对话涉及多轮交互，需要在后端维护会话ID（session_id）。每个需求从创建到生成总结为一个会话周期。建议使用Redis存储对话上下文，会话过期时间设为1小时。


### 4.5 实时语音识别方案

设计师可以在创建需求时通过语音快速输入，极大提升效率。

- **浏览器原生方案**：使用 `Web Speech API`（`SpeechRecognition` / `webkitSpeechRecognition`），直接在Vue组件中调用。示例代码可参考Vue3多模态融合方案。
- **语言设置**：`recognition.lang = 'zh-CN'`，支持中文普通话识别
- **特点**：无需额外API费用，实时转文字，但准确率依赖用户环境和网络。建议设置 `interimResults = true`，实现边说边显示临时结果的效果。
- **兜底方案**：同时提供「上传录音文件」入口，用户可预先录制音频后上传，文件存储至OSS，作为 `raw_audio_url` 留证。


## 5. 数据库设计（DDL）

以下为新增表和字段的设计。所有表名采用 `underscore` 格式，与现有表保持一致。

### 5.1 扩展 `user` 表

```sql
-- 用户表扩展（已有，需新增字段）
ALTER TABLE `user` ADD COLUMN `tenant_role` VARCHAR(20) DEFAULT 'member' 
    COMMENT 'designer/boss/pattern_maker/design_assistant';
ALTER TABLE `user` ADD COLUMN `tenant_id` BIGINT COMMENT '所属租户ID（主账号租户）';
```

### 5.2 扩展 `tenant` 表

```sql
-- 租户表扩展
ALTER TABLE `tenant` ADD COLUMN `tenant_type` VARCHAR(20) DEFAULT 'normal' 
    COMMENT 'normal/supplier/pattern_service';
ALTER TABLE `tenant` ADD COLUMN `parent_tenant_id` BIGINT DEFAULT 0 COMMENT '父租户ID（预留扩展）';
```

### 5.3 租户绑定设计助理（多对多）

```sql
-- 租户-设计助理绑定表
CREATE TABLE `tenant_assistant` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '商家租户ID',
  `assistant_user_id` BIGINT NOT NULL COMMENT '设计助理 user.id',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (`tenant_id`)
);
```

### 5.4 设计需求主表

```sql
CREATE TABLE `design_requirement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
  `creator_id` BIGINT NOT NULL COMMENT '设计师 user.id',
  `title` VARCHAR(200) COMMENT '需求标题',
  -- 原始素材
  `raw_images` JSON COMMENT '图片URL数组',
  `raw_videos` JSON COMMENT '视频URL数组',
  `raw_audio_url` VARCHAR(500) COMMENT '语音留证文件URL',
  `raw_text` TEXT COMMENT '设计师手动输入的文本',
  `conversation_history` JSON COMMENT '与AI的对话记录（role, content, time）',
  `ai_summary` TEXT COMMENT 'AI最终生成的结构化总结',
  -- 状态与流转
  `designer_approved` TINYINT DEFAULT 0 COMMENT '0-未确认,1-确认发布,2-转助理',
  `assistant_id` BIGINT DEFAULT 0 COMMENT '指派的助理 user.id（当转助理时）',
  `status` VARCHAR(20) DEFAULT 'draft' 
      COMMENT 'draft/assistant_processing/released/completed/cancelled',
  `total_token_cost` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_creator_id (`creator_id`),
  INDEX idx_status (`status`)
);
```

### 5.5 子任务表（面料/打版）

```sql
CREATE TABLE `design_task` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `requirement_id` BIGINT NOT NULL COMMENT '关联需求ID',
  `task_type` VARCHAR(20) NOT NULL COMMENT 'fabric/pattern',
  `assignee_type` VARCHAR(20) NOT NULL COMMENT 'supplier/pattern_service',
  `assignee_id` BIGINT NOT NULL COMMENT '接收方的 tenant.id',
  `content` JSON COMMENT '任务详情JSON（面料规格/版型要求等）',
  `status` VARCHAR(20) DEFAULT 'pending' 
      COMMENT 'draft/pending/accepted/shipped/delivered/rejected/done/cancelled',
  `deadline` DATETIME COMMENT '截止时间',
  `result_url` VARCHAR(500) COMMENT '结果文件URL（报价单/纸样等）',
  `fabric_task_id` BIGINT DEFAULT 0 COMMENT '仅对pattern任务：关联的fabric任务ID',
  -- 物流相关（仅fabric任务）
  `logistics_company` VARCHAR(50),
  `logistics_tracking_no` VARCHAR(100),
  `logistics_status` VARCHAR(20) DEFAULT 'pending' 
      COMMENT 'pending/shipped/delivered',
  `offline_logistics_note` TEXT COMMENT '线下物流备注',
  `shipped_at` DATETIME,
  `delivered_at` DATETIME,
  -- 催办相关
  `notified_at` DATETIME COMMENT '上次催办时间',
  `completed_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_requirement_id (`requirement_id`),
  INDEX idx_assignee_id (`assignee_id`),
  INDEX idx_status (`status`)
);
```

### 5.6 面料库表（面料商上传）

```sql
CREATE TABLE `fabric_library` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `supplier_tenant_id` BIGINT NOT NULL COMMENT '面料商租户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '面料名称',
  `category` VARCHAR(50) COMMENT '品类（真丝/羊毛/棉麻）',
  `images` JSON COMMENT '图片URL数组',
  `video_url` VARCHAR(500) COMMENT '小样视频URL',
  `specs` JSON COMMENT '规格（克重、门幅、成分等）',
  `price_per_meter` DECIMAL(10,2) COMMENT '单价（元/米）',
  `stock_status` VARCHAR(20) DEFAULT 'in_stock' COMMENT 'in_stock/out_of_stock',
  `is_visible` TINYINT DEFAULT 1 COMMENT '是否在前端展示',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_supplier_tenant_id (`supplier_tenant_id`),
  INDEX idx_is_visible (`is_visible`)
);
```

### 5.7 分配规则表（管理员配置）

```sql
CREATE TABLE `task_assign_rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `rule_name` VARCHAR(100) NOT NULL,
  `keyword` VARCHAR(100) NOT NULL COMMENT '关键词（如“真丝”）',
  `target_tenant_id` BIGINT NOT NULL COMMENT '匹配的面料商/版师租户ID',
  `task_type` VARCHAR(20) NOT NULL COMMENT 'fabric/pattern',
  `priority` INT DEFAULT 0 COMMENT '优先级，越高越优先',
  `enabled` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_keyword (`keyword`),
  INDEX idx_task_type (`task_type`)
);
```

### 5.8 站内信表

```sql
CREATE TABLE `message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `receiver_id` BIGINT NOT NULL COMMENT '接收方 user.id',
  `sender_id` BIGINT DEFAULT 0 COMMENT '发送方 user.id（系统消息为0）',
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT,
  `type` VARCHAR(20) DEFAULT 'system' COMMENT 'system/task/remind',
  `is_read` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_receiver_id (`receiver_id`),
  INDEX idx_is_read (`is_read`)
);
```

### 5.9 催办记录表

```sql
CREATE TABLE `task_remind_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL,
  `remind_time` DATETIME NOT NULL,
  `remind_channel` VARCHAR(20) DEFAULT 'internal' COMMENT 'internal/email/sms',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task_id (`task_id`)
);
```


## 6. 后端API设计

所有API前缀建议为 `/api/plugins/ai_design_assistant`，统一通过API网关路由，便于后续插件化管理。API调用需携带 `X-Tenant-Id` 和租户的API密钥，复用现有平台的鉴权中间件。

### 6.1 需求管理（设计师端）

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/design/requirement/create` | 创建需求（含原始素材、对话历史） | `ROLE_DESIGNER` |
| POST | `/api/design/requirement/chat` | AI对话接口（需传session_id，非流式返回） | `ROLE_DESIGNER` |
| POST | `/api/design/requirement/summarize` | 生成AI总结（基于对话历史） | `ROLE_DESIGNER` |
| POST | `/api/design/requirement/confirm` | 设计师确认AI总结，直接发布任务 | `ROLE_DESIGNER` |
| POST | `/api/design/requirement/transfer` | 转给本租户专属设计助理 | `ROLE_DESIGNER` |
| GET | `/api/design/requirement/list` | 获取本租户需求列表（支持分页、状态筛选） | `ROLE_DESIGNER`, `ROLE_BOSS` |
| GET | `/api/design/requirement/detail/{id}` | 获取需求详情（含子任务） | `ROLE_DESIGNER`, `ROLE_BOSS` |

### 6.2 设计助理端

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/design/assistant/pending-list` | 助理待处理需求列表 | `ROLE_DESIGN_ASSISTANT` |
| GET | `/api/design/assistant/detail/{requirementId}` | 助理查看需求详情（含预拆分子任务） | `ROLE_DESIGN_ASSISTANT` |
| PUT | `/api/design/assistant/task/{taskId}` | 助理修改子任务（任何字段） | `ROLE_DESIGN_ASSISTANT` |
| POST | `/api/design/assistant/task` | 助理新增子任务 | `ROLE_DESIGN_ASSISTANT` |
| DELETE | `/api/design/assistant/task/{taskId}` | 助理删除子任务 | `ROLE_DESIGN_ASSISTANT` |
| POST | `/api/design/assistant/publish/{requirementId}` | 助理发布所有子任务 | `ROLE_DESIGN_ASSISTANT` |

### 6.3 任务处理端（面料商/版师）

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/design/task/my-tasks` | 获取分配给当前登录用户的任务 | `ROLE_SUPPLIER`, `ROLE_PATTERN_SERVICE`, `ROLE_PATTERN_MAKER` |
| GET | `/api/design/task/detail/{taskId}` | 获取子任务详情 | 接收方 |
| PUT | `/api/design/task/{taskId}/status` | 更新任务状态（接受/拒绝/完成） | 接收方 |
| GET | `/api/design/task/{taskId}/can-accept` | 判断打版任务是否可以接受（检查面料是否完成） | `ROLE_PATTERN_SERVICE`, `ROLE_PATTERN_MAKER` |
| POST | `/api/design/task/{taskId}/ship` | 面料商发货（提交物流信息） | `ROLE_SUPPLIER` |
| POST | `/api/design/task/{taskId}/upload-result` | 上传结果文件（报价单/纸样） | 接收方 |

### 6.4 面料库管理

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/fabric-library/list` | 浏览面料库（分页，按品类筛选） | `ROLE_DESIGNER`, `ROLE_SUPPLIER` |
| GET | `/api/fabric-library/supplier-list` | 面料商管理自己的面料库 | `ROLE_SUPPLIER` |
| POST | `/api/fabric-library` | 面料商添加面料 | `ROLE_SUPPLIER` |
| PUT | `/api/fabric-library/{id}` | 面料商编辑面料 | `ROLE_SUPPLIER` |
| DELETE | `/api/fabric-library/{id}` | 面料商下架面料 | `ROLE_SUPPLIER` |

### 6.5 统计与通知

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/design/statistics/tenant` | 租户维度统计（任务完成率、平均耗时、Token消耗） | `ROLE_BOSS` |
| GET | `/api/design/statistics/platform` | 平台维度总览（管理员） | `ROLE_ADMIN` |
| GET | `/api/message/list` | 获取当前用户站内信列表 | 所有角色 |
| PUT | `/api/message/{id}/read` | 标记消息已读 | 所有角色 |

### 6.6 管理端API

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/admin/assign-rule` | 管理员新增分配规则 | `ROLE_ADMIN` |
| PUT | `/api/admin/assign-rule/{id}` | 编辑分配规则 | `ROLE_ADMIN` |
| GET | `/api/admin/assign-rule/list` | 查看分配规则列表 | `ROLE_ADMIN` |
| POST | `/api/admin/supplier/verify` | 审核面料商/版师入驻申请 | `ROLE_ADMIN` |
| GET | `/api/admin/supplier/list` | 查看所有供应商/版师租户 | `ROLE_ADMIN` |

### 6.7 租户绑定设计助理（管理端）

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/tenant/assistant/list` | 获取本租户的设计助理列表 | `ROLE_BOSS` |
| POST | `/api/tenant/assistant/bind` | 绑定一名员工为设计助理 | `ROLE_BOSS` |
| DELETE | `/api/tenant/assistant/{id}` | 解除绑定 | `ROLE_BOSS` |


## 7. 前端页面规划

### 7.1 租户端（merchant-web）新增模块

所有页面应放在 `src/views/design-assistant/` 目录下，路由整合到现有路由配置中。由于AI设计助手是一个独立功能模块，暂不强制使用qiankun微前端，后续插件数量增多时可重构为独立子应用。

| 页面路径 | 功能 | 对应角色 |
|---------|------|----------|
| `views/design-requirement/create.vue` | 需求创建（多模态表单+AI对话界面） | 设计师 |
| `views/design-requirement/list.vue` | 需求列表（我的提交） | 设计师、老板 |
| `views/design-requirement/detail.vue` | 需求详情（含子任务状态、物流跟踪） | 设计师、老板 |
| `views/design-assistant/pending-list.vue` | 助理待办列表 | 设计助理 |
| `views/design-assistant/detail.vue` | 助理复核页面（显示预拆分子任务、可编辑） | 设计助理 |
| `views/task/my-tasks.vue` | 供应商/版师的任务列表（接受/拒绝/发货/完成） | 面料商、版师 |
| `views/task/task-detail.vue` | 子任务详情 | 任务接收方 |
| `views/task/board.vue` | 任务看板（全局状态视图） | 设计师、老板 |
| `views/fabric-library/manage.vue` | 面料商管理面料库 | 面料商 |
| `views/fabric-library/select.vue` | 设计师选择现成面料（模态框） | 设计师 |
| `views/statistics/tenant.vue` | 租户统计（ECharts图表：任务完成率、Token消耗） | 老板 |
| `views/message/list.vue` | 站内信列表 | 所有角色 |

### 7.2 管理端（admin-web）新增模块

| 页面路径 | 功能 | 对应角色 |
|---------|------|----------|
| `views/assign-rule/index.vue` | 分配规则管理（增删改查） | 管理员 |
| `views/supplier/audit.vue` | 供应商/版师入驻审核 | 管理员 |
| `views/supplier/list.vue` | 供应商/版师租户列表 | 管理员 |
| `views/statistics/platform.vue` | 平台总览（插件热度、总Token消耗、充值总额） | 管理员 |

### 7.3 前端关键组件开发指引

**1. AI对话组件 (`AiChat.vue`)**
- 功能：右侧对话区域，左侧实时的AI总结预览区
- 通信：与后端 `/chat` 接口通过SSE或普通POST交互（非流式优先，降低复杂度）
- 总结生成：调用 `/summarize` 接口，展示可编辑的JSON视图（通过 `vue-json-editor` 或类似组件）
- 状态：对话历史保存在Pinia store中，支持断点续传（页面刷新后恢复）

**2. 语音输入组件 (`VoiceInput.vue`)**
- 功能：点击麦克风图标开始录音，实时转文字；同时提供「上传音频文件」按钮
- 实现方式：使用 `window.SpeechRecognition` 或 `webkitSpeechRecognition`，封装为Vue组件。主要代码结构：
  ```typescript
  const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.onresult = (event) => {
    const transcript = Array.from(event.results)
      .map(result => result[0].transcript)
      .join('')
    transcript.value = transcript
  }
  ```
- 降级方案：使用百度语音识别API替代，需申请API密钥，适合对准确率要求更高的场景

**3. 助理子任务编辑组件**
- 功能：显示任务列表（面料任务和打版任务），支持行内编辑
- 技术：使用 `element-plus` 的 `el-table` + 行内编辑模式，每个子任务可修改分配对象、截止时间、content JSON内容
- 新增子任务：弹窗选择任务类型（面料/打版），填写分配对象等信息

**4. 物流信息组件 (`LogisticsInfo.vue`)**
- 功能：展示物流轨迹（时间轴样式），调用快递鸟API获取实时轨迹
- 时间轴UI可复用 `el-timeline` 组件

**5. 任务看板组件**
- 功能：使用ECharts展示各任务状态分布、完成率趋势、Token消耗排行等
- 数据来源：调用 `/statistics/tenant` 接口


## 8. 开源组件集成方案

本节给出各开源组件的详细集成步骤，按推荐的优先级顺序排列。

### 8.1 APISIX（API网关）—— 建议优先集成

**选型理由**：APISIX是Apache顶级开源项目，采用etcd作为配置中心，支持毫秒级热更新。你的插件需要「动态上架/下架」，APISIX路由配置变更实时生效，非常适合插件化架构。

**集成方式**：
```bash
# 1. 通过Docker快速部署APISIX
docker run -d --name apisix -p 9180:9180 -p 9080:9080 apache/apisix:3.10.0-debian

# 2. 通过Admin API创建路由（插件上架时自动调用）
curl -X PUT http://127.0.0.1:9180/apisix/admin/routes/ai-design-assistant \
  -H 'X-API-KEY: your-api-key' -d '{
    "uri": "/api/plugins/ai_design_assistant/*",
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "localhost:8081": 1
      }
    }
  }'
```

**注意**：APISIX会接管所有外部请求，需配置好前端静态资源的代理规则，避免管理端和租户端访问受限。建议新用户量小时暂不加APISIX，直接从Spring Boot Gateway开始，用户量>500时迁移。

### 8.2 qiankun（微前端）—— 用于插件独立部署

**选型理由**：qiankun由蚂蚁金服开源，基于single-spa构建，支持多技术栈混合（Vue/React/Angular），提供JS沙箱和样式隔离，生产可用性经过阿里大规模验证。

**集成方式**（参见搜索结果）：
- 主应用安装：`pnpm i qiankun`
- 主应用注册子应用：使用 `registerMicroApps` 方法，指定子应用的 `name`、`entry` 和 `container`
- 子应用使用 `vite-plugin-qiankun` 插件，打包为UMD格式
- 子应用改造 `main.ts`，添加 `renderWithQiankun` 生命周期钩子

**建议**：AI设计助手前期作为普通Vue模块开发，暂不拆分为独立子应用。当平台上架3个以上插件后，再进行微前端重构。

### 8.3 轻量级工作流引擎（Easy Work）—— 可选

**选型理由**：Easy Work是基于状态机模型的轻量级Java流程引擎，学习成本比Camunda降低90%，性能比传统引擎提升3倍以上。**建议**：初期用数据库状态字段+业务逻辑判断即可满足需求。当流程超过10个节点、涉及多条件分支时，再引入Easy Work。

### 8.4 快递鸟API（物流查询）—— 建议接入

**选型理由**：覆盖国内外1200+物流服务商，统一接口接入，支持Webhook推送。

**集成步骤**：
1. 注册快递鸟开发者账号，获取AppKey和API ID
2. 在后端配置文件中添加快递鸟API密钥：
   ```yaml
   kdniao:
     app-key: your-app-key
     api-id: your-api-id
     api-url: https://api.kdniao.com/api/dist
   ```
3. 封装物流查询服务，支持即时查询（`OrderCode` + `LogisticCode`）和Webhook接收
4. 建议MD5签名验证，保障数据传输安全

### 8.5 Lago（开源计费系统）—— 后期扩容时使用

**选型理由**：开源事件驱动计费引擎，被Mistral AI、Groq等公司采用，支持按量计费、订阅+按量混合模式、预充值积分/钱包，Docker一键部署。

**建议**：前期使用自研方案（Redis计数 + MySQL落库 + 定时任务生成账单），实现简单、维护成本低。当租户数超过1000家或月Token调用量超过百万级时，再迁移至Lago。


## 9. 安全与风控

### 9.1 数据隔离

- 商家之间的所有需求数据、任务数据、面料库数据通过 `tenant_id` 物理隔离（数据库层面）
- 供应商/版师只能访问分配给自己的子任务，无权查看其他商家的数据
- 设计助理只能访问本租户转派的需求，无权访问其他租户的助理待办项
- OSS存储：每个租户/供应商建议使用独立bucket或路径前缀进行隔离

### 9.2 AI内容合规

- AI模型生成的需求总结和任务分配建议需要经过内容安全过滤，避免生成违法或不当内容
- 建议在API网关层增加内容审核中间件，调用第三方审核API（如阿里云内容安全）过滤敏感词汇
- 商家上传的商品图、描述等均需通过审核，避免平台因违法内容被牵连

### 9.3 计量计费精度

- Token消耗在AI对话接口 `/chat` 和 `/summarize` 中实时计费，在API返回前完成扣费
- 计费操作必须与接口调用在同一个事务（或补偿事务）中完成，避免调用成功但扣费失败的「不一致」问题
- 建议使用分布式事务或Saga模式，或使用Redis原子操作 + 异步补偿机制
- 关于Lago计费系统：Lago 基于事件驱动架构，支持每秒处理百万级计费事件，计费精度高，是后期扩展的理想选择

### 9.4 超时与重试

- 第三方AI模型API可能超时或异常，建议配置3次重试 + 降级处理
- 长时任务（如批量生成图片）可改为异步模式：接口立即返回 `task_id`，后台执行完成后通过Webhook回调通知前端
- 消息队列：推荐使用RabbitMQ或Kafka处理异步任务，并配合死信队列处理异常情况


## 10. 开发路线图与工作量估算

### 10.1 阶段划分

| 阶段 | 周期 | 主要任务 | 产出 |
|------|------|----------|------|
| **第一阶段：基础框架** | 1周 | DDL编写与执行、实体类生成、MyBatis Plus Mapper、基础CRUD、认证扩展 | 可运行的后端基础框架 |
| **第二阶段：核心流程** | 1.5周 | AI对话接口、需求创建、确认/转助理、自动拆分、助理复核与发布、状态流转、站内信系统 | 完整的核心业务流程可用 |
| **第三阶段：任务处理** | 1周 | 面料商/版师任务列表、接受/拒绝/完成、物流信息提交、板师开工条件约束、定时催办 | 任务闭环可用 |
| **第四阶段：前端页面** | 1.5周 | AI对话组件、语音输入组件、需求表单、助理工作台、任务看板、面料库管理 | 完整前端功能 |
| **第五阶段：管理端与统计** | 0.5周 | 分配规则配置、供应商审核、租户统计ECharts、平台总览 | 管理后台完整 |
| **第六阶段：测试与上线** | 0.5周 | 集成测试、单元测试、性能测试、文档整理 | 可上线产品 |

**总预估**：6周（约240人时）

> **工作量估算依据**：开发团队已具备完整的Spring Boot + Vue3技术栈，按照日均8小时、单人连续开发计算。上述估算已包含冗余，实际可能压缩至4-5周。

### 10.2 优先级说明

- **P0（必须有）**：需求创建、AI对话、确认/转助理、助理复核发布、面料商/版师任务处理、物流信息、板师开工条件约束
- **P1（应该有）**：面料库管理、定时催办、站内信通知、租户统计图表
- **P2（可以有）**：快递鸟API自动物流更新（初期手动输入单号即可）、微前端拆分、Lago计费系统集成


## 11. 风险提示与应对

### 11.1 技术风险

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| AI模型API不稳定或超时 | 用户对话卡死 | 设置合理超时（10秒），实现重试机制（最多3次），失败时返回友好提示并支持手动编辑 |
| 语音识别准确率不足 | 用户体验差 | 提供「编辑识别结果」入口；使用百度语音API作为降级方案；支持上传录音文件留证 |
| 并发调用导致数据库压力 | 响应延迟 | 初期加Redis缓存（会话状态、计数），服务无状态化支持水平扩展 |
| 第三方物流API配额超限 | 物流状态无法自动更新 | 初期允许面料商手动输入物流单号，不强制调用API；配置配额预警通知 |

### 11.2 产品风险

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 商家需求理解不一致 | 助理负担加重 | 持续优化AI Prompt，增加标注数据进行Fine-tune；定期收集助理反馈优化模型 |
| 设计助理短缺 | 商家效率下降 | 支持一个租户绑定多名设计助理；提供助理操作手册和培训支持 |
| 面料商/版师入驻少 | 任务无法分发 | 先通过管理员手动添加服务商，逐步开放注册入口，引入产业带资源 |

### 11.3 商标与合规风险

- 平台名称「濮院毛衫」基于第9类商标（已覆盖可下载软件），但SaaS云服务建议补充注册第42类（科学技术服务）
- 商家生成的AI内容需遵守大模型服务商条款，不得用于违法内容生成


## 12. 测试验收清单（Checklist）

### 12.1 基础功能

- [ ] 设计师创建需求：能正常选择图片/视频上传（最多9张），输入文本
- [ ] 语音识别：浏览器录音后成功转文字，上传音频文件后可正常保存
- [ ] AI对话：发送消息后AI正常回复，对话历史保存在会话中
- [ ] AI总结生成：返回正确的结构化JSON（包含面料+版型）
- [ ] 确认发布：子任务正常创建，面料商和版师收到站内信通知
- [ ] 转给助理：需求状态变为 `assistant_processing`，助理待办列表中能看到

### 12.2 助理功能

- [ ] 助理打开待处理请求，能看到预拆分的面料任务和打版任务（状态为`draft`）
- [ ] 助理可编辑子任务的任意字段（分配对象、截止时间、content JSON）
- [ ] 助理可新增子任务、删除子任务
- [ ] 助理点击发布后，所有子任务状态变为 `pending`，接收方收到通知

### 12.3 面料商/版师功能

- [ ] 面料商在我的任务中看到分配任务，可接受/拒绝/完成
- [ ] 面料商发货时可选择「线上发货」（物流公司+单号）或「线下发货」（填写备注）
- [ ] 打版任务在面料未完成时「接受」按钮禁用，提示原因
- [ ] 打版任务在面料完成后按钮可用，可正常接受并上传纸样文件
- [ ] 定时催办（09/14/18点）能正确识别超时任务并发送站内信

### 12.4 面料库

- [ ] 面料商可上传面料（图片、视频、规格、价格），并管理上下架状态
- [ ] 设计师创建需求时可浏览面料库并选择现成面料
- [ ] 选择现成面料后，面料任务自动分配给该面料商

### 12.5 权限与安全

- [ ] 设计师只能看到/操作本租户的需求
- [ ] 助理只能看到本租户绑定的待处理需求
- [ ] 面料商只能看到分配给自己的面料任务
- [ ] 板师只能看到分配给自己的打版任务
- [ ] 管理员可配置分配规则，关键词匹配正确
- [ ] Token计费在AI调用时准确扣除，余额不足时返回提示且调用失败

### 12.6 性能与稳定性

- [ ] 单用户并发对话10轮，API响应 < 2秒
- [ ] 100个并发请求时数据库连接池不耗尽（压测验证）
- [ ] 定时任务执行时不影响正常接口响应


## 13. 附录

### 13.1 关键API调用示例

**创建需求**：
```http
POST /api/design/requirement/create
X-Tenant-Id: tenant_001
Content-Type: application/json

{
  "title": "秋季连衣裙需求",
  "raw_text": "需要一款秋季连衣裙，重磅真丝面料，法式小尖领，腰部收褶设计",
  "raw_images": ["oss://image1.jpg", "oss://image2.jpg"],
  "raw_videos": [],
  "conversation_history": [
    {"role": "user", "content": "我想要做一款连衣裙"},
    {"role": "assistant", "content": "请问您对面料有什么要求？"},
    {"role": "user", "content": "重磅真丝吧，20mm以上"}
  ]
}
```

**AI对话**：
```http
POST /api/design/requirement/chat
X-Tenant-Id: tenant_001
Content-Type: application/json

{
  "session_id": "req_123",  // 新建时传空字符串或不传，后端返回新的session_id
  "message": "领口要法式小尖领",
  "requirement_id": 0  // 新建时传0
}
```

**助理发布需求**：
```http
POST /api/design/assistant/publish/123
X-Tenant-Id: tenant_001
X-User-Id: assistant_456
Content-Type: application/json

{
  "force_publish": false  // true表示忽略面料未完成等警告
}
```

**查询余额（复用现有接口）**：
```http
GET /api/v1/account/balance
X-Tenant-Id: tenant_001
```

### 13.2 状态流转速查表

| 实体 | 状态 | 可选流转 |
|------|------|----------|
| **design_requirement** | draft → assistant_processing → released → completed / cancelled | 设计师确认发布时助理环节可跳过 |
| **design_task (面料)** | draft → pending → accepted → shipped → delivered / done / cancelled | 发货后物流状态独立管理 |
| **design_task (打版)** | draft → pending → (等待面料完成) → accepted → done / cancelled | 面料未完成时无法接受 |

### 13.3 参考文档链接

- qiankun 官方文档：https://qiankun.umijs.org/
- APISIX 官方文档：https://apisix.apache.org/
- 快递鸟开发者中心：https://www.kdniao.com/
- Lago GitHub：https://github.com/getlago/lago
- Easy Work GitHub：https://github.com/jamhihi/easy-work


**文档编写者**：AI产品团队
**最后更新**：2026-05-19
**文档审核状态**：□ 待评审 ■ 待确认后启动开发

---


## 开发启动确认

请开发团队基于以上PRD进行技术方案评估，如有任何疑问，请评估、完善，确认后即可进入第一阶段开发。