-- Puyuan AI Platform - 初始化脚本 (MySQL 8)
-- 用途: Docker 部署时自动建表 + 种子数据
-- 更新日期: 2026-06-04
-- 说明: 建表语句与 backend/sql/schema_mvp.sql 保持一致，本文件额外包含种子数据和视图

-- ============================================================
-- 建表（与 schema_mvp.sql 完全一致）
-- ============================================================

SOURCE schema_mvp.sql;

-- ============================================================
-- 种子数据
-- ============================================================

-- 租户
INSERT INTO tenant (id, tenant_code, name, status, level, tenant_type)
VALUES
  (2001, 'maojia_001', '示例商家A', 1, 'basic', 'standard'),
  (2002, 'maojia_002', '示例商家B', 1, 'vip', 'standard')
ON DUPLICATE KEY UPDATE
  name = VALUES(name), status = VALUES(status), level = VALUES(level), tenant_type = VALUES(tenant_type);

-- 用户
INSERT INTO user_account (id, tenant_id, mobile, nickname, role_code, status)
VALUES
  (1001, 2001, '13800000001', '商家老板A', 'merchant_owner', 1),
  (1002, 2001, '13800000002', '运营A', 'merchant_operator', 1),
  (1003, 2002, '13800000003', '商家老板B', 'merchant_owner', 1),
  (9001, 2001, '13900000001', '平台超管', 'platform_super_admin', 1)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname), role_code = VALUES(role_code), status = VALUES(status);

-- 租户-用户关联
INSERT INTO tenant_user (tenant_id, user_id, role, status)
VALUES
  (2001, 1001, 'boss', 'active'),
  (2001, 1002, 'operator', 'active'),
  (2002, 1003, 'boss', 'active')
ON DUPLICATE KEY UPDATE role = VALUES(role), status = VALUES(status);

-- 角色配置
INSERT INTO tenant_role_config (role_code, role_name, description, permissions, sort_order, is_system)
VALUES
  ('boss', 'Boss', 'Studio owner with all permissions', '["*"]', 1, 1),
  ('tenant_admin', 'Admin', 'Can manage members and access all features', '["member_manage","plugin_invoke","billing_view","settings_view"]', 2, 1),
  ('tenant_operator', 'Operator', 'Can invoke AI plugins and view billing', '["plugin_invoke","billing_view"]', 3, 1),
  ('tenant_viewer', 'Viewer', 'Read-only access', '["billing_view"]', 4, 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name), description = VALUES(description), permissions = VALUES(permissions), sort_order = VALUES(sort_order);

-- 插件
INSERT INTO plugin (id, plugin_id, name, version, backend_api, frontend_entry, billing_type, default_token_cost, status, review_status)
VALUES
  (3001, 'ai_image_gen_v1', 'AI商品图生成', '1.0.0', 'http://plugin-ai-image-svc/api/generate', '/plugins/ai-image', 'token', 500, 1, 'pass'),
  (3002, 'ai_script_gen_v1', 'AI视频脚本生成', '1.0.0', 'http://plugin-ai-script-svc/api/generate', '/plugins/ai-script', 'token', 200, 1, 'pass'),
  (3010, 'ai_image_gen', 'AI商品图生成', '1.0.0', '/api/plugin/invoke/ai_image_gen', '/ai-tools/image-gen', 'token', 20, 1, 'pass'),
  (3011, 'ai_script_gen', 'AI视频脚本生成', '1.0.0', '/api/plugin/invoke/ai_script_gen', '/ai-tools/script-gen', 'token', 20, 1, 'pass'),
  (3012, 'ai_translate', 'AI跨境翻译', '1.0.0', '/api/plugin/invoke/ai_translate', '/ai-tools/translate', 'token', 5, 1, 'pass'),
  (3020, 'ai_design_assistant', 'AI设计助手', '1.0.0', '/api/plugin/invoke/ai_design_assistant', '/design-requirement/create', 'token', 5, 1, 'pass')
ON DUPLICATE KEY UPDATE
  name = VALUES(name), version = VALUES(version), backend_api = VALUES(backend_api),
  frontend_entry = VALUES(frontend_entry), billing_type = VALUES(billing_type),
  default_token_cost = VALUES(default_token_cost), status = VALUES(status),
  review_status = VALUES(review_status), updated_at = NOW();

-- 租户插件启用
INSERT INTO tenant_plugin (tenant_id, plugin_id, enabled, config_json)
VALUES
  (2001, 'ai_image_gen_v1', 1, JSON_OBJECT('default_scene', 'outdoor')),
  (2001, 'ai_script_gen_v1', 1, JSON_OBJECT('default_platform', 'douyin')),
  (2002, 'ai_image_gen_v1', 1, JSON_OBJECT('default_scene', 'studio')),
  (2001, 'ai_image_gen', 1, JSON_OBJECT()),
  (2001, 'ai_script_gen', 1, JSON_OBJECT()),
  (2001, 'ai_translate', 1, JSON_OBJECT()),
  (2002, 'ai_image_gen', 1, JSON_OBJECT()),
  (2002, 'ai_script_gen', 1, JSON_OBJECT()),
  (2002, 'ai_translate', 1, JSON_OBJECT())
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), config_json = VALUES(config_json);

