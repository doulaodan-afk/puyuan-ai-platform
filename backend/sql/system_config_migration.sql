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
