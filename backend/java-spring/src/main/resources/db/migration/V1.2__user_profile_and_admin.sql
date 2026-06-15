-- ============================================================
-- V1.2__user_profile_and_admin.sql
-- 添加用户个人资料和租户管理员管理功能
-- 
-- 注意: 表 tenant_role_config 和 member_role_audit_log 已在 V1.0 中创建，
-- 此脚本仅执行增量字段变更。
-- ============================================================

-- 扩展 user_account 表（使用 ADD COLUMN IF NOT EXISTS 的变通方法）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'avatar_url';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `avatar_url` VARCHAR(500) NULL COMMENT ''用户头像URL'' AFTER `nickname`',
    'SELECT ''Column avatar_url already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'phone';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `phone` VARCHAR(20) NULL COMMENT ''手机号'' AFTER `avatar_url`',
    'SELECT ''Column phone already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'wechat_openid';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `wechat_openid` VARCHAR(100) NULL COMMENT ''微信OpenID'' AFTER `phone`',
    'SELECT ''Column wechat_openid already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'wechat_unionid';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `wechat_unionid` VARCHAR(100) NULL COMMENT ''微信UnionID'' AFTER `wechat_openid`',
    'SELECT ''Column wechat_unionid already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'email';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `email` VARCHAR(100) NULL COMMENT ''邮箱'' AFTER `wechat_unionid`',
    'SELECT ''Column email already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND INDEX_NAME = 'idx_wechat_openid';
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `user_account` ADD INDEX `idx_wechat_openid` (`wechat_openid`)',
    'SELECT ''Index idx_wechat_openid already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND INDEX_NAME = 'idx_email';
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `user_account` ADD INDEX `idx_email` (`email`)',
    'SELECT ''Index idx_email already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 确保 mobile 和 phone 字段一致（迁移现有数据）
UPDATE `user_account` SET `phone` = `mobile` WHERE `phone` IS NULL AND `mobile` IS NOT NULL;

-- 确保角色配置存在（幂等插入）
INSERT INTO `tenant_role_config` (`role_code`, `role_name`, `description`, `permissions`, `sort_order`, `is_system`) VALUES
('boss', 'Boss', 'Studio owner with all permissions', '["*"]', 1, 1),
('tenant_admin', 'Admin', 'Can manage members and access all features', '["member_manage","plugin_invoke","billing_view","settings_view"]', 2, 1),
('tenant_operator', 'Operator', 'Can invoke AI plugins and view billing', '["plugin_invoke","billing_view"]', 3, 1),
('tenant_viewer', 'Viewer', 'Read-only access', '["billing_view"]', 4, 1)
ON DUPLICATE KEY UPDATE
`role_name` = VALUES(`role_name`),
`description` = VALUES(`description`),
`permissions` = VALUES(`permissions`),
`sort_order` = VALUES(`sort_order`);
