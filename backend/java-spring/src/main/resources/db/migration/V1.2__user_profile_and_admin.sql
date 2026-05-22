-- V1.2__user_profile_and_admin.sql
-- 添加用户个人资料和租户管理员管理功能

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

-- 更新 role_code 字段注释（如果需要）
-- ALTER TABLE `user_account`
-- MODIFY COLUMN `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码: boss/tenant_admin/tenant_operator/tenant_viewer/platform_super_admin';

-- 确保 mobile 和 phone 字段一致（迁移现有数据）
UPDATE `user_account` SET `phone` = `mobile` WHERE `phone` IS NULL AND `mobile` IS NOT NULL;

-- 添加成员角色变更审计日志表
CREATE TABLE IF NOT EXISTS `member_role_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `member_user_id` BIGINT NOT NULL COMMENT '成员用户ID',
    `operator_user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `action` VARCHAR(20) NOT NULL COMMENT '操作类型: invite/add_role/remove_role/remove_member/enable/disable',
    `old_role` VARCHAR(20) NULL COMMENT '原角色',
    `new_role` VARCHAR(20) NULL COMMENT '新角色',
    `old_status` VARCHAR(20) NULL COMMENT '原状态',
    `new_status` VARCHAR(20) NULL COMMENT '新状态',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_member_user_id` (`member_user_id`),
    INDEX `idx_operator_user_id` (`operator_user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员角色变更审计日志表';

-- 插入角色配置表
CREATE TABLE IF NOT EXISTS `tenant_role_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) NULL COMMENT '角色描述',
    `permissions` JSON NULL COMMENT '权限列表（JSON格式）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色配置表';

-- 插入系统默认角色
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