-- =====================================================
-- 全局 AI 提供商配置迁移脚本
-- 将 AI 配置从 application.yml 迁移到 system_config 表
-- 实现动态生效，无需重启
-- =====================================================

-- 插入 AI 提供商全局配置
INSERT INTO system_config (config_group, config_key, config_value, enabled, sort_order, description)
VALUES ('ai_provider', 'base_url', 'https://api.siliconflow.cn/v1', 1, 0, 'AI 提供商 API 地址')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO system_config (config_group, config_key, config_value, enabled, sort_order, description)
VALUES ('ai_provider', 'api_key', '', 1, 1, 'AI 提供商 API Key（加密存储）')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO system_config (config_group, config_key, config_value, enabled, sort_order, description)
VALUES ('ai_provider', 'default_model', 'deepseek-ai/DeepSeek-V3', 1, 2, '全局默认 AI 模型')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
