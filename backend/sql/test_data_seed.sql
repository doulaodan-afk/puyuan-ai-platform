-- AI设计助手插件 - 测试数据种子脚本
-- 版本：1.0
-- 日期：2026-05-19
-- 说明：创建测试用户、租户和分配规则

USE puyuan_ai_mvp;

-- ========================================
-- 1. 创建测试租户
-- ========================================

-- 商家租户
INSERT IGNORE INTO `tenant` (`id`, `tenant_code`, `name`, `status`, `level`, `tenant_type`, `parent_tenant_id`) VALUES
(2001, 'MERCHANT_A', '测试商家A', 1, 'basic', 'normal', 0),
(2002, 'MERCHANT_B', '测试商家B', 1, 'basic', 'normal', 0);

-- 面料商租户
INSERT IGNORE INTO `tenant` (`id`, `tenant_code`, `name`, `status`, `level`, `tenant_type`, `parent_tenant_id`) VALUES
(3001, 'FABRIC_A', '测试面料商A', 1, 'basic', 'supplier', 0),
(3002, 'FABRIC_B', '测试面料商B', 1, 'basic', 'supplier', 0);

-- 版师服务商租户
INSERT IGNORE INTO `tenant` (`id`, `tenant_code`, `name`, `status`, `level`, `tenant_type`, `parent_tenant_id`) VALUES
(4001, 'PATTERN_A', '测试版师服务商A', 1, 'basic', 'pattern_service', 0);

-- ========================================
-- 2. 创建测试用户
-- ========================================

-- 商家A用户
INSERT IGNORE INTO `user_account` (`id`, `tenant_id`, `mobile`, `nickname`, `role_code`, `status`) VALUES
(101, 2001, '13800101001', '设计师A', 'merchant_owner', 1),
(102, 2001, '13800101002', '运营A', 'merchant_operator', 1),
(201, 2001, '13800102001', '助理A', 'merchant_editor', 1);

-- 商家B用户
INSERT IGNORE INTO `user_account` (`id`, `tenant_id`, `mobile`, `nickname`, `role_code`, `status`) VALUES
(103, 2002, '13800101003', '设计师B', 'merchant_owner', 1);

-- 面料商A用户
INSERT IGNORE INTO `user_account` (`id`, `tenant_id`, `mobile`, `nickname`, `role_code`, `status`) VALUES
(301, 3001, '13800103001', '面料商A', 'merchant_owner', 1);

-- 面料商B用户
INSERT IGNORE INTO `user_account` (`id`, `tenant_id`, `mobile`, `nickname`, `role_code`, `status`) VALUES
(302, 3002, '13800103002', '面料商B', 'merchant_owner', 1);

-- 版师A用户
INSERT IGNORE INTO `user_account` (`id`, `tenant_id`, `mobile`, `nickname`, `role_code`, `status`) VALUES
(401, 4001, '13800104001', '版师A', 'merchant_owner', 1);

-- ========================================
-- 3. 更新用户 tenant_role 字段
-- ========================================

UPDATE `user_account` SET `tenant_role` = 'designer' WHERE `id` IN (101, 102, 103);
UPDATE `user_account` SET `tenant_role` = 'design_assistant' WHERE `id` IN (201);
UPDATE `user_account` SET `tenant_role` = 'supplier' WHERE `id` IN (301, 302);
UPDATE `user_account` SET `tenant_role` = 'pattern_maker' WHERE `id` IN (401);

-- ========================================
-- 4. 为测试租户充值 Token 余额
-- ========================================

-- 为每个租户创建钱包并充值 10000 Tokens
INSERT IGNORE INTO `account_wallet` (`tenant_id`, `token_balance`, `frozen_token`) VALUES
(2001, 10000, 0),
(2002, 10000, 0),
(3001, 10000, 0),
(3002, 10000, 0),
(4001, 10000, 0);

-- 更新现有钱包余额
UPDATE `account_wallet` SET `token_balance` = 10000 WHERE `tenant_id` IN (2001, 2002, 3001, 3002, 4001);

-- ========================================
-- 5. 清空旧分配规则并插入测试规则
-- ========================================

