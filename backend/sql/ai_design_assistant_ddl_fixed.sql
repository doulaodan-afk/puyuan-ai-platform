-- AI设计助手插件数据库 DDL - 修正版
-- 版本：1.0
-- 日期：2026-05-19

-- ========================================
-- 1. 租户-设计助理绑定表
-- ========================================
CREATE TABLE IF NOT EXISTS `tenant_assistant` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '商家租户ID',
  `assistant_user_id` BIGINT NOT NULL COMMENT '设计助理 user_account.id',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_assistant_user_id (`assistant_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户设计助理绑定表';

-- ========================================
-- 2. 设计需求主表
-- ========================================
CREATE TABLE IF NOT EXISTS `design_requirement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
  `creator_id` BIGINT NOT NULL COMMENT '设计师 user_account.id',
  `title` VARCHAR(200) COMMENT '需求标题',
  `raw_images` JSON COMMENT '图片URL数组',
  `raw_videos` JSON COMMENT '视频URL数组',
  `raw_audio_url` VARCHAR(500) COMMENT '语音留证文件URL',
  `raw_text` TEXT COMMENT '设计师手动输入的文本',
  `conversation_history` JSON COMMENT '与AI的对话记录（role, content, time）',
  `ai_summary` TEXT COMMENT 'AI最终生成的结构化总结（JSON）',
  `designer_approved` TINYINT DEFAULT 0 COMMENT '0-未确认,1-确认发布,2-转助理',
  `assistant_id` BIGINT DEFAULT 0 COMMENT '指派的助理 user_account.id（当转助理时）',
  `status` VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/assistant_processing/released/completed/cancelled',
  `total_token_cost` INT DEFAULT 0 COMMENT '消耗的Token总数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_creator_id (`creator_id`),
  INDEX idx_status (`status`),
  INDEX idx_created_at (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计需求表';

-- ========================================
-- 3. 子任务表（面料/打版）
-- ========================================
CREATE TABLE IF NOT EXISTS `design_task` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `requirement_id` BIGINT NOT NULL COMMENT '关联需求ID',
  `task_type` VARCHAR(20) NOT NULL COMMENT 'fabric/pattern',
  `assignee_type` VARCHAR(20) NOT NULL COMMENT 'supplier/pattern_service/internal',
  `assignee_id` BIGINT NOT NULL COMMENT '接收方的 tenant.id（外部）或 user_account.id（内部）',
  `content` JSON COMMENT '任务详情JSON（面料规格/版型要求等）',
  `status` VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/pending/accepted/shipped/delivered/rejected/done/cancelled',
  `deadline` DATETIME COMMENT '截止时间',
  `result_url` VARCHAR(500) COMMENT '结果文件URL（报价单/纸样等）',
  `fabric_task_id` BIGINT DEFAULT 0 COMMENT '仅对pattern任务：关联的fabric任务ID',
  `logistics_company` VARCHAR(50) COMMENT '物流公司',
  `logistics_tracking_no` VARCHAR(100) COMMENT '物流单号',
  `logistics_status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/shipped/delivered',
  `offline_logistics_note` TEXT COMMENT '线下物流备注',
  `shipped_at` DATETIME COMMENT '发货时间',
  `delivered_at` DATETIME COMMENT '送达时间',
  `notified_at` DATETIME COMMENT '上次催办时间',
  `reject_reason` TEXT COMMENT '拒绝原因',
  `completed_at` DATETIME COMMENT '完成时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_requirement_id (`requirement_id`),
  INDEX idx_assignee_id (`assignee_id`),
  INDEX idx_status (`status`),
  INDEX idx_fabric_task_id (`fabric_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计子任务表';

-- ========================================
-- 4. 面料库表
-- ========================================
CREATE TABLE IF NOT EXISTS `fabric_library` (
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
  INDEX idx_is_visible (`is_visible`),
  INDEX idx_category (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面料库表';

-- ========================================
-- 5. 分配规则表
-- ========================================
CREATE TABLE IF NOT EXISTS `task_assign_rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `keyword` VARCHAR(100) NOT NULL COMMENT '关键词（如"真丝"）',
  `target_tenant_id` BIGINT NOT NULL COMMENT '匹配的面料商/版师租户ID',
  `task_type` VARCHAR(20) NOT NULL COMMENT 'fabric/pattern',
  `priority` INT DEFAULT 0 COMMENT '优先级，越高越优先',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_keyword (`keyword`),
  INDEX idx_task_type (`task_type`),
  INDEX idx_enabled (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务分配规则表';

-- ========================================
-- 6. 站内信表
-- ========================================
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `receiver_id` BIGINT NOT NULL COMMENT '接收方 user_account.id',
  `sender_id` BIGINT DEFAULT 0 COMMENT '发送方 user_account.id（系统消息为0）',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息内容',
  `type` VARCHAR(20) DEFAULT 'system' COMMENT 'system/task/remind',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
  `related_id` BIGINT DEFAULT 0 COMMENT '关联ID（需求ID/任务ID等）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_receiver_id (`receiver_id`),
  INDEX idx_is_read (`is_read`),
  INDEX idx_type (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信表';

-- ========================================
-- 7. 催办记录表
-- ========================================
CREATE TABLE IF NOT EXISTS `task_remind_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `remind_time` DATETIME NOT NULL COMMENT '催办时间',
  `remind_channel` VARCHAR(20) DEFAULT 'internal' COMMENT 'internal/email/sms',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task_id (`task_id`),
  INDEX idx_remind_time (`remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务催办记录表';

-- ========================================
-- 8. AI会话表（用于保存会话上下文）
-- ========================================
CREATE TABLE IF NOT EXISTS `ai_session` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `session_id` VARCHAR(100) NOT NULL UNIQUE COMMENT '会话ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `requirement_id` BIGINT DEFAULT 0 COMMENT '关联的需求ID（创建后更新）',
  `context` JSON COMMENT '会话上下文',
  `expires_at` DATETIME COMMENT '过期时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_session_id (`session_id`),
  INDEX idx_expires_at (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

-- ========================================
-- 9. 插件配置表（用于存储设计助手插件配置）
-- ========================================
CREATE TABLE IF NOT EXISTS `plugin_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `plugin_code` VARCHAR(50) NOT NULL COMMENT '插件代码',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值（JSON）',
  `description` VARCHAR(200) COMMENT '配置说明',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plugin_code (`plugin_code`),
  UNIQUE KEY uk_plugin_key (`plugin_code`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件配置表';

-- ========================================
-- 初始化种子数据
-- ========================================

-- 插入AI设计助手插件
INSERT INTO `plugin` (`id`, `plugin_id`, `name`, `version`, `backend_api`, `frontend_entry`, `billing_type`, `default_token_cost`, `status`, `review_status`, `created_at`, `updated_at`)
VALUES
(3020, 'ai_design_assistant', 'AI设计助手', '1.0.0', '/api/plugin/invoke/ai_design_assistant', '/design-requirement/create', 'token', 5, 1, 'pass', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
version = VALUES(version),
backend_api = VALUES(backend_api),
frontend_entry = VALUES(frontend_entry),
updated_at = NOW();

-- 为示例租户启用插件
INSERT IGNORE INTO `tenant_plugin` (`tenant_id`, `plugin_id`, `enabled`, `config_json`)
SELECT id, 'ai_design_assistant', 1, JSON_OBJECT()
FROM `tenant`
WHERE id IN (2001, 2002);

-- 插入默认分配规则
INSERT INTO `task_assign_rule` (`rule_name`, `keyword`, `target_tenant_id`, `task_type`, `priority`, `enabled`)
VALUES
('真丝面料供应商', '真丝', 3001, 'fabric', 10, 1),
('羊毛面料供应商', '羊毛', 3002, 'fabric', 10, 1),
('棉麻面料供应商', '棉麻', 3003, 'fabric', 10, 1),
('通用版师服务商', '默认', 4001, 'pattern', 5, 1);

-- 插入插件配置
INSERT INTO `plugin_config` (`plugin_code`, `config_key`, `config_value`, `description`)
VALUES
('ai_design_assistant', 'system_prompt', '你是一个专业的服装设计专家，帮助设计师完善需求。', 'AI对话系统提示词'),
('ai_design_assistant', 'token_cost_per_chat', '5', '每次对话消耗Token数'),
('ai_design_assistant', 'token_cost_summarize', '15', '生成总结消耗Token数'),
('ai_design_assistant', 'remind_times', '09:00,14:00,18:00', '催办时间点')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

-- ========================================
-- 创建视图（用于查询优化）
-- ========================================

-- 需求统计视图
DROP VIEW IF EXISTS `v_requirement_stats`;
CREATE VIEW `v_requirement_stats` AS
SELECT
    tenant_id,
    COUNT(*) as total,
    SUM(CASE WHEN status = 'draft' THEN 1 ELSE 0 END) as draft_count,
    SUM(CASE WHEN status = 'assistant_processing' THEN 1 ELSE 0 END) as processing_count,
    SUM(CASE WHEN status = 'released' THEN 1 ELSE 0 END) as released_count,
    SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed_count,
    SUM(total_token_cost) as total_tokens
FROM `design_requirement`
GROUP BY tenant_id;

-- 任务统计视图
DROP VIEW IF EXISTS `v_task_stats`;
CREATE VIEW `v_task_stats` AS
SELECT
    assignee_id,
    task_type,
    COUNT(*) as total,
    SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) as pending_count,
    SUM(CASE WHEN status = 'accepted' THEN 1 ELSE 0 END) as accepted_count,
    SUM(CASE WHEN status = 'shipped' THEN 1 ELSE 0 END) as shipped_count,
    SUM(CASE WHEN status = 'delivered' THEN 1 ELSE 0 END) as delivered_count,
    SUM(CASE WHEN status = 'done' THEN 1 ELSE 0 END) as done_count,
    SUM(CASE WHEN status = 'rejected' THEN 1 ELSE 0 END) as rejected_count
FROM `design_task`
GROUP BY assignee_id, task_type;