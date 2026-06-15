-- ============================================================
-- U1.0__baseline_schema_rollback.sql
-- 濮院毛衫 AI 平台 - V1.0 基线架构回滚脚本
-- 
-- ⚠️ 警告: 此脚本会删除所有表和数据，仅用于紧急回滚场景！
-- 执行前请确认已备份数据库。
-- 
-- 用法: mysql -u root -p puyuan_ai_mvp < U1.0__baseline_schema_rollback.sql
-- ============================================================

-- 按依赖关系逆序删除表
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `tenant_rate_limit_config`;
DROP TABLE IF EXISTS `system_config`;
DROP TABLE IF EXISTS `task_remind_log`;
DROP TABLE IF EXISTS `task_assign_rule`;
DROP TABLE IF EXISTS `supplier_collaboration`;
DROP TABLE IF EXISTS `supplier_registration`;
DROP TABLE IF EXISTS `fabric_library`;
DROP TABLE IF EXISTS `design_task`;
DROP TABLE IF EXISTS `design_requirement`;
DROP TABLE IF EXISTS `plugin_invoke_log`;
DROP TABLE IF EXISTS `billing_statement_daily`;
DROP TABLE IF EXISTS `recharge_order`;
DROP TABLE IF EXISTS `billing_ledger`;
DROP TABLE IF EXISTS `idempotency_record`;
DROP TABLE IF EXISTS `account_wallet`;
DROP TABLE IF EXISTS `ai_session`;
DROP TABLE IF EXISTS `ai_scene_model`;
DROP TABLE IF EXISTS `ai_scene`;
DROP TABLE IF EXISTS `ai_provider`;
DROP TABLE IF EXISTS `plugin_deployment_task`;
DROP TABLE IF EXISTS `plugin_config`;
DROP TABLE IF EXISTS `tenant_plugin`;
DROP TABLE IF EXISTS `plugin`;
DROP TABLE IF EXISTS `tenant_assistant`;
DROP TABLE IF EXISTS `user_login_log`;
DROP TABLE IF EXISTS `member_role_audit_log`;
DROP TABLE IF EXISTS `tenant_role_config`;
DROP TABLE IF EXISTS `tenant_user`;
DROP TABLE IF EXISTS `user_account`;
DROP TABLE IF EXISTS `tenant`;

-- 清理 Flyway 历史记录（如果使用了 baseline-on-migrate）
DELETE FROM `flyway_schema_history` WHERE `version` >= '1.0';
