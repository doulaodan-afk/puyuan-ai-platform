-- ============================================================
-- 迁移 v3：多 API Key 轮询 + 租户分级限流
-- 执行前请确认当前环境已备份
-- ============================================================

-- 1. ai_provider 表增加多 Key 支持字段
ALTER TABLE ai_provider
    ADD COLUMN IF NOT EXISTS api_keys TEXT COMMENT '多 API Key（JSON 数组，如 ["sk-xxx1","sk-xxx2"]），优先于 api_key',
    ADD COLUMN IF NOT EXISTS key_strategy VARCHAR(32) DEFAULT 'round_robin' COMMENT '多 Key 轮询策略：round_robin/least_loaded/random',
    ADD COLUMN IF NOT EXISTS key_max_rpm INT DEFAULT 0 COMMENT '每个 Key 的最大 RPM，0 表示不限制';

-- 2. 新增 ai_provider_key_usage 表，记录每个 Key 的调用统计（用于 least_loaded 策略）
CREATE TABLE IF NOT EXISTS ai_provider_key_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL COMMENT '提供商 ID',
    key_index INT NOT NULL COMMENT 'Key 在 api_keys 数组中的索引（从 0 开始）',
    key_hash VARCHAR(64) NOT NULL COMMENT 'api_key 的 SHA-256 哈希（前 16 位，用于标识而不泄露 Key）',
    last_minute_requests INT DEFAULT 0 COMMENT '最近一分钟的请求数',
    total_requests BIGINT DEFAULT 0 COMMENT '总请求数',
    total_failures BIGINT DEFAULT 0 COMMENT '总失败次数',
    last_fail_time DATETIME COMMENT '最近一次失败时间',
    cooldown_until DATETIME COMMENT '冷却截止时间（Key 被暂时禁用后）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_provider_key (provider_id, key_index),
    INDEX idx_cooldown (cooldown_until)
) COMMENT 'AI 提供商 Key 使用统计表';

-- 3. 新增 tenant_rate_limit_config 表，支持租户分级限流
CREATE TABLE IF NOT EXISTS tenant_rate_limit_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户 ID（0 表示默认配置）',
    plan_type VARCHAR(32) DEFAULT 'default' COMMENT '套餐类型：default/premium/enterprise',
    max_rpm INT NOT NULL DEFAULT 10 COMMENT '每分钟最大请求数',
    burst_multiplier DECIMAL(3,1) DEFAULT 2.0 COMMENT '突发系数（允许短时超过 max_rpm 的倍数）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant (tenant_id),
    INDEX idx_plan_type (plan_type)
) COMMENT '租户限流配置表';

-- 4. 插入默认租户限流配置
INSERT IGNORE INTO tenant_rate_limit_config (tenant_id, plan_type, max_rpm, burst_multiplier, enabled)
VALUES
    (0, 'default', 10, 2.0, 1),
    (0, 'premium', 100, 3.0, 1);

-- 5. 新增 system_config 配置项：多 Key 轮询全局开关
INSERT IGNORE INTO system_config (config_group, config_key, config_value, enabled, sort_order, description)
VALUES
    ('ai_provider', 'multi_key_enabled', 'true', 1, 10, '是否启用多 Key 轮询功能'),
    ('ai_provider', 'key_cooldown_seconds', '60', 1, 11, 'Key 限流后的冷却时间（秒）'),
    ('ai_provider', 'max_retry_attempts', '3', 1, 12, 'AI 调用最大重试次数'),
    ('rate_limit', 'tenant_default_rpm', '10', 1, 0, '租户默认每分钟请求数'),
    ('rate_limit', 'tenant_premium_rpm', '100', 1, 1, '付费租户每分钟请求数'),
    ('rate_limit', 'tenant_burst_multiplier', '2.0', 1, 2, '租户突发系数');
