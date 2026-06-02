-- Puyuan AI Platform MVP schema (MySQL 8)
-- # MEMORY: this schema prioritizes billing idempotency and tenant isolation, because charge disputes are the highest business risk in MVP.

CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  level VARCHAR(32) DEFAULT 'basic',
  tenant_type VARCHAR(32) DEFAULT 'standard',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  mobile VARCHAR(32) NOT NULL,
  nickname VARCHAR(64),
  avatar_url VARCHAR(500) NULL COMMENT '用户头像URL',
  phone VARCHAR(20) NULL COMMENT '手机号',
  wechat_openid VARCHAR(100) NULL COMMENT '微信OpenID',
  wechat_unionid VARCHAR(100) NULL COMMENT '微信UnionID',
  email VARCHAR(100) NULL COMMENT '邮箱',
  password VARCHAR(255) NULL COMMENT '密码',
  role_code VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_tenant (tenant_id),
  INDEX idx_user_role (role_code),
  INDEX idx_user_mobile (mobile),
  INDEX idx_wechat_openid (wechat_openid),
  INDEX idx_email (email),
  CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plugin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_id VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  version VARCHAR(32) NOT NULL,
  backend_api VARCHAR(255) NOT NULL,
  frontend_entry VARCHAR(255),
  description VARCHAR(500) NULL COMMENT '插件描述',
  icon_url VARCHAR(500) NULL COMMENT '图标URL',
  frontend_path VARCHAR(255) NULL COMMENT '前端路径',
  lifecycle_status VARCHAR(32) DEFAULT 'development' COMMENT '生命周期状态',
  created_by BIGINT NULL COMMENT '创建人ID',
  tested_at DATETIME NULL COMMENT '测试时间',
  published_at DATETIME NULL COMMENT '发布时间',
  gray_tenant_ids VARCHAR(1000) NULL COMMENT '灰度租户ID列表',
  backend_deploy_config TEXT NULL COMMENT '后端部署配置',
  billing_type VARCHAR(16) NOT NULL,
  default_token_cost INT DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  review_status VARCHAR(16) NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plugin_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_plugin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plugin_id VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  config_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_plugin (tenant_id, plugin_id),
  INDEX idx_tenant_plugin_enabled (enabled),
  CONSTRAINT fk_tenant_plugin_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  CONSTRAINT fk_tenant_plugin_plugin FOREIGN KEY (plugin_id) REFERENCES plugin(plugin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS account_wallet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL UNIQUE,
  token_balance BIGINT NOT NULL DEFAULT 0,
  cash_balance DECIMAL(18,2) NOT NULL DEFAULT 0,
  frozen_token BIGINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_wallet_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS idempotency_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  scope VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64),
  response_body JSON,
  status VARCHAR(16) NOT NULL,
  expire_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_idem_scope (scope),
  INDEX idx_idem_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS billing_ledger (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  biz_no VARCHAR(64) NOT NULL UNIQUE,
  request_id VARCHAR(64),
  entry_type VARCHAR(32) NOT NULL,
  direction VARCHAR(8) NOT NULL,
  token_amount BIGINT NOT NULL DEFAULT 0,
  cash_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  balance_after BIGINT NOT NULL DEFAULT 0,
  plugin_id VARCHAR(64),
  status VARCHAR(16) NOT NULL,
  occurred_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ledger_tenant (tenant_id),
  INDEX idx_ledger_request (request_id),
  INDEX idx_ledger_plugin (plugin_id),
  INDEX idx_ledger_occurred (occurred_at),
  CONSTRAINT fk_ledger_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recharge_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  tenant_id BIGINT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  token_grant BIGINT NOT NULL,
  pay_channel VARCHAR(32) NOT NULL,
  pay_status VARCHAR(16) NOT NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_recharge_tenant (tenant_id),
  INDEX idx_recharge_status (pay_status),
  CONSTRAINT fk_recharge_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS billing_statement_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  token_in BIGINT NOT NULL DEFAULT 0,
  token_out BIGINT NOT NULL DEFAULT 0,
  call_count INT NOT NULL DEFAULT 0,
  amount_recharge DECIMAL(18,2) NOT NULL DEFAULT 0,
  amount_refund DECIMAL(18,2) NOT NULL DEFAULT 0,
  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_daily_statement (tenant_id, stat_date),
  INDEX idx_daily_date (stat_date),
  CONSTRAINT fk_daily_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plugin_invoke_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id VARCHAR(64) NOT NULL UNIQUE,
  tenant_id BIGINT NOT NULL,
  plugin_id VARCHAR(64) NOT NULL,
  model_vendor VARCHAR(32),
  token_used INT NOT NULL DEFAULT 0,
  latency_ms INT NOT NULL DEFAULT 0,
  result_code INT NOT NULL DEFAULT 0,
  risk_level VARCHAR(16) DEFAULT 'low',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_invoke_tenant (tenant_id),
  INDEX idx_invoke_plugin (plugin_id),
  INDEX idx_invoke_created (created_at),
  CONSTRAINT fk_invoke_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT,
  operator_id BIGINT,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32),
  target_id VARCHAR(64),
  detail_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_tenant (tenant_id),
  INDEX idx_audit_operator (operator_id),
  INDEX idx_audit_action (action),
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- AI璁捐鍔╂墜鎻掍欢鏁版嵁搴?DDL - 淇鐗?-- 鐗堟湰锛?.0
-- 鏃ユ湡锛?026-05-19

