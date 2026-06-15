-- AI 插件种子数据
-- 支持三个 MVP 插件：图片生成、脚本生成、跨境翻译

-- 插入 AI 图片生成插件
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
VALUES
  (3010, 'ai_image_gen', 'AI 商品图生成', '1.0.0', '/api/plugin/invoke/ai_image_gen', '/ai-tools/image-gen', 'token', 10, 1, 'pass', NOW(), NOW())
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

-- 插入 AI 脚本生成插件
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
VALUES
  (3011, 'ai_script_gen', 'AI 视频脚本生成', '1.0.0', '/api/plugin/invoke/ai_script_gen', '/ai-tools/script-gen', 'token', 10, 1, 'pass', NOW(), NOW())
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

-- 插入 AI 跨境翻译插件
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status, created_at, updated_at)
VALUES
  (3012, 'ai_translate', 'AI 跨境翻译', '1.0.0', '/api/plugin/invoke/ai_translate', '/ai-tools/translate', 'token', 3, 1, 'pass', NOW(), NOW())
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

-- 为租户启用这些插件（示例租户 2001, 2002）
INSERT INTO tenant_plugin (tenant_id, plugin_id, enabled, config_json)
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