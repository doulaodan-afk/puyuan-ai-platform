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