-- 钱包
INSERT INTO account_wallet (tenant_id, token_balance, cash_balance, frozen_token, status)
VALUES
  (2001, 125000, 520.00, 0, 1),
  (2002, 98000, 300.00, 0, 1)
ON DUPLICATE KEY UPDATE
  token_balance = VALUES(token_balance), cash_balance = VALUES(cash_balance),
  frozen_token = VALUES(frozen_token), status = VALUES(status);

-- 充值订单
INSERT INTO recharge_order (order_no, tenant_id, amount, token_grant, pay_channel, pay_status, paid_at)
VALUES
  ('RC20260518001', 2001, 200.00, 450000, 'wechat_pay', 'paid', NOW()),
  ('RC20260518002', 2002, 50.00, 100000, 'wechat_pay', 'paid', NOW())
ON DUPLICATE KEY UPDATE
  amount = VALUES(amount), token_grant = VALUES(token_grant),
  pay_status = VALUES(pay_status), paid_at = VALUES(paid_at);

-- 计费流水
INSERT INTO billing_ledger (tenant_id, biz_no, request_id, entry_type, direction, token_amount, cash_amount, balance_after, plugin_id, status, occurred_at)
VALUES
  (2001, 'BIZ20260518001', 'req_seed_001', 'recharge', 'in', 450000, 200.00, 575000, NULL, 'success', NOW()),
  (2001, 'BIZ20260518002', 'req_seed_002', 'debit', 'out', 450, 0.54, 574550, 'ai_image_gen_v1', 'success', NOW())
ON DUPLICATE KEY UPDATE
  status = VALUES(status), occurred_at = VALUES(occurred_at);

-- 日账单
INSERT INTO billing_statement_daily (tenant_id, stat_date, token_in, token_out, call_count, amount_recharge, amount_refund)
VALUES
  (2001, CURRENT_DATE(), 450000, 450, 1, 200.00, 0.00),
  (2002, CURRENT_DATE(), 100000, 0, 0, 50.00, 0.00)
ON DUPLICATE KEY UPDATE
  token_in = VALUES(token_in), token_out = VALUES(token_out),
  call_count = VALUES(call_count), amount_recharge = VALUES(amount_recharge),
  amount_refund = VALUES(amount_refund);

-- 插件调用日志
INSERT INTO plugin_invoke_log (request_id, tenant_id, plugin_id, model_vendor, token_used, latency_ms, result_code, risk_level)
VALUES
  ('req_seed_invoke_001', 2001, 'ai_image_gen_v1', 'openai', 450, 2800, 0, 'low')
ON DUPLICATE KEY UPDATE
  token_used = VALUES(token_used), latency_ms = VALUES(latency_ms),
  result_code = VALUES(result_code), risk_level = VALUES(risk_level);

-- 审计日志
INSERT INTO audit_log (tenant_id, operator_id, action, target_type, target_id, detail_json)
VALUES
  (2001, 9001, 'plugin_enable', 'plugin', 'ai_script_gen_v1', JSON_OBJECT('enabled', true)),
  (2001, 9001, 'pricing_view', 'pricing', 'global', JSON_OBJECT('note', 'seed action'));

-- 系统配置
INSERT INTO system_config (config_group, config_key, config_value, enabled, sort_order, description)
VALUES
  ('ai_image', 'provider_name', 'OpenAI', 1, 1, 'AI image provider name'),
  ('ai_image', 'model_name', 'dall-e-3', 1, 1, 'AI image model name'),
  ('ai_image', 'endpoint', 'https://api.openai.com/v1/images/generations', 1, 1, 'AI image API endpoint'),
  ('ai_image', 'api_key', 'ENCRYPTED:sk-mock-key-for-demo', 1, 1, 'OpenAI API Key (demo)'),
  ('ai_image', 'priority', '1', 1, 1, 'Config priority'),
  ('ai_text', 'provider_name', 'OpenAI', 1, 1, 'AI text provider name'),
  ('ai_text', 'model_name', 'gpt-4o', 1, 1, 'AI text model name'),
  ('ai_text', 'endpoint', 'https://api.openai.com/v1/chat/completions', 1, 1, 'AI text API endpoint'),
  ('ai_text', 'api_key', 'ENCRYPTED:sk-mock-key-for-demo', 1, 1, 'OpenAI API Key (demo)'),
  ('ai_text', 'priority', '1', 1, 1, 'Config priority'),
  ('ai_translate', 'provider_name', 'OpenAI', 1, 1, 'AI translate provider name'),
  ('ai_translate', 'model_name', 'gpt-4o-mini', 1, 1, 'AI translate model name'),
  ('ai_translate', 'endpoint', 'https://api.openai.com/v1/chat/completions', 1, 1, 'AI translate API endpoint'),
  ('ai_translate', 'api_key', 'ENCRYPTED:sk-mock-key-for-demo', 1, 1, 'OpenAI API Key (demo)'),
  ('ai_translate', 'priority', '1', 1, 1, 'Config priority'),
  ('oss', 'provider_name', 'Aliyun', 1, 1, 'OSS provider name'),
  ('oss', 'access_key_id', 'ENCRYPTED:mock-access-key-id', 1, 1, 'OSS Access Key ID'),
  ('oss', 'access_key_secret', 'ENCRYPTED:mock-access-key-secret', 1, 1, 'OSS Access Key Secret'),
  ('oss', 'endpoint', 'oss-cn-hangzhou.aliyuncs.com', 1, 1, 'OSS endpoint'),
  ('oss', 'bucket_name', 'puyuan-maoshan', 1, 1, 'OSS Bucket name'),
  ('oss', 'region', 'cn-hangzhou', 1, 1, 'OSS region'),
  ('oss', 'priority', '1', 1, 1, 'Config priority')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 插件配置