-- ========================================
-- 1. 绉熸埛-璁捐鍔╃悊缁戝畾琛?-- ========================================
CREATE TABLE IF NOT EXISTS `tenant_assistant` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '鍟嗗绉熸埛ID',
  `assistant_user_id` BIGINT NOT NULL COMMENT '璁捐鍔╃悊 user_account.id',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_assistant_user_id (`assistant_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绉熸埛璁捐鍔╃悊缁戝畾琛?;

-- ========================================
-- 2. 璁捐闇€姹備富琛?-- ========================================
CREATE TABLE IF NOT EXISTS `design_requirement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '鎵€灞炵鎴?,
  `creator_id` BIGINT NOT NULL COMMENT '璁捐甯?user_account.id',
  `title` VARCHAR(200) COMMENT '闇€姹傛爣棰?,
  `raw_images` JSON COMMENT '鍥剧墖URL鏁扮粍',
  `raw_videos` JSON COMMENT '瑙嗛URL鏁扮粍',
  `raw_audio_url` VARCHAR(500) COMMENT '璇煶鐣欒瘉鏂囦欢URL',
  `raw_text` TEXT COMMENT '璁捐甯堟墜鍔ㄨ緭鍏ョ殑鏂囨湰',
  `conversation_history` JSON COMMENT '涓嶢I鐨勫璇濊褰曪紙role, content, time锛?,
  `ai_summary` TEXT COMMENT 'AI鏈€缁堢敓鎴愮殑缁撴瀯鍖栨€荤粨锛圝SON锛?,
  `designer_approved` TINYINT DEFAULT 0 COMMENT '0-鏈‘璁?1-纭鍙戝竷,2-杞姪鐞?,
  `assistant_id` BIGINT DEFAULT 0 COMMENT '鎸囨淳鐨勫姪鐞?user_account.id锛堝綋杞姪鐞嗘椂锛?,
  `status` VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/assistant_processing/released/completed/cancelled',
  `total_token_cost` INT DEFAULT 0 COMMENT '娑堣€楃殑Token鎬绘暟',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_creator_id (`creator_id`),
  INDEX idx_status (`status`),
  INDEX idx_created_at (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁捐闇€姹傝〃';

-- ========================================
-- 3. 瀛愪换鍔¤〃锛堥潰鏂?鎵撶増锛?-- ========================================
CREATE TABLE IF NOT EXISTS `design_task` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `requirement_id` BIGINT NOT NULL COMMENT '鍏宠仈闇€姹侷D',
  `task_type` VARCHAR(20) NOT NULL COMMENT 'fabric/pattern',
  `assignee_type` VARCHAR(20) NOT NULL COMMENT 'supplier/pattern_service/internal',
  `assignee_id` BIGINT NOT NULL COMMENT '鎺ユ敹鏂圭殑 tenant.id锛堝閮級鎴?user_account.id锛堝唴閮級',
  `content` JSON COMMENT '浠诲姟璇︽儏JSON锛堥潰鏂欒鏍?鐗堝瀷瑕佹眰绛夛級',
  `status` VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/pending/accepted/shipped/delivered/rejected/done/cancelled',
  `deadline` DATETIME COMMENT '鎴鏃堕棿',
  `result_url` VARCHAR(500) COMMENT '缁撴灉鏂囦欢URL锛堟姤浠峰崟/绾告牱绛夛級',
  `fabric_task_id` BIGINT DEFAULT 0 COMMENT '浠呭pattern浠诲姟锛氬叧鑱旂殑fabric浠诲姟ID',
  `logistics_company` VARCHAR(50) COMMENT '鐗╂祦鍏徃',
  `logistics_tracking_no` VARCHAR(100) COMMENT '鐗╂祦鍗曞彿',
  `logistics_status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/shipped/delivered',
  `offline_logistics_note` TEXT COMMENT '绾夸笅鐗╂祦澶囨敞',
  `shipped_at` DATETIME COMMENT '鍙戣揣鏃堕棿',
  `delivered_at` DATETIME COMMENT '閫佽揪鏃堕棿',
  `notified_at` DATETIME COMMENT '涓婃鍌姙鏃堕棿',
  `reject_reason` TEXT COMMENT '鎷掔粷鍘熷洜',
  `completed_at` DATETIME COMMENT '瀹屾垚鏃堕棿',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_requirement_id (`requirement_id`),
  INDEX idx_assignee_id (`assignee_id`),
  INDEX idx_status (`status`),
  INDEX idx_fabric_task_id (`fabric_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁捐瀛愪换鍔¤〃';

-- ========================================
-- 4. 闈㈡枡搴撹〃
-- ========================================
CREATE TABLE IF NOT EXISTS `fabric_library` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `supplier_tenant_id` BIGINT NOT NULL COMMENT '闈㈡枡鍟嗙鎴稩D',
  `name` VARCHAR(100) NOT NULL COMMENT '闈㈡枡鍚嶇О',
  `category` VARCHAR(50) COMMENT '鍝佺被锛堢湡涓?缇婃瘺/妫夐夯锛?,
  `images` JSON COMMENT '鍥剧墖URL鏁扮粍',
  `video_url` VARCHAR(500) COMMENT '灏忔牱瑙嗛URL',
  `specs` JSON COMMENT '瑙勬牸锛堝厠閲嶃€侀棬骞呫€佹垚鍒嗙瓑锛?,
  `price_per_meter` DECIMAL(10,2) COMMENT '鍗曚环锛堝厓/绫筹級',
  `stock_status` VARCHAR(20) DEFAULT 'in_stock' COMMENT 'in_stock/out_of_stock',
  `is_visible` TINYINT DEFAULT 1 COMMENT '鏄惁鍦ㄥ墠绔睍绀?,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_supplier_tenant_id (`supplier_tenant_id`),
  INDEX idx_is_visible (`is_visible`),
  INDEX idx_category (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闈㈡枡搴撹〃';

-- ========================================
-- 5. 鍒嗛厤瑙勫垯琛?-- ========================================
CREATE TABLE IF NOT EXISTS `task_assign_rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `rule_name` VARCHAR(100) NOT NULL COMMENT '瑙勫垯鍚嶇О',
  `keyword` VARCHAR(100) NOT NULL COMMENT '鍏抽敭璇嶏紙濡?鐪熶笣"锛?,
  `target_tenant_id` BIGINT NOT NULL COMMENT '鍖归厤鐨勯潰鏂欏晢/鐗堝笀绉熸埛ID',
  `task_type` VARCHAR(20) NOT NULL COMMENT 'fabric/pattern',
  `priority` INT DEFAULT 0 COMMENT '浼樺厛绾э紝瓒婇珮瓒婁紭鍏?,
  `enabled` TINYINT DEFAULT 1 COMMENT '鏄惁鍚敤',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_keyword (`keyword`),
  INDEX idx_task_type (`task_type`),
  INDEX idx_enabled (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浠诲姟鍒嗛厤瑙勫垯琛?;

-- ========================================
-- 6. 绔欏唴淇¤〃
-- ========================================
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `receiver_id` BIGINT NOT NULL COMMENT '鎺ユ敹鏂?user_account.id',
  `sender_id` BIGINT DEFAULT 0 COMMENT '鍙戦€佹柟 user_account.id锛堢郴缁熸秷鎭负0锛?,
  `title` VARCHAR(200) NOT NULL COMMENT '娑堟伅鏍囬',
  `content` TEXT COMMENT '娑堟伅鍐呭',
  `type` VARCHAR(20) DEFAULT 'system' COMMENT 'system/task/remind',
  `is_read` TINYINT DEFAULT 0 COMMENT '鏄惁宸茶',
  `related_id` BIGINT DEFAULT 0 COMMENT '鍏宠仈ID锛堥渶姹侷D/浠诲姟ID绛夛級',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_receiver_id (`receiver_id`),
  INDEX idx_is_read (`is_read`),
  INDEX idx_type (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绔欏唴淇¤〃';

-- ========================================
-- 7. 鍌姙璁板綍琛?-- ========================================
CREATE TABLE IF NOT EXISTS `task_remind_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL COMMENT '浠诲姟ID',
  `remind_time` DATETIME NOT NULL COMMENT '鍌姙鏃堕棿',
  `remind_channel` VARCHAR(20) DEFAULT 'internal' COMMENT 'internal/email/sms',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task_id (`task_id`),
  INDEX idx_remind_time (`remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浠诲姟鍌姙璁板綍琛?;

-- ========================================
-- 8. AI浼氳瘽琛紙鐢ㄤ簬淇濆瓨浼氳瘽涓婁笅鏂囷級
-- ========================================
CREATE TABLE IF NOT EXISTS `ai_session` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `session_id` VARCHAR(100) NOT NULL UNIQUE COMMENT '浼氳瘽ID',
  `tenant_id` BIGINT NOT NULL COMMENT '绉熸埛ID',
  `user_id` BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
  `requirement_id` BIGINT DEFAULT 0 COMMENT '鍏宠仈鐨勯渶姹侷D锛堝垱寤哄悗鏇存柊锛?,
  `context` JSON COMMENT '浼氳瘽涓婁笅鏂?,
  `expires_at` DATETIME COMMENT '杩囨湡鏃堕棿',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_session_id (`session_id`),
  INDEX idx_expires_at (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI浼氳瘽琛?;

-- ========================================
-- 9. 鎻掍欢閰嶇疆琛紙鐢ㄤ簬瀛樺偍璁捐鍔╂墜鎻掍欢閰嶇疆锛?-- ========================================
CREATE TABLE IF NOT EXISTS `plugin_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `plugin_code` VARCHAR(50) NOT NULL COMMENT '鎻掍欢浠ｇ爜',
  `config_key` VARCHAR(100) NOT NULL COMMENT '閰嶇疆閿?,
  `config_value` TEXT COMMENT '閰嶇疆鍊硷紙JSON锛?,
  `description` VARCHAR(200) COMMENT '閰嶇疆璇存槑',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plugin_code (`plugin_code`),
  UNIQUE KEY uk_plugin_key (`plugin_code`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鎻掍欢閰嶇疆琛?;

-- ========================================
-- 鍒濆鍖栫瀛愭暟鎹?-- ========================================

-- 鎻掑叆AI璁捐鍔╂墜鎻掍欢
INSERT INTO `plugin` (`id`, `plugin_id`, `name`, `version`, `backend_api`, `frontend_entry`, `billing_type`, `default_token_cost`, `status`, `review_status`, `created_at`, `updated_at`)
VALUES
(3020, 'ai_design_assistant', 'AI璁捐鍔╂墜', '1.0.0', '/api/plugin/invoke/ai_design_assistant', '/design-requirement/create', 'token', 5, 1, 'pass', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
version = VALUES(version),
backend_api = VALUES(backend_api),
frontend_entry = VALUES(frontend_entry),
updated_at = NOW();

-- 涓虹ず渚嬬鎴峰惎鐢ㄦ彃浠?INSERT IGNORE INTO `tenant_plugin` (`tenant_id`, `plugin_id`, `enabled`, `config_json`)
SELECT id, 'ai_design_assistant', 1, JSON_OBJECT()
FROM `tenant`
WHERE id IN (2001, 2002);

-- 鎻掑叆榛樿鍒嗛厤瑙勫垯
INSERT INTO `task_assign_rule` (`rule_name`, `keyword`, `target_tenant_id`, `task_type`, `priority`, `enabled`)
VALUES
('鐪熶笣闈㈡枡渚涘簲鍟?, '鐪熶笣', 3001, 'fabric', 10, 1),
('缇婃瘺闈㈡枡渚涘簲鍟?, '缇婃瘺', 3002, 'fabric', 10, 1),
('妫夐夯闈㈡枡渚涘簲鍟?, '妫夐夯', 3003, 'fabric', 10, 1),
('閫氱敤鐗堝笀鏈嶅姟鍟?, '榛樿', 4001, 'pattern', 5, 1);

-- 鎻掑叆鎻掍欢閰嶇疆
INSERT INTO `plugin_config` (`plugin_code`, `config_key`, `config_value`, `description`)
VALUES
('ai_design_assistant', 'system_prompt', '浣犳槸涓€涓笓涓氱殑鏈嶈璁捐涓撳锛屽府鍔╄璁″笀瀹屽杽闇€姹傘€?, 'AI瀵硅瘽绯荤粺鎻愮ず璇?),
('ai_design_assistant', 'token_cost_per_chat', '5', '姣忔瀵硅瘽娑堣€桾oken鏁?),
('ai_design_assistant', 'token_cost_summarize', '15', '鐢熸垚鎬荤粨娑堣€桾oken鏁?),
('ai_design_assistant', 'remind_times', '09:00,14:00,18:00', '鍌姙鏃堕棿鐐?)
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

-- ========================================
-- 鍒涘缓瑙嗗浘锛堢敤浜庢煡璇紭鍖栵級
-- ========================================

-- 闇€姹傜粺璁¤鍥?DROP VIEW IF EXISTS `v_requirement_stats`;
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

-- 浠诲姟缁熻瑙嗗浘
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
-- 闈㈡枡鍟嗗叆椹荤敵璇疯〃
CREATE TABLE IF NOT EXISTS `supplier_registration` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `company_name` VARCHAR(128) NOT NULL COMMENT '鍏徃鍚嶇О',
  `contact_name` VARCHAR(64) NOT NULL COMMENT '鑱旂郴浜哄鍚?,
  `contact_mobile` VARCHAR(32) NOT NULL COMMENT '鑱旂郴浜烘墜鏈哄彿',
  `business_license` VARCHAR(500) COMMENT '钀ヤ笟鎵х収鍥剧墖URL',
  `address` VARCHAR(255) COMMENT '鍏徃鍦板潃',
  `fabric_categories` JSON COMMENT '闈㈡枡鍝佺被鏁扮粍',
  `description` TEXT COMMENT '鍏徃浠嬬粛',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `reject_reason` TEXT COMMENT '椹冲洖鍘熷洜',
  `tenant_id` BIGINT DEFAULT 0 COMMENT '瀹℃牳閫氳繃鍚庡叧鑱旂殑绉熸埛ID',
  `user_id` BIGINT DEFAULT 0 COMMENT '瀹℃牳閫氳繃鍚庡叧鑱旂殑鐢ㄦ埛ID',
  `admin_id` BIGINT DEFAULT 0 COMMENT '瀹℃牳浜篒D',
  `reviewed_at` DATETIME COMMENT '瀹℃牳鏃堕棿',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (`status`),
  INDEX idx_contact_mobile (`contact_mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闈㈡枡鍟嗗叆椹荤敵璇疯〃';

-- 闈㈡枡鍟嗗悎浣滆〃
CREATE TABLE IF NOT EXISTS `supplier_collaboration` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `merchant_tenant_id` BIGINT NOT NULL COMMENT '鍟嗗绉熸埛ID',
  `supplier_tenant_id` BIGINT NOT NULL COMMENT '闈㈡枡鍟嗙鎴稩D',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/accepted/rejected/blocked',
  `invited_by` BIGINT DEFAULT 0 COMMENT '閭€璇蜂汉user_id',
  `responded_by` BIGINT DEFAULT 0 COMMENT '鍝嶅簲浜簎ser_id',
  `responded_at` DATETIME COMMENT '鍝嶅簲鏃堕棿',
  `block_reason` TEXT COMMENT '灞忚斀鍘熷洜',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_merchant_supplier (`merchant_tenant_id`, `supplier_tenant_id`),
  INDEX idx_merchant_tenant (`merchant_tenant_id`),
  INDEX idx_supplier_tenant (`supplier_tenant_id`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闈㈡枡鍟嗗悎浣滆〃';
DROP TABLE IF EXISTS `system_config`;

CREATE TABLE `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `config_group` VARCHAR(50) NOT NULL COMMENT 'Config group: ai_image, ai_text, ai_translate, oss',
  `config_key` VARCHAR(100) NOT NULL COMMENT 'Config key: api_key, model_name, endpoint, priority, provider_name',
  `config_value` TEXT NOT NULL COMMENT 'Config value (encrypted)',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Enabled: 1=true, 0=false',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Sort order (lower is higher priority)',
  `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  KEY `idx_group_enabled` (`config_group`, `enabled`),
  KEY `idx_group_sort` (`config_group`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System config table - stores encrypted sensitive configurations';

INSERT INTO `system_config` (`config_group`, `config_key`, `config_value`, `enabled`, `sort_order`, `description`)
VALUES
  ('ai_image', 'provider_name', 'OpenAI', 1, 1, 'AI image provider name'),
  ('ai_image', 'model_name', 'dall-e-3', 1, 1, 'AI image model name'),
  ('ai_image', 'endpoint', 'https://api.openai.com/v1/images/generations', 1, 1, 'AI image API endpoint'),
  ('ai_image', 'api_key', 'ENCRYPTED:sk-mock-key-for-demo', 1, 1, 'OpenAI API Key (demo)'),
  ('ai_image', 'priority', '1', 1, 1, 'Config priority');

INSERT INTO `system_config` (`config_group`, `config_key`, `config_value`, `enabled`, `sort_order`, `description`)
VALUES
  ('ai_text', 'provider_name', 'OpenAI', 1, 1, 'AI text provider name'),
  ('ai_text', 'model_name', 'gpt-4o', 1, 1, 'AI text model name'),
  ('ai_text', 'endpoint', 'https://api.openai.com/v1/chat/completions', 1, 1, 'AI text API endpoint'),
  ('ai_text', 'api_key', 'ENCRYPTED:sk-mock-key-for-demo', 1, 1, 'OpenAI API Key (demo)'),
  ('ai_text', 'priority', '1', 1, 1, 'Config priority');

INSERT INTO `system_config` (`config_group`, `config_key`, `config_value`, `enabled`, `sort_order`, `description`)
VALUES
  ('ai_translate', 'provider_name', 'OpenAI', 1, 1, 'AI translate provider name'),
  ('ai_translate', 'model_name', 'gpt-4o-mini', 1, 1, 'AI translate model name'),
  ('ai_translate', 'endpoint', 'https://api.openai.com/v1/chat/completions', 1, 1, 'AI translate API endpoint'),
  ('ai_translate', 'api_key', 'ENCRYPTED:sk-mock-key-for-demo', 1, 1, 'OpenAI API Key (demo)'),
  ('ai_translate', 'priority', '1', 1, 1, 'Config priority');

INSERT INTO `system_config` (`config_group`, `config_key`, `config_value`, `enabled`, `sort_order`, `description`)
VALUES
  ('oss', 'provider_name', 'Aliyun', 1, 1, 'OSS provider name'),
  ('oss', 'access_key_id', 'ENCRYPTED:mock-access-key-id', 1, 1, 'OSS Access Key ID'),
  ('oss', 'access_key_secret', 'ENCRYPTED:mock-access-key-secret', 1, 1, 'OSS Access Key Secret'),
  ('oss', 'endpoint', 'oss-cn-hangzhou.aliyuncs.com', 1, 1, 'OSS endpoint'),
  ('oss', 'bucket_name', 'puyuan-maoshan', 1, 1, 'OSS Bucket name'),
  ('oss', 'region', 'cn-hangzhou', 1, 1, 'OSS region'),
  ('oss', 'priority', '1', 1, 1, 'Config priority');
-- Security Features Migration
-- 娣诲姞瀵嗙爜瀛楁鍜岀櫥褰曟棩蹇楄〃

-- 1. 娣诲姞 password 瀛楁鍒?user_account 琛?-- 娉ㄦ剰锛歁ySQL 涓嶆敮鎸?IF NOT EXISTS锛岄渶瑕佸厛妫€鏌ユ槸鍚﹀瓨鍦?SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'password');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column already exists.''',
                   'ALTER TABLE user_account ADD COLUMN password VARCHAR(255) DEFAULT NULL COMMENT ''鐧诲綍瀵嗙爜(SHA256鍔犲瘑)''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 鍒涘缓 user_login_log 琛?CREATE TABLE IF NOT EXISTS user_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓婚敭ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鐧诲綍鏃堕棿',
    login_ip VARCHAR(50) DEFAULT NULL COMMENT '鐧诲綍IP',
    device_type VARCHAR(50) DEFAULT NULL COMMENT '璁惧绫诲瀷(PC/iOS/Android/Unknown)',
    device_info VARCHAR(255) DEFAULT NULL COMMENT '璁惧璇︽儏',
    location VARCHAR(100) DEFAULT NULL COMMENT '鐧诲綍鍦扮偣',
    is_success TINYINT(1) DEFAULT 1 COMMENT '鏄惁鎴愬姛(1=鎴愬姛,0=澶辫触)',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '澶辫触鍘熷洜',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢ㄦ埛鐧诲綍鏃ュ織琛?;
-- 澶氱鎴蜂汉鍛樻ā鍨嬮噸鏋?- 鏁版嵁搴撹縼绉昏剼鏈?-- 鐗堟湰锛?.0
-- 鏃ユ湡锛?026-05-19
-- 璇存槑锛氬皢 user 琛ㄧ殑 tenant_id 鍜?tenant_role 杩佺Щ鍒?tenant_user 琛紝瀹炵幇澶氬澶氬叧绯?
-- ========================================
-- 1. 鍒涘缓 tenant_user 琛?-- ========================================

CREATE TABLE IF NOT EXISTS `tenant_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '绉熸埛ID',
  `user_id` BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
  `role` VARCHAR(20) NOT NULL COMMENT '瑙掕壊锛歜oss/designer/design_assistant/pattern_maker',
  `invited_by` BIGINT DEFAULT NULL COMMENT '閭€璇蜂汉 user_id',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_role (`role`),
  INDEX idx_status (`status`),
  INDEX idx_invited_by (`invited_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绉熸埛鐢ㄦ埛鍏宠仈琛?;

-- ========================================
-- 2. 杩佺Щ鐜版湁鏁版嵁
-- ========================================

-- 灏?user 琛ㄤ腑鐨?tenant_id 鍜?tenant_role 鏁版嵁杩佺Щ鍒?tenant_user 琛?INSERT IGNORE INTO `tenant_user` (`tenant_id`, `user_id`, `role`, `status`, `created_at`)
SELECT
    ua.tenant_id,
    ua.id AS user_id,
    'designer' AS role,
    'active' AS status,
    ua.created_at
FROM `user_account` ua
WHERE ua.tenant_id > 0
  AND NOT EXISTS (
    SELECT 1 FROM `tenant_user` tu
    WHERE tu.tenant_id = ua.tenant_id AND tu.user_id = ua.id
  );

-- ========================================
-- 3. 楠岃瘉杩佺Щ缁撴灉
-- ========================================

SELECT '杩佺Щ楠岃瘉' AS '';
SELECT
    '鐢ㄦ埛鎬绘暟' AS '缁熻椤?,
    COUNT(*) AS '鏁伴噺'
FROM `user_account`
UNION ALL
SELECT
    '鏈夌鎴风殑鐢ㄦ埛鏁?,
    COUNT(DISTINCT user_id)
FROM `tenant_user`
UNION ALL
SELECT
    '绉熸埛鐢ㄦ埛鍏宠仈璁板綍鏁?,
    COUNT(*)
FROM `tenant_user`
UNION ALL
SELECT
    '娑夊強鐨勭鎴锋暟',
    COUNT(DISTINCT tenant_id)
FROM `tenant_user`
UNION ALL
SELECT
    'boss 瑙掕壊鐢ㄦ埛鏁?,
    COUNT(DISTINCT user_id)
FROM `tenant_user`
WHERE role = 'boss';

-- ========================================
-- 4. 澶囦唤鏃ф暟鎹紙鍙€夛紝寤鸿鎵嬪姩澶囦唤瀹屾暣鏁版嵁搴擄級
-- ========================================

-- 鍒涘缓澶囦唤琛紙濡傛灉闇€瑕佸洖婊氾級
-- 娉ㄦ剰锛氱敱浜庤〃缁撴瀯鍙兘宸插彉鍖栵紝澶囦唤鍔熻兘鏆傛椂娉ㄩ噴
-- CREATE TABLE IF NOT EXISTS `user_account_backup` LIKE `user_account`;
-- INSERT INTO `user_account_backup` SELECT * FROM `user_account`;

-- ========================================
-- 5. 鍒犻櫎鏃у瓧娈碉紙璋ㄦ厧鎿嶄綔锛侊級
-- ========================================

-- 妫€鏌ュ瓧娈垫槸鍚﹀瓨鍦ㄥ悗鍐嶅垹闄?SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'tenant_role'
);

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `user_account` DROP COLUMN `tenant_role`',
    'SELECT "tenant_role 瀛楁涓嶅瓨鍦紝璺宠繃鍒犻櫎" AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tenant_id 瀛楁淇濈暀锛屼絾涓嶅啀鐢ㄤ簬鍗曚竴绉熸埛缁戝畾
-- 鏀逛负鐢ㄤ簬璁板綍鏈€鍚庣櫥褰曠殑绉熸埛锛屽彲鍚庣画浼樺寲

-- ========================================
-- 6. 鍒涘缓瑙嗗浘锛堟柟渚挎煡璇級
-- ========================================

-- 绉熸埛鐢ㄦ埛璇︽儏瑙嗗浘
CREATE OR REPLACE VIEW `v_tenant_user_detail` AS
SELECT
    tu.id AS tenant_user_id,
    tu.tenant_id,
    t.name AS tenant_name,
    tu.user_id,
    ua.mobile,
    ua.nickname,
    tu.role,
    tu.status,
    tu.invited_by,
    inviter.nickname AS inviter_name,
    tu.created_at AS joined_at
FROM `tenant_user` tu
JOIN `tenant` t ON tu.tenant_id = t.id
JOIN `user_account` ua ON tu.user_id = ua.id
LEFT JOIN `user_account` inviter ON tu.invited_by = inviter.id
WHERE tu.status = 'active';

-- 鐢ㄦ埛鎵€灞炵鎴疯鍥?CREATE OR REPLACE VIEW `v_user_tenants` AS
SELECT
    ua.id AS user_id,
    ua.mobile,
    ua.nickname,
    tu.tenant_id,
    t.name AS tenant_name,
    tu.role,
    tu.status AS membership_status
FROM `user_account` ua
JOIN `tenant_user` tu ON ua.id = tu.user_id
JOIN `tenant` t ON tu.tenant_id = t.id
WHERE tu.status = 'active';

-- ========================================
-- 7. 鏇存柊 tenant 琛紙娣诲姞 member_count 瀛楁锛?-- ========================================

-- 妫€鏌ュ苟娣诲姞 member_count 瀛楁
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
      AND TABLE_NAME = 'tenant'
      AND COLUMN_NAME = 'member_count'
);

SET @sql = IF(@col_exists > 0,
    'SELECT "member_count 瀛楁宸插瓨鍦? AS result',
    'ALTER TABLE `tenant` ADD COLUMN `member_count` INT DEFAULT 1 COMMENT "鎴愬憳鏁伴噺"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 鍒濆鍖栨垚鍛樻暟閲?UPDATE `tenant` t
SET member_count = (
    SELECT COUNT(*)
    FROM `tenant_user` tu
    WHERE tu.tenant_id = t.id AND tu.status = 'active'
);

-- ========================================
-- 8. 鍒涘缓瑙﹀彂鍣紙鑷姩鏇存柊鎴愬憳鏁伴噺锛?-- ========================================

DELIMITER //

DROP TRIGGER IF EXISTS `tr_tenant_user_insert`//

CREATE TRIGGER `tr_tenant_user_insert`
AFTER INSERT ON `tenant_user`
FOR EACH ROW
BEGIN
    IF NEW.status = 'active' THEN
        UPDATE `tenant`
        SET member_count = member_count + 1
        WHERE id = NEW.tenant_id;
    END IF;
END//

DROP TRIGGER IF EXISTS `tr_tenant_user_update`//

CREATE TRIGGER `tr_tenant_user_update`
AFTER UPDATE ON `tenant_user`
FOR EACH ROW
BEGIN
    -- 浠?inactive 鍙樹负 active
    IF NEW.status = 'active' AND OLD.status != 'active' THEN
        UPDATE `tenant` SET member_count = member_count + 1 WHERE id = NEW.tenant_id;
    END IF;
    -- 浠?active 鍙樹负 inactive 鎴?deleted
    IF OLD.status = 'active' AND NEW.status != 'active' THEN
        UPDATE `tenant` SET member_count = member_count - 1 WHERE id = OLD.tenant_id;
    END IF;
END//

DROP TRIGGER IF EXISTS `tr_tenant_user_delete`//

CREATE TRIGGER `tr_tenant_user_delete`
AFTER DELETE ON `tenant_user`
FOR EACH ROW
BEGIN
    IF OLD.status = 'active' THEN
        UPDATE `tenant` SET member_count = member_count - 1 WHERE id = OLD.tenant_id;
    END IF;
END//

DELIMITER ;

-- ========================================
-- 9. 鎻掑叆娴嬭瘯鏁版嵁锛堥獙璇佸绉熸埛鍔熻兘锛?-- ========================================

-- 鏌ユ壘宸叉湁鐨勭敤鎴峰拰绉熸埛
SET @boss_user_id = (SELECT id FROM `user_account` WHERE role_code = 'merchant_owner' LIMIT 1);
SET @designer_user_id = (SELECT id FROM `user_account` WHERE role_code = 'merchant_editor' AND id != @boss_user_id LIMIT 1);
SET @tenant_a_id = (SELECT id FROM `tenant` WHERE tenant_code = 'MERCHANT_A' LIMIT 1);
SET @tenant_b_id = (SELECT id FROM `tenant` WHERE tenant_code = 'MERCHANT_B' LIMIT 1);

-- 濡傛灉鏈夌敤鎴峰拰绉熸埛锛屾坊鍔犲绉熸埛娴嬭瘯鏁版嵁
-- 鐢ㄦ埛A (boss) 鍚屾椂鏄鎴稟鍜岀鎴稡鐨勮€佹澘
INSERT IGNORE INTO `tenant_user` (`tenant_id`, `user_id`, `role`, `status`)
VALUES
(@tenant_a_id, @boss_user_id, 'boss', 'active'),
(@tenant_b_id, @boss_user_id, 'boss', 'active');

-- 鐢ㄦ埛B (designer) 鍚屾椂鏄鎴稟鍜岀鎴稡鐨勮璁″笀
INSERT IGNORE INTO `tenant_user` (`tenant_id`, `user_id`, `role`, `invited_by`, `status`)
VALUES
(@tenant_a_id, @designer_user_id, 'designer', @boss_user_id, 'active'),
(@tenant_b_id, @designer_user_id, 'designer', @boss_user_id, 'active');

-- ========================================
-- 10. 杩佺Щ瀹屾垚妫€鏌?-- ========================================

SELECT '鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲' AS '';
SELECT '杩佺Щ瀹屾垚锛佽纭浠ヤ笅鏁版嵁锛? AS '';
SELECT '鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲' AS '';

SELECT
    '鐢ㄦ埛' AS '绫诲瀷',
    ua.id AS 'ID',
    ua.mobile AS '鎵嬫満鍙?,
    ua.nickname AS '鏄电О',
    GROUP_CONCAT(CONCAT(t.name, '(', tu.role, ')') SEPARATOR ', ') AS '鎵€灞炲伐浣滃'
FROM `user_account` ua
JOIN `tenant_user` tu ON ua.id = tu.user_id
JOIN `tenant` t ON tu.tenant_id = t.id
WHERE tu.status = 'active'
GROUP BY ua.id
ORDER BY ua.id
LIMIT 20;

-- 鏄剧ず姣忎釜宸ヤ綔瀹ょ殑鎴愬憳
SELECT
    '宸ヤ綔瀹? AS '绫诲瀷',
    t.id AS 'ID',
    t.name AS '宸ヤ綔瀹ゅ悕绉?,
    t.member_count AS '鎴愬憳鏁?,
    GROUP_CONCAT(CONCAT(ua.nickname, '(', tu.role, ')') SEPARATOR ', ') AS '鎴愬憳鍒楄〃'
FROM `tenant` t
LEFT JOIN `tenant_user` tu ON t.id = tu.tenant_id AND tu.status = 'active'
LEFT JOIN `user_account` ua ON tu.user_id = ua.id
GROUP BY t.id
ORDER BY t.id;

SELECT '鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲' AS '';
SELECT '杩佺Щ鑴氭湰鎵ц瀹屾瘯锛? AS '';
SELECT '娉ㄦ剰浜嬮」锛? AS '';
SELECT '1. 鍘?user_account 琛ㄧ殑 tenant_role 瀛楁宸插垹闄? AS '';
SELECT '2. tenant_id 瀛楁淇濈暀浣嗕笉鍐嶇敤浜庡崟涓€缁戝畾' AS '';
SELECT '3. 鎵€鏈夌鎴?鐢ㄦ埛鍏崇郴宸茶縼绉诲埌 tenant_user 琛? AS '';
SELECT '4. tenant 琛ㄦ柊澧?member_count 瀛楁鑷姩缁熻鎴愬憳鏁? AS '';
SELECT '鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲鈺愨晲' AS '';

-- 澶囦唤琛ㄤ俊鎭紙濡傞渶鍥炴粴锛屼娇鐢細INSERT INTO user_account SELECT * FROM user_account_backup锛?SELECT CONCAT('澶囦唤琛?user_account_backup 鍖呭惈 ', COUNT(*), ' 鏉¤褰?) AS '澶囦唤淇℃伅'
FROM `user_account_backup`;
-- =============================================
-- Plugin Lifecycle Management Migration
-- =============================================

-- Plugin 琛ㄦ柊澧炲瓧娈?ALTER TABLE plugin
  ADD COLUMN lifecycle_status VARCHAR(20) DEFAULT 'testing' COMMENT 'testing/enabled/disabled/gray';

ALTER TABLE plugin
  ADD COLUMN frontend_path VARCHAR(500) COMMENT '鍓嶇璧勬簮璺緞锛圕DN 鎴栫浉瀵硅矾寰勶級';

ALTER TABLE plugin
  ADD COLUMN backend_deploy_config TEXT COMMENT '鍚庣閮ㄧ讲閰嶇疆 JSON';

ALTER TABLE plugin
  ADD COLUMN gray_tenant_ids TEXT COMMENT '鐏板害鍙戝竷鐨勭鎴?ID 鍒楄〃锛岄€楀彿鍒嗛殧';

ALTER TABLE plugin
  ADD COLUMN created_by BIGINT COMMENT '鍒涘缓浜?ID';

ALTER TABLE plugin
  ADD COLUMN tested_at DATETIME COMMENT '娌欑娴嬭瘯鏃堕棿';

ALTER TABLE plugin
  ADD COLUMN published_at DATETIME COMMENT '姝ｅ紡鍙戝竷鏃堕棿';

-- 杩佺Щ鍘熸湁鐨?INT status 鍒版柊瀛楁
UPDATE plugin SET lifecycle_status = CASE WHEN status = 1 THEN 'enabled' ELSE 'disabled' END WHERE lifecycle_status IS NULL;
UPDATE plugin SET lifecycle_status = 'enabled' WHERE status = 1 AND lifecycle_status IS NULL;
UPDATE plugin SET lifecycle_status = 'disabled' WHERE status = 0 AND lifecycle_status IS NULL;

-- 娣诲姞绱㈠紩
ALTER TABLE plugin ADD INDEX idx_lifecycle_status (lifecycle_status);

-- =============================================
-- 鏂板鎻掍欢閮ㄧ讲浠诲姟琛?-- =============================================
CREATE TABLE IF NOT EXISTS plugin_deployment_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_id VARCHAR(64) NOT NULL COMMENT '鎻掍欢鍞竴鏍囪瘑',
  docker_image VARCHAR(512) COMMENT 'Docker 闀滃儚鍦板潃',
  env_vars TEXT COMMENT '鐜鍙橀噺 JSON',
  status VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/running/success/failed',
  error_message TEXT COMMENT '閿欒淇℃伅',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plugin_id (plugin_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鎻掍欢閮ㄧ讲浠诲姟琛?;
-- 瀹¤鏃ュ織琛ㄥ瓧娈垫墿灞?-- 娣诲姞 IP銆乷ld_value銆乶ew_value 瀛楁鐢ㄤ簬璁板綍绯荤粺閰嶇疆鍙樻洿

ALTER TABLE `audit_log`
ADD COLUMN `ip` VARCHAR(64) DEFAULT NULL COMMENT '鎿嶄綔鑰?IP 鍦板潃' AFTER `detail_json`,
ADD COLUMN `old_value` TEXT DEFAULT NULL COMMENT '鍙樻洿鍓嶅€硷紙鑴辨晱锛? AFTER `ip`,
ADD COLUMN `new_value` TEXT DEFAULT NULL COMMENT '鍙樻洿鍚庡€硷紙鑴辨晱锛? AFTER `old_value`,
ADD INDEX `idx_operator_id` (`operator_id`),
ADD INDEX `idx_target_type_id` (`target_type`, `target_id`),
ADD INDEX `idx_created_at` (`created_at`);
-- Seed data for MVP
-- # MEMORY: fixed IDs make cross-team debugging easier when frontend and backend compare logs in early integration.

INSERT INTO tenant (id, tenant_code, name, status, level)
VALUES
  (2001, 'maojia_001', '示例商家A', 1, 'basic'),
  (2002, 'maojia_002', '示例商家B', 1, 'vip')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  level = VALUES(level);

INSERT INTO user_account (id, tenant_id, mobile, nickname, role_code, status)
VALUES
  (1001, 2001, '13800000001', '商家老板A', 'merchant_owner', 1),
  (1002, 2001, '13800000002', '运营A', 'merchant_operator', 1),
  (1003, 2002, '13800000003', '商家老板B', 'merchant_owner', 1),
  (9001, 2001, '13900000001', '平台超管', 'platform_super_admin', 1)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  role_code = VALUES(role_code),
  status = VALUES(status);

INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status)
VALUES
  (3001, 'ai_image_gen_v1', 'AI商品图生成', '1.0.0', 'http://plugin-ai-image-svc/api/generate', '/plugins/ai-image', 'token', 500, 1, 'pass'),
  (3002, 'ai_script_gen_v1', 'AI视频脚本生成', '1.0.0', 'http://plugin-ai-script-svc/api/generate', '/plugins/ai-script', 'token', 200, 1, 'pass')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  backend_api = VALUES(backend_api),
  frontend_entry = VALUES(frontend_entry),
  billing_type = VALUES(billing_type),
  default_token_cost = VALUES(default_token_cost),
  status = VALUES(status),
  review_status = VALUES(review_status);

INSERT INTO tenant_plugin (tenant_id, plugin_id, enabled, config_json)
VALUES
  (2001, 'ai_image_gen_v1', 1, JSON_OBJECT('default_scene', 'outdoor')),
  (2001, 'ai_script_gen_v1', 1, JSON_OBJECT('default_platform', 'douyin')),
  (2002, 'ai_image_gen_v1', 1, JSON_OBJECT('default_scene', 'studio'))
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  config_json = VALUES(config_json);

INSERT INTO account_wallet (tenant_id, token_balance, cash_balance, frozen_token, status)
VALUES
  (2001, 125000, 520.00, 0, 1),
  (2002, 98000, 300.00, 0, 1)
ON DUPLICATE KEY UPDATE
  token_balance = VALUES(token_balance),
  cash_balance = VALUES(cash_balance),
  frozen_token = VALUES(frozen_token),
  status = VALUES(status);

INSERT INTO recharge_order (order_no, tenant_id, amount, token_grant, pay_channel, pay_status, paid_at)
VALUES
  ('RC20260518001', 2001, 200.00, 450000, 'wechat_pay', 'paid', NOW()),
  ('RC20260518002', 2002, 50.00, 100000, 'wechat_pay', 'paid', NOW())
ON DUPLICATE KEY UPDATE
  amount = VALUES(amount),
  token_grant = VALUES(token_grant),
  pay_status = VALUES(pay_status),
  paid_at = VALUES(paid_at);

INSERT INTO billing_ledger (tenant_id, biz_no, request_id, entry_type, direction, token_amount, cash_amount, balance_after, plugin_id, status, occurred_at)
VALUES
  (2001, 'BIZ20260518001', 'req_seed_001', 'recharge', 'in', 450000, 200.00, 575000, NULL, 'success', NOW()),
  (2001, 'BIZ20260518002', 'req_seed_002', 'debit', 'out', 450, 0.54, 574550, 'ai_image_gen_v1', 'success', NOW())
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  occurred_at = VALUES(occurred_at);

INSERT INTO billing_statement_daily (tenant_id, stat_date, token_in, token_out, call_count, amount_recharge, amount_refund)
VALUES
  (2001, CURRENT_DATE(), 450000, 450, 1, 200.00, 0.00),
  (2002, CURRENT_DATE(), 100000, 0, 0, 50.00, 0.00)
ON DUPLICATE KEY UPDATE
  token_in = VALUES(token_in),
  token_out = VALUES(token_out),
  call_count = VALUES(call_count),
  amount_recharge = VALUES(amount_recharge),
  amount_refund = VALUES(amount_refund);

INSERT INTO plugin_invoke_log (request_id, tenant_id, plugin_id, model_vendor, token_used, latency_ms, result_code, risk_level)
VALUES
  ('req_seed_invoke_001', 2001, 'ai_image_gen_v1', 'openai', 450, 2800, 0, 'low')
ON DUPLICATE KEY UPDATE
  token_used = VALUES(token_used),
  latency_ms = VALUES(latency_ms),
  result_code = VALUES(result_code),
  risk_level = VALUES(risk_level);

INSERT INTO audit_log (tenant_id, operator_id, action, target_type, target_id, detail_json)
VALUES
  (2001, 9001, 'plugin_enable', 'plugin', 'ai_script_gen_v1', JSON_OBJECT('enabled', true)),
  (2001, 9001, 'pricing_view', 'pricing', 'global', JSON_OBJECT('note', 'seed action'));
-- AI 鎻掍欢绉嶅瓙鏁版嵁
-- 鏀寔涓変釜 MVP 鎻掍欢锛氬浘鐗囩敓鎴愩€佽剼鏈敓鎴愩€佽法澧冪炕璇?
-- 鎻掑叆 AI 鍥剧墖鐢熸垚鎻掍欢
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
VALUES
  (3010, 'ai_image_gen', 'AI 鍟嗗搧鍥剧敓鎴?, '1.0.0', '/api/plugin/invoke/ai_image_gen', '/ai-tools/image-gen', 'token', 20, 1, 'pass', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  backend_api = VALUES(backend_api),
  frontend_entry = VALUES(frontend_entry),
  billing_type = VALUES(billing_type),
  default_token_cost = VALUES(default_token_cost),
  status = VALUES(status),
  review_status = VALUES(review_status),
  updated_at = NOW();

-- 鎻掑叆 AI 鑴氭湰鐢熸垚鎻掍欢
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
VALUES
  (3011, 'ai_script_gen', 'AI 瑙嗛鑴氭湰鐢熸垚', '1.0.0', '/api/plugin/invoke/ai_script_gen', '/ai-tools/script-gen', 'token', 20, 1, 'pass', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  backend_api = VALUES(backend_api),
  frontend_entry = VALUES(frontend_entry),
  billing_type = VALUES(billing_type),
  default_token_cost = VALUES(default_token_cost),
  status = VALUES(status),
  review_status = VALUES(review_status),
  updated_at = NOW();

-- 鎻掑叆 AI 璺ㄥ缈昏瘧鎻掍欢
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
VALUES
  (3012, 'ai_translate', 'AI 璺ㄥ缈昏瘧', '1.0.0', '/api/plugin/invoke/ai_translate', '/ai-tools/translate', 'token', 5, 1, 'pass', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  backend_api = VALUES(backend_api),
  frontend_entry = VALUES(frontend_entry),
  billing_type = VALUES(billing_type),
  default_token_cost = VALUES(default_token_cost),
  status = VALUES(status),
  review_status = VALUES(review_status),
  updated_at = NOW();

-- 涓虹鎴峰惎鐢ㄨ繖浜涙彃浠讹紙绀轰緥绉熸埛 2001, 2002锛?INSERT INTO tenant_plugin (tenant_id, plugin_id, enabled, config_json)
VALUES
  (2001, 'ai_image_gen', 1, JSON_OBJECT()),
  (2001, 'ai_script_gen', 1, JSON_OBJECT()),
  (2001, 'ai_translate', 1, JSON_OBJECT()),
  (2002, 'ai_image_gen', 1, JSON_OBJECT()),
  (2002, 'ai_script_gen', 1, JSON_OBJECT()),
  (2002, 'ai_translate', 1, JSON_OBJECT())
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  config_json = VALUES(config_json);

-- ========================================
-- V1.2 迁移：用户资料扩展 + 角色审计 + 角色配置
-- ========================================

CREATE TABLE IF NOT EXISTS `member_role_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `member_user_id` BIGINT NOT NULL COMMENT '成员用户ID',
    `operator_user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `action` VARCHAR(20) NOT NULL COMMENT '操作类型',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员角色变更审计日志表';

CREATE TABLE IF NOT EXISTS `tenant_role_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) NULL COMMENT '角色描述',
    `permissions` JSON NULL COMMENT '权限列表',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色配置表';

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

UPDATE `user_account` SET `phone` = `mobile` WHERE `phone` IS NULL AND `mobile` IS NOT NULL;
