-- Puyuan AI Platform MVP schema (MySQL 8)
-- # MEMORY: this schema prioritizes billing idempotency and tenant isolation, because charge disputes are the highest business risk in MVP.

CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  level VARCHAR(32) DEFAULT 'basic',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  mobile VARCHAR(32) NOT NULL,
  nickname VARCHAR(64),
  role_code VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_tenant (tenant_id),
  INDEX idx_user_role (role_code),
  INDEX idx_user_mobile (mobile),
  CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plugin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_id VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  version VARCHAR(32) NOT NULL,
  backend_api VARCHAR(255) NOT NULL,
  frontend_entry VARCHAR(255),
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
