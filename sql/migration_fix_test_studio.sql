-- ============================================================
-- 迁移脚本：修复"测试工作室"不可见 & AI设计助手入口缺失
-- 适用场景：已有数据库，无需完整重置
-- 执行方式：mysql -u root -p <database> < migration_fix_test_studio.sql
-- ============================================================

-- 1. 添加"测试工作室"租户
INSERT INTO tenant (id, tenant_code, name, status, level, tenant_type)
VALUES (2003, 'test_studio_001', '测试工作室', 1, 'basic', 'standard')
ON DUPLICATE KEY UPDATE
  name = VALUES(name), status = VALUES(status);

-- 2. 将 13800000003 (user_id=1003) 加入测试工作室（角色：设计师）
INSERT INTO tenant_user (tenant_id, user_id, role, status)
VALUES (2003, 1003, 'designer', 'active')
ON DUPLICATE KEY UPDATE role = VALUES(role), status = VALUES(status);

-- 3. 为测试工作室创建钱包
INSERT INTO account_wallet (tenant_id, token_balance, cash_balance, frozen_token, status)
VALUES (2003, 100, 0.00, 0, 1)
ON DUPLICATE KEY UPDATE
  token_balance = VALUES(token_balance), cash_balance = VALUES(cash_balance),
  frozen_token = VALUES(frozen_token), status = VALUES(status);

-- 4. 为所有租户启用 AI 设计助手插件（关键修复！）
INSERT INTO tenant_plugin (tenant_id, plugin_id, enabled, config_json)
VALUES
  (2001, 'ai_design_assistant', 1, JSON_OBJECT()),
  (2002, 'ai_design_assistant', 1, JSON_OBJECT()),
  (2003, 'ai_design_assistant', 1, JSON_OBJECT())
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), config_json = VALUES(config_json);

-- 5. 同步更新 member_count
UPDATE tenant t SET member_count = (
    SELECT COUNT(*) FROM tenant_user tu
    WHERE tu.tenant_id = t.id AND tu.status = 'active'
);

-- 验证结果
SELECT t.id, t.name, t.member_count
FROM tenant t
WHERE t.id IN (2001, 2002, 2003);

SELECT tu.tenant_id, t.name AS tenant_name, tu.user_id, ua.mobile, tu.role, tu.status
FROM tenant_user tu
JOIN tenant t ON tu.tenant_id = t.id
JOIN user_account ua ON tu.user_id = ua.id
WHERE ua.mobile IN ('13800000001', '13800000003')
ORDER BY tu.tenant_id;

SELECT tp.tenant_id, t.name, tp.plugin_id, tp.enabled
FROM tenant_plugin tp
JOIN tenant t ON tp.tenant_id = t.id
WHERE tp.plugin_id = 'ai_design_assistant';