INSERT INTO plugin_config (plugin_code, config_key, config_value, description)
VALUES
  ('ai_design_assistant', 'system_prompt', '你是一个专业的服装设计专家，帮助设计师完善需求。', 'AI对话系统提示语'),
  ('ai_design_assistant', 'token_cost_per_chat', '5', '每次对话消耗Token数'),
  ('ai_design_assistant', 'token_cost_summarize', '15', '生成总结消耗Token数'),
  ('ai_design_assistant', 'remind_times', '09:00,14:00,18:00', '催办时间点')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 任务分配规则
INSERT INTO task_assign_rule (rule_name, keyword, target_tenant_id, task_type, priority, enabled)
VALUES
  ('真丝面料供应商', '真丝', 3001, 'fabric', 10, 1),
  ('羊毛面料供应商', '羊毛', 3002, 'fabric', 10, 1),
  ('棉麻面料供应商', '棉麻', 3003, 'fabric', 10, 1),
  ('通用版型服务商', '默认', 4001, 'pattern', 5, 1);

-- ============================================================
-- 视图（方便查询优化）
-- ============================================================

CREATE OR REPLACE VIEW v_requirement_stats AS
SELECT
    tenant_id,
    COUNT(*) as total,
    SUM(CASE WHEN status = 'draft' THEN 1 ELSE 0 END) as draft_count,
    SUM(CASE WHEN status = 'assistant_processing' THEN 1 ELSE 0 END) as processing_count,
    SUM(CASE WHEN status = 'released' THEN 1 ELSE 0 END) as released_count,
    SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed_count,
    SUM(total_token_cost) as total_tokens
FROM design_requirement
GROUP BY tenant_id;

CREATE OR REPLACE VIEW v_task_stats AS
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
FROM design_task
GROUP BY assignee_id, task_type;

CREATE OR REPLACE VIEW v_tenant_user_detail AS
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
FROM tenant_user tu
JOIN tenant t ON tu.tenant_id = t.id
JOIN user_account ua ON tu.user_id = ua.id
LEFT JOIN user_account inviter ON tu.invited_by = inviter.id
WHERE tu.status = 'active';

CREATE OR REPLACE VIEW v_user_tenants AS
SELECT
    ua.id AS user_id,
    ua.mobile,
    ua.nickname,
    tu.tenant_id,
    t.name AS tenant_name,
    tu.role,
    tu.status AS membership_status
FROM user_account ua
JOIN tenant_user tu ON ua.id = tu.user_id
JOIN tenant t ON tu.tenant_id = t.id
WHERE tu.status = 'active';

-- ============================================================
-- 触发器（自动更新 tenant.member_count）
-- ============================================================

DELIMITER //

DROP TRIGGER IF EXISTS tr_tenant_user_insert//
CREATE TRIGGER tr_tenant_user_insert
AFTER INSERT ON tenant_user
FOR EACH ROW
BEGIN
    IF NEW.status = 'active' THEN
        UPDATE tenant SET member_count = member_count + 1 WHERE id = NEW.tenant_id;
    END IF;
END//

DROP TRIGGER IF EXISTS tr_tenant_user_update//
CREATE TRIGGER tr_tenant_user_update
AFTER UPDATE ON tenant_user
FOR EACH ROW
BEGIN
    IF NEW.status = 'active' AND OLD.status != 'active' THEN
        UPDATE tenant SET member_count = member_count + 1 WHERE id = NEW.tenant_id;
    END IF;
    IF OLD.status = 'active' AND NEW.status != 'active' THEN
        UPDATE tenant SET member_count = member_count - 1 WHERE id = OLD.tenant_id;
    END IF;
END//

DROP TRIGGER IF EXISTS tr_tenant_user_delete//
CREATE TRIGGER tr_tenant_user_delete
AFTER DELETE ON tenant_user
FOR EACH ROW
BEGIN
    IF OLD.status = 'active' THEN
        UPDATE tenant SET member_count = member_count - 1 WHERE id = OLD.tenant_id;
    END IF;
END//

DELIMITER ;

-- 初始化 member_count
UPDATE tenant t
SET member_count = (
    SELECT COUNT(*)
    FROM tenant_user tu
    WHERE tu.tenant_id = t.id AND tu.status = 'active'
);

-- 同步 phone = mobile
UPDATE user_account SET phone = mobile WHERE phone IS NULL AND mobile IS NOT NULL;