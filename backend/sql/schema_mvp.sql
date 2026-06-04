-- Puyuan AI Platform MVP schema (MySQL 8)
-- 全量建表脚本，与 Java 实体类保持一致
-- 更新日期: 2026-06-04

-- ============================================================
-- 核心租户与用户
-- ============================================================

CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  level VARCHAR(32) DEFAULT 'basic',
  tenant_type VARCHAR(32) DEFAULT 'standard',
  member_count INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  mobile VARCHAR(32) NOT NULL,
  nickname VARCHAR(64),
  avatar_url VARCHAR(500),
  phone VARCHAR(20),
  wechat_openid VARCHAR(100),
  wechat_unionid VARCHAR(100),
  email VARCHAR(100),
  password VARCHAR(255),
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

CREATE TABLE IF NOT EXISTS tenant_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  invited_by BIGINT,
  status VARCHAR(20) DEFAULT 'active',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_user (tenant_id, user_id),
  INDEX idx_user_id (user_id),
  INDEX idx_tenant_id (tenant_id),
  INDEX idx_role (role),
  INDEX idx_status (status),
  INDEX idx_invited_by (invited_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_role_config (
  role_code VARCHAR(32) PRIMARY KEY,
  role_name VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  permissions JSON,
  sort_order INT DEFAULT 0,
  is_system TINYINT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member_role_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  member_user_id BIGINT NOT NULL,
  operator_user_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,
  old_role VARCHAR(20),
  new_role VARCHAR(20),
  old_status VARCHAR(20),
  new_status VARCHAR(20),
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tenant_id (tenant_id),
  INDEX idx_member_user_id (member_user_id),
  INDEX idx_operator_user_id (operator_user_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  login_time DATETIME NOT NULL,
  login_ip VARCHAR(64),
  device_type VARCHAR(32),
  device_info VARCHAR(255),
  location VARCHAR(128),
  is_success TINYINT NOT NULL DEFAULT 1,
  fail_reason VARCHAR(255),
  INDEX idx_login_user (user_id),
  INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_assistant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  assistant_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ta_tenant (tenant_id),
  INDEX idx_ta_user (assistant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 插件系统
-- ============================================================

CREATE TABLE IF NOT EXISTS plugin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_id VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  version VARCHAR(32) NOT NULL,
  backend_api VARCHAR(255) NOT NULL,
  frontend_entry VARCHAR(255),
  description VARCHAR(500),
  icon_url VARCHAR(500),
  frontend_path VARCHAR(255),
  lifecycle_status VARCHAR(32) DEFAULT 'development',
  created_by BIGINT,
  tested_at DATETIME,
  published_at DATETIME,
  gray_tenant_ids VARCHAR(1000),
  backend_deploy_config TEXT,
  billing_type VARCHAR(16) NOT NULL,
  default_token_cost INT DEFAULT 0,
  ai_model VARCHAR(64),
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
  config_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_plugin (tenant_id, plugin_id),
  INDEX idx_tenant_plugin_enabled (enabled),
  CONSTRAINT fk_tenant_plugin_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  CONSTRAINT fk_tenant_plugin_plugin FOREIGN KEY (plugin_id) REFERENCES plugin(plugin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plugin_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_code VARCHAR(64) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value TEXT,
  description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plugin_config (plugin_code, config_key),
  INDEX idx_plugin_code (plugin_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plugin_deployment_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_id VARCHAR(64) NOT NULL,
  docker_image VARCHAR(255),
  env_vars TEXT,
  status VARCHAR(32) DEFAULT 'pending',
  error_message TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_pdt_plugin (plugin_id),
  INDEX idx_pdt_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- AI 服务
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_provider (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(128),
  base_url VARCHAR(255),
  api_key VARCHAR(255),
  api_keys TEXT,
  key_strategy VARCHAR(32) DEFAULT 'round_robin',
  key_max_rpm INT DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  priority INT DEFAULT 0,
  description VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_scene (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL UNIQUE,
  scene_name VARCHAR(128) NOT NULL,
  api_type VARCHAR(32),
  scene_description VARCHAR(500),
  recommendation_prompt TEXT,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_scene_model (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  model_id VARCHAR(64) NOT NULL,
  is_primary TINYINT NOT NULL DEFAULT 0,
  is_fallback TINYINT NOT NULL DEFAULT 0,
  priority INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_asm_scene (scene_id),
  INDEX idx_asm_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL UNIQUE,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  requirement_id BIGINT,
  context TEXT,
  expires_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ai_session_tenant (tenant_id),
  INDEX idx_ai_session_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 计费与钱包
-- ============================================================

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
  paid_at DATETIME,
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

-- ============================================================
-- 设计需求与任务
-- ============================================================

CREATE TABLE IF NOT EXISTS design_requirement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  creator_id BIGINT NOT NULL,
  title VARCHAR(255),
  raw_images TEXT,
  raw_videos TEXT,
  raw_audio_url VARCHAR(500),
  raw_text TEXT,
  conversation_history TEXT,
  ai_summary TEXT,
  designer_approved INT DEFAULT 0,
  assistant_id BIGINT,
  status VARCHAR(32) DEFAULT 'draft',
  total_token_cost INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_dr_tenant (tenant_id),
  INDEX idx_dr_creator (creator_id),
  INDEX idx_dr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS design_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  requirement_id BIGINT NOT NULL,
  task_type VARCHAR(32),
  assignee_type VARCHAR(32),
  assignee_id BIGINT,
  content TEXT,
  status VARCHAR(32) DEFAULT 'draft',
  deadline DATETIME,
  result_url VARCHAR(500),
  fabric_task_id BIGINT,
  logistics_company VARCHAR(64),
  logistics_tracking_no VARCHAR(64),
  logistics_status VARCHAR(32) DEFAULT 'pending',
  offline_logistics_note VARCHAR(500),
  shipped_at DATETIME,
  delivered_at DATETIME,
  notified_at DATETIME,
  reject_reason VARCHAR(500),
  completed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_dt_requirement (requirement_id),
  INDEX idx_dt_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 面料库与供应商
-- ============================================================

CREATE TABLE IF NOT EXISTS fabric_library (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  supplier_tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64),
  images TEXT,
  video_url VARCHAR(500),
  specs TEXT,
  price_per_meter DECIMAL(18,2) DEFAULT 0,
  stock_status VARCHAR(32) DEFAULT 'in_stock',
  is_visible INT DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_fl_supplier (supplier_tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS supplier_registration (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64),
  contact_mobile VARCHAR(32),
  business_license VARCHAR(255),
  address VARCHAR(255),
  fabric_categories TEXT,
  description VARCHAR(500),
  status VARCHAR(32) DEFAULT 'pending',
  reject_reason VARCHAR(500),
  tenant_id BIGINT,
  user_id BIGINT,
  admin_id BIGINT,
  reviewed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sr_status (status),
  INDEX idx_sr_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS supplier_collaboration (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_tenant_id BIGINT NOT NULL,
  supplier_tenant_id BIGINT NOT NULL,
  status VARCHAR(32) DEFAULT 'pending',
  invited_by BIGINT,
  responded_by BIGINT,
  responded_at DATETIME,
  block_reason VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sc_merchant (merchant_tenant_id),
  INDEX idx_sc_supplier (supplier_tenant_id),
  INDEX idx_sc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 任务分配与提醒
-- ============================================================

CREATE TABLE IF NOT EXISTS task_assign_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_name VARCHAR(64) NOT NULL,
  keyword VARCHAR(128),
  target_tenant_id BIGINT,
  task_type VARCHAR(32),
  priority INT DEFAULT 0,
  enabled INT DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_remind_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  remind_time DATETIME NOT NULL,
  remind_channel VARCHAR(32) DEFAULT 'internal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_trl_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 系统配置与消息
-- ============================================================

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_group VARCHAR(64) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value TEXT,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT DEFAULT 0,
  description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_system_config (config_group, config_key),
  INDEX idx_sc_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_rate_limit_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plan_type VARCHAR(32) DEFAULT 'default',
  max_rpm INT DEFAULT 0,
  burst_multiplier DECIMAL(5,2) DEFAULT 1.00,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_rlc_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receiver_id BIGINT NOT NULL,
  sender_id BIGINT,
  title VARCHAR(128),
  content TEXT,
  type VARCHAR(32) DEFAULT 'system',
  is_read INT DEFAULT 0,
  related_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_msg_receiver (receiver_id),
  INDEX idx_msg_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 审计日志
-- ============================================================

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