DELETE FROM `task_assign_rule` WHERE `id` <= 4;

INSERT IGNORE INTO `task_assign_rule` (`id`, `rule_name`, `keyword`, `target_tenant_id`, `task_type`, `priority`, `enabled`) VALUES
(1, '真丝面料供应商', '真丝', 3001, 'fabric', 10, 1),
(2, '羊毛面料供应商', '羊毛', 3002, 'fabric', 10, 1),
(3, '棉麻面料供应商', '棉麻', 3001, 'fabric', 10, 1),
(4, '通用版师服务商', '默认', 4001, 'pattern', 5, 1);

-- ========================================
-- 6. 清空旧测试数据
-- ========================================

DELETE FROM `design_task` WHERE `requirement_id` IN (SELECT `id` FROM `design_requirement` WHERE `creator_id` IN (101, 103));
DELETE FROM `design_requirement` WHERE `creator_id` IN (101, 103);
DELETE FROM `message` WHERE `receiver_id` IN (101, 103, 201, 301, 302, 401);
DELETE FROM `task_remind_log`;
DELETE FROM `ai_session`;

-- ========================================
-- 7. 创建测试面料库数据
-- ========================================

DELETE FROM `fabric_library` WHERE `supplier_tenant_id` IN (3001, 3002);

INSERT INTO `fabric_library` (`id`, `supplier_tenant_id`, `name`, `category`, `images`, `specs`, `price_per_meter`, `stock_status`, `is_visible`) VALUES
(1, 3001, '真丝提花面料', '真丝',
  '["https://example.com/fabric1_1.jpg", "https://example.com/fabric1_2.jpg"]',
  '{"weight": "68g/m²", "width": "140cm", "composition": "100%桑蚕丝", "pattern": "小碎花"}',
  120.00, 'in_stock', 1),
(2, 3001, '雪纺纱', '真丝',
  '["https://example.com/fabric2_1.jpg"]',
  '{"weight": "42g/m²", "width": "114cm", "composition": "100%桑蚕丝", "transparency": "半透明"}',
  85.00, 'in_stock', 1),
(3, 3001, '测试私有面料', '真丝',
  '["https://example.com/fabric3_1.jpg"]',
  '{"weight": "60g/m²", "width": "140cm"}',
  100.00, 'in_stock', 0),
(4, 3002, '羊毛混纺', '羊毛',
  '["https://example.com/fabric4_1.jpg"]',
  '{"weight": "280g/m²", "width": "150cm", "composition": "70%羊毛 30%锦纶"}',
  180.00, 'in_stock', 1);

-- ========================================
-- 8. 绑定设计助理
-- ========================================

DELETE FROM `tenant_assistant` WHERE `tenant_id` IN (2001, 2002);

INSERT IGNORE INTO `tenant_assistant` (`tenant_id`, `assistant_user_id`) VALUES
(2001, 201);

-- ========================================
-- 9. 验证数据
-- ========================================

SELECT '测试租户' AS '数据类型', COUNT(*) AS '数量' FROM `tenant` WHERE `id` IN (2001, 2002, 3001, 3002, 4001)
UNION ALL
SELECT '测试用户', COUNT(*) FROM `user_account` WHERE `id` IN (101, 102, 103, 201, 301, 302, 401)
UNION ALL
SELECT '钱包账户', COUNT(*) FROM `account_wallet` WHERE `tenant_id` IN (2001, 2002, 3001, 3002, 4001)
UNION ALL
SELECT '分配规则', COUNT(*) FROM `task_assign_rule`
UNION ALL
SELECT '面料库', COUNT(*) FROM `fabric_library`
UNION ALL
SELECT '助理绑定', COUNT(*) FROM `tenant_assistant`;

-- ========================================
-- 测试数据创建完成
-- ========================================

SELECT '测试数据创建完成！可以使用以下用户登录测试：' AS '';
SELECT CONCAT('用户ID: ', id, ' | 租户ID: ', tenant_id, ' | ', nickname, ' | ', tenant_role) AS '测试用户信息'
FROM `user_account`
WHERE `id` IN (101, 102, 103, 201, 301, 302, 401)
ORDER BY `id`;