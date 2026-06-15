-- ============================================================
-- V1.0__baseline_schema.sql
-- 濮院毛衫 AI 平台 - 基线数据库架构
-- 
-- 此脚本为 Flyway 基线迁移，包含平台所有核心表结构。
-- 基于 schema_mvp.sql，使用 IF NOT EXISTS 确保幂等性。
-- 
-- 执行策略: baseline-on-migrate=true，新环境从 V1.0 开始。
-- ============================================================

-- ============================================================
-- 租户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码',
    `name` VARCHAR(100) NOT NULL COMMENT '租户名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `level` VARCHAR(20) NOT NULL DEFAULT 'basic' COMMENT '等级: basic/vip',
    `tenant_type` VARCHAR(20) NOT NULL DEFAULT 'standard' COMMENT '类型: standard/enterprise',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ============================================================
-- 用户账户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户ID',
    `mobile` VARCHAR(20) NULL COMMENT '手机号',
    `nickname` VARCHAR(50) NULL COMMENT '昵称',
    `avatar_url` VARCHAR(500) NULL COMMENT '用户头像URL',
    `phone` VARCHAR(20) NULL COMMENT '手机号(冗余)',
    `wechat_openid` VARCHAR(100) NULL COMMENT '微信OpenID',
    `wechat_unionid` VARCHAR(100) NULL COMMENT '微信UnionID',
    `email` VARCHAR(100) NULL COMMENT '邮箱',
    `role_code` VARCHAR(50) NOT NULL DEFAULT 'merchant_operator' COMMENT '角色代码',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_mobile` (`mobile`),
    INDEX `idx_wechat_openid` (`wechat_openid`),
    INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

-- ============================================================
-- 租户用户关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_code` VARCHAR(50) NOT NULL DEFAULT 'merchant_operator' COMMENT '在租户中的角色',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户用户关联表';

-- ============================================================
-- 租户角色配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant_role_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) NULL COMMENT '角色描述',
    `permissions` JSON NULL COMMENT '权限列表（JSON格式）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户角色配置表';

-- ============================================================
-- 成员角色变更审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `member_role_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `member_user_id` BIGINT NOT NULL COMMENT '成员用户ID',
    `operator_user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `action` VARCHAR(20) NOT NULL COMMENT '操作类型: invite/add_role/remove_role/remove_member/enable/disable',
    `old_role` VARCHAR(20) NULL COMMENT '原角色',
    `new_role` VARCHAR(20) NULL COMMENT '新角色',
    `old_status` VARCHAR(20) NULL COMMENT '原状态',
    `new_status` VARCHAR(20) NULL COMMENT '新状态',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_member_user_id` (`member_user_id`),
    INDEX `idx_operator_user_id` (`operator_user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成员角色变更审计日志表';

