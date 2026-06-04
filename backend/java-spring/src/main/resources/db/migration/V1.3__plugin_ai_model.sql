-- V1.3__plugin_ai_model.sql
-- 为 plugin 表新增 ai_model 字段，支持插件与 AI 模型关联

-- 新增 ai_model 列
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plugin' AND COLUMN_NAME = 'ai_model';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `plugin` ADD COLUMN `ai_model` VARCHAR(100) NULL COMMENT ''插件绑定的AI模型ID（为空则使用默认模型）'' AFTER `default_token_cost`',
    'SELECT ''Column ai_model already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
