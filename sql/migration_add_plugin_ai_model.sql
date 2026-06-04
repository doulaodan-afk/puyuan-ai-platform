-- ============================================================
-- 迁移：为 plugin 表增加 ai_model 字段
-- 用途：插件与 AI 模型关联功能
-- 日期：2026-06-04
-- ============================================================

-- 检查字段是否已存在，若不存在则添加
-- 注意：Plugin.java 的 @TableField("ai_model") 已映射此字段，
-- 如果数据库表中没有该列，需要执行以下 ALTER TABLE 语句

ALTER TABLE `plugin`
    ADD COLUMN IF NOT EXISTS `ai_model` VARCHAR(100) NULL COMMENT '绑定的 AI 模型 ID（如 deepseek-ai/DeepSeek-V3），为空时使用默认模型';

-- 验证迁移
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
  AND TABLE_NAME = 'plugin'
  AND COLUMN_NAME = 'ai_model';