-- ============================================================
-- 用户登录日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tenant_id` BIGINT NULL COMMENT '租户ID',
    `login_type` VARCHAR(20) NOT NULL COMMENT '登录类型: sms/wechat',
    `ip_address` VARCHAR(50) NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) NULL COMMENT '用户代理',
    `login_result` TINYINT NOT NULL DEFAULT 1 COMMENT '登录结果: 1=成功, 0=失败',
    `fail_reason` VARCHAR(200) NULL COMMENT '失败原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

-- ============================================================
-- 租户助理表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant_assistant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '助理用户ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户助理表';

-- ============================================================
-- 插件表
-- ============================================================
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '插件ID',
    `plugin_code` VARCHAR(50) NOT NULL COMMENT '插件编码',
    `name` VARCHAR(100) NOT NULL COMMENT '插件名称',
    `description` VARCHAR(500) NULL COMMENT '插件描述',
    `category` VARCHAR(50) NULL COMMENT '分类',
    `icon_url` VARCHAR(500) NULL COMMENT '图标URL',
    `version` VARCHAR(20) NOT NULL DEFAULT '1.0.0' COMMENT '版本号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive/draft',
    `default_token_cost` INT NOT NULL DEFAULT 10 COMMENT '默认Token消耗',
    `ai_model` VARCHAR(100) NULL COMMENT '插件绑定的AI模型ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plugin_code` (`plugin_code`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';

-- ============================================================
-- 租户插件关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant_plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `plugin_id` BIGINT NOT NULL COMMENT '插件ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive',
    `custom_config` JSON NULL COMMENT '自定义配置',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_plugin` (`tenant_id`, `plugin_id`),
    INDEX `idx_plugin_id` (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户插件关联表';

-- ============================================================
-- 插件配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `plugin_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` BIGINT NOT NULL COMMENT '插件ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT NULL COMMENT '配置值',
    `value_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型: string/number/boolean/json',
    `description` VARCHAR(200) NULL COMMENT '配置说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plugin_key` (`plugin_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件配置表';

-- ============================================================
-- 插件部署任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS `plugin_deployment_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` BIGINT NOT NULL COMMENT '插件ID',
    `action` VARCHAR(20) NOT NULL COMMENT '操作: deploy/update/offline',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
    `result_message` TEXT NULL COMMENT '结果信息',
    `operator_id` BIGINT NULL COMMENT '操作人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plugin_id` (`plugin_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件部署任务表';

-- ============================================================
-- AI 提供商表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_provider` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `provider_code` VARCHAR(50) NOT NULL COMMENT '提供商编码',
    `provider_name` VARCHAR(100) NOT NULL COMMENT '提供商名称',
    `base_url` VARCHAR(500) NULL COMMENT 'API基础URL',
    `api_key` VARCHAR(500) NULL COMMENT 'API密钥',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_code` (`provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI提供商表';

-- ============================================================
-- AI 场景表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_scene` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scene_code` VARCHAR(50) NOT NULL COMMENT '场景编码',
    `scene_name` VARCHAR(100) NOT NULL COMMENT '场景名称',
    `description` VARCHAR(200) NULL COMMENT '场景描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scene_code` (`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI场景表';

-- ============================================================
-- AI 场景模型关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_scene_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scene_id` BIGINT NOT NULL COMMENT '场景ID',
    `provider_id` BIGINT NOT NULL COMMENT '提供商ID',
    `model_id` VARCHAR(100) NOT NULL COMMENT '模型ID',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认模型',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scene_provider_model` (`scene_id`, `provider_id`, `model_id`),
    INDEX `idx_provider_id` (`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI场景模型关联表';

-- ============================================================
-- AI 会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plugin_id` BIGINT NULL COMMENT '插件ID',
    `session_type` VARCHAR(20) NOT NULL DEFAULT 'chat' COMMENT '会话类型',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/closed',
    `metadata` JSON NULL COMMENT '元数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_user` (`tenant_id`, `user_id`),
    INDEX `idx_plugin_id` (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

-- ============================================================
-- 账户钱包表
-- ============================================================
CREATE TABLE IF NOT EXISTS `account_wallet` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `balance` BIGINT NOT NULL DEFAULT 0 COMMENT '余额（分）',
    `total_charged` BIGINT NOT NULL DEFAULT 0 COMMENT '累计充值（分）',
    `total_consumed` BIGINT NOT NULL DEFAULT 0 COMMENT '累计消费（分）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户钱包表';

-- ============================================================
-- 幂等记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `idempotency_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `idempotency_key` VARCHAR(100) NOT NULL COMMENT '幂等键',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `result` JSON NULL COMMENT '处理结果',
    `status` VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT '状态: processing/success/failed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    INDEX `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等记录表';

-- ============================================================
-- 计费账本表
-- ============================================================
CREATE TABLE IF NOT EXISTS `billing_ledger` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NULL COMMENT '用户ID',
    `transaction_type` VARCHAR(20) NOT NULL COMMENT '交易类型: charge/consume/refund',
    `amount` BIGINT NOT NULL COMMENT '金额（分）',
    `balance_before` BIGINT NOT NULL COMMENT '交易前余额',
    `balance_after` BIGINT NOT NULL COMMENT '交易后余额',
    `related_plugin_id` BIGINT NULL COMMENT '关联插件ID',
    `related_order_id` VARCHAR(50) NULL COMMENT '关联订单ID',
    `remark` VARCHAR(200) NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_related_order` (`related_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计费账本表';

-- ============================================================
-- 充值订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `recharge_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` BIGINT NOT NULL COMMENT '充值金额（分）',
    `pay_amount` BIGINT NULL COMMENT '实际支付金额（分）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/paid/failed/closed',
    `pay_channel` VARCHAR(20) NULL COMMENT '支付渠道: wechat',
    `transaction_id` VARCHAR(100) NULL COMMENT '第三方交易号',
    `paid_at` DATETIME NULL COMMENT '支付时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';

-- ============================================================
-- 每日账单汇总表
-- ============================================================
CREATE TABLE IF NOT EXISTS `billing_statement_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `statement_date` DATE NOT NULL COMMENT '账单日期',
    `total_charge` BIGINT NOT NULL DEFAULT 0 COMMENT '当日充值总额（分）',
    `total_consume` BIGINT NOT NULL DEFAULT 0 COMMENT '当日消费总额（分）',
    `opening_balance` BIGINT NOT NULL DEFAULT 0 COMMENT '期初余额',
    `closing_balance` BIGINT NOT NULL DEFAULT 0 COMMENT '期末余额',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_date` (`tenant_id`, `statement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日账单汇总表';

-- ============================================================
-- 插件调用日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `plugin_invoke_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plugin_id` BIGINT NOT NULL COMMENT '插件ID',
    `session_id` BIGINT NULL COMMENT '会话ID',
    `invoke_type` VARCHAR(20) NOT NULL DEFAULT 'sync' COMMENT '调用类型: sync/async',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
    `input_data` JSON NULL COMMENT '输入数据',
    `output_data` JSON NULL COMMENT '输出数据',
    `token_cost` INT NOT NULL DEFAULT 0 COMMENT 'Token消耗',
    `duration_ms` BIGINT NULL COMMENT '耗时（毫秒）',
    `error_message` TEXT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_plugin_id` (`plugin_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件调用日志表';

-- ============================================================
-- 设计需求表
-- ============================================================
CREATE TABLE IF NOT EXISTS `design_requirement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `title` VARCHAR(200) NOT NULL COMMENT '需求标题',
    `raw_text` TEXT NULL COMMENT '原始需求文本',
    `summary` TEXT NULL COMMENT 'AI生成摘要',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/assistant_processing/released/completed/cancelled',
    `assistant_id` BIGINT NULL COMMENT '助理ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计需求表';

-- ============================================================
-- 设计任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS `design_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `requirement_id` BIGINT NOT NULL COMMENT '需求ID',
    `task_type` VARCHAR(20) NOT NULL COMMENT '任务类型: fabric/pattern',
    `assignee_id` BIGINT NULL COMMENT '承接人ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/accepted/shipped/delivered/done',
    `result_url` VARCHAR(500) NULL COMMENT '结果URL',
    `logistics_company` VARCHAR(100) NULL COMMENT '物流公司',
    `logistics_tracking_no` VARCHAR(100) NULL COMMENT '物流单号',
    `can_accept` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可以接受',
    `cannot_accept_reason` VARCHAR(200) NULL COMMENT '不能接受原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_requirement_id` (`requirement_id`),
    INDEX `idx_assignee_id` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计任务表';

-- ============================================================
-- 面料库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `fabric_library` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `name` VARCHAR(200) NOT NULL COMMENT '面料名称',
    `description` TEXT NULL COMMENT '描述',
    `image_url` VARCHAR(500) NULL COMMENT '图片URL',
    `category` VARCHAR(50) NULL COMMENT '分类',
    `is_visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面料库表';

-- ============================================================
-- 供应商注册表
-- ============================================================
CREATE TABLE IF NOT EXISTS `supplier_registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `supplier_type` VARCHAR(20) NOT NULL COMMENT '供应商类型: fabric/pattern',
    `company_name` VARCHAR(200) NULL COMMENT '公司名称',
    `contact_name` VARCHAR(100) NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) NULL COMMENT '联系电话',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商注册表';

-- ============================================================
-- 供应商协作表
-- ============================================================
CREATE TABLE IF NOT EXISTS `supplier_collaboration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `designer_tenant_id` BIGINT NOT NULL COMMENT '设计师租户ID',
    `supplier_tenant_id` BIGINT NOT NULL COMMENT '供应商租户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collaboration` (`designer_tenant_id`, `supplier_tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商协作表';

-- ============================================================
-- 任务分配规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS `task_assign_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_type` VARCHAR(20) NOT NULL COMMENT '任务类型',
    `rule_type` VARCHAR(20) NOT NULL DEFAULT 'round_robin' COMMENT '规则类型: round_robin/random/weighted',
    `rule_config` JSON NULL COMMENT '规则配置',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务分配规则表';

-- ============================================================
-- 任务提醒日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `task_remind_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `remind_type` VARCHAR(20) NOT NULL COMMENT '提醒类型: sms/app_push',
    `remind_result` TINYINT NOT NULL DEFAULT 1 COMMENT '提醒结果: 1=成功, 0=失败',
    `fail_reason` VARCHAR(200) NULL COMMENT '失败原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务提醒日志表';

-- ============================================================
-- 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT NULL COMMENT '配置值',
    `value_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型',
    `description` VARCHAR(200) NULL COMMENT '配置说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================================
-- 租户限流配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant_rate_limit_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `rate_limit_rpm` INT NOT NULL DEFAULT 10 COMMENT '每分钟请求限制',
    `burst_multiplier` DECIMAL(3,1) NOT NULL DEFAULT 2.0 COMMENT '突发系数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户限流配置表';

-- ============================================================
-- 消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `message_type` VARCHAR(20) NOT NULL COMMENT '消息类型: system/plugin/billing',
    `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content` TEXT NULL COMMENT '消息内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_user` (`tenant_id`, `user_id`),
    INDEX `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NULL COMMENT '租户ID',
    `user_id` BIGINT NULL COMMENT '用户ID',
    `action` VARCHAR(50) NOT NULL COMMENT '操作',
    `target_type` VARCHAR(50) NULL COMMENT '目标类型',
    `target_id` VARCHAR(50) NULL COMMENT '目标ID',
    `detail` JSON NULL COMMENT '操作详情',
    `ip_address` VARCHAR(50) NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) NULL COMMENT '用户代理',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ============================================================
-- 插入默认角色配置（幂等）
-- ============================================================
INSERT INTO `tenant_role_config` (`role_code`, `role_name`, `description`, `permissions`, `sort_order`, `is_system`) VALUES
('boss', 'Boss', 'Studio owner with all permissions', '["*"]', 1, 1),
('tenant_admin', 'Admin', 'Can manage members and access all features', '["member_manage","plugin_invoke","billing_view","settings_view"]', 2, 1),
('tenant_operator', 'Operator', 'Can invoke AI plugins and view billing', '["plugin_invoke","billing_view"]', 3, 1),
('tenant_viewer', 'Viewer', 'Read-only access', '["billing_view"]', 4, 1)
ON DUPLICATE KEY UPDATE
`role_name` = VALUES(`role_name`),
`description` = VALUES(`description`),
`permissions` = VALUES(`permissions`),
`sort_order` = VALUES(`sort_order`);
