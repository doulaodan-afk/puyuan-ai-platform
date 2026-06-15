-- 幂等迁移脚本：确保所有列/表/索引存在
-- 可安全重复执行，用于已有数据卷的Docker部署
-- 执行方式：docker exec -i puyuan-mysql mysql -u root -proot123 puyuan_ai_mvp < migrate-idempotent.sql

-- ========================================
-- user_account 扩展列
-- ========================================

-- avatar_url
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'avatar_url';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `avatar_url` VARCHAR(500) NULL COMMENT ''用户头像URL'' AFTER `nickname`',
    'SELECT ''Column avatar_url already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- phone
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'phone';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `phone` VARCHAR(20) NULL COMMENT ''手机号'' AFTER `avatar_url`',
    'SELECT ''Column phone already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- wechat_openid
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'wechat_openid';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `wechat_openid` VARCHAR(100) NULL COMMENT ''微信OpenID'' AFTER `phone`',
    'SELECT ''Column wechat_openid already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- wechat_unionid
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'wechat_unionid';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `wechat_unionid` VARCHAR(100) NULL COMMENT ''微信UnionID'' AFTER `wechat_openid`',
    'SELECT ''Column wechat_unionid already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- email
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'email';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `email` VARCHAR(100) NULL COMMENT ''邮箱'' AFTER `wechat_unionid`',
    'SELECT ''Column email already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- password
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'password';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user_account` ADD COLUMN `password` VARCHAR(255) NULL COMMENT ''密码'' AFTER `email`',
    'SELECT ''Column password already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- idx_wechat_openid
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND INDEX_NAME = 'idx_wechat_openid';
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `user_account` ADD INDEX `idx_wechat_openid` (`wechat_openid`)',
    'SELECT ''Index idx_wechat_openid already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- idx_email
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND INDEX_NAME = 'idx_email';
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `user_account` ADD INDEX `idx_email` (`email`)',
    'SELECT ''Index idx_email already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 同步 phone = mobile
UPDATE `user_account` SET `phone` = `mobile` WHERE `phone` IS NULL AND `mobile` IS NOT NULL;

-- ========================================
-- tenant 扩展列
-- ========================================

-- tenant_type
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant' AND COLUMN_NAME = 'tenant_type';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `tenant` ADD COLUMN `tenant_type` VARCHAR(32) DEFAULT ''standard'' COMMENT ''租户类型'' AFTER `level`',
    'SELECT ''Column tenant_type already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- plugin 扩展列
-- ========================================

-- description
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'description';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `description` VARCHAR(500) NULL COMMENT ''插件描述'' AFTER `frontend_entry`',
    'SELECT ''Column description already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- icon_url
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'icon_url';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `icon_url` VARCHAR(500) NULL COMMENT ''图标URL'' AFTER `description`',
    'SELECT ''Column icon_url already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- frontend_path
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'frontend_path';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `frontend_path` VARCHAR(255) NULL COMMENT ''前端路径'' AFTER `icon_url`',
    'SELECT ''Column frontend_path already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- lifecycle_status
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'lifecycle_status';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `lifecycle_status` VARCHAR(32) DEFAULT ''development'' COMMENT ''生命周期状态'' AFTER `frontend_path`',
    'SELECT ''Column lifecycle_status already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- created_by
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'created_by';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `created_by` BIGINT NULL COMMENT ''创建人ID'' AFTER `lifecycle_status`',
    'SELECT ''Column created_by already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tested_at
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'tested_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `tested_at` DATETIME NULL COMMENT ''测试时间'' AFTER `created_by`',
    'SELECT ''Column tested_at already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- published_at
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'published_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `published_at` DATETIME NULL COMMENT ''发布时间'' AFTER `tested_at`',
    'SELECT ''Column published_at already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- gray_tenant_ids
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'gray_tenant_ids';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `gray_tenant_ids` VARCHAR(1000) NULL COMMENT ''灰度租户ID列表'' AFTER `published_at`',
    'SELECT ''Column gray_tenant_ids already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- backend_deploy_config
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'backend_deploy_config';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `backend_deploy_config` TEXT NULL COMMENT ''后端部署配置'' AFTER `gray_tenant_ids`',
    'SELECT ''Column backend_deploy_config already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 新增表：member_role_audit_log、tenant_role_config
-- ========================================

CREATE TABLE IF NOT EXISTS `member_role_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `member_user_id` BIGINT NOT NULL COMMENT '成员用户ID',
    `operator_user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `action` VARCHAR(20) NOT NULL COMMENT '操作类型',
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

CREATE TABLE IF NOT EXISTS `tenant_role_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) NULL COMMENT '角色描述',
    `permissions` JSON NULL COMMENT '权限列表',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色配置表';

INSERT INTO `tenant_role_config` (`role_code`, `role_name`, `description`, `permissions`, `sort_order`, `is_system`) VALUES
('boss', 'Boss', 'Studio owner with all permissions', '["*"]', 1, 1),
('tenant_admin', 'Admin', 'Can manage members and access all features', '["member_manage","plugin_invoke","billing_view","settings_view"]', 2, 1),
('tenant_operator', '面料特供商', '与工作室深度合作的特别供应商，可调用AI插件和查看账单', '["plugin_invoke","billing_view","fabric_manage"]', 3, 1),
('tenant_viewer', 'Viewer', 'Read-only access', '["billing_view"]', 4, 1)
ON DUPLICATE KEY UPDATE
`role_name` = VALUES(`role_name`),
`description` = VALUES(`description`),
`permissions` = VALUES(`permissions`),
`sort_order` = VALUES(`sort_order`);

-- ========================================
-- 完成
-- ========================================
SELECT 'Migration completed successfully' AS result;