-- 审计日志表字段扩展
-- 添加 IP、old_value、new_value 字段用于记录系统配置变更

ALTER TABLE `audit_log`
ADD COLUMN `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作者 IP 地址' AFTER `detail_json`,
ADD COLUMN `old_value` TEXT DEFAULT NULL COMMENT '变更前值（脱敏）' AFTER `ip`,
ADD COLUMN `new_value` TEXT DEFAULT NULL COMMENT '变更后值（脱敏）' AFTER `old_value`,
ADD INDEX `idx_operator_id` (`operator_id`),
ADD INDEX `idx_target_type_id` (`target_type`, `target_id`),
ADD INDEX `idx_created_at` (`created_at`);
