-- ============================================================
-- V1.5__tenant_profile_fields.sql
-- 租户表增加企业详情字段：logo_url、行业、联系方式、地址、简介
-- ============================================================

ALTER TABLE `tenant`
    ADD COLUMN `logo_url` VARCHAR(500) NULL COMMENT '企业Logo URL' AFTER `tenant_type`,
    ADD COLUMN `industry` VARCHAR(50) NULL COMMENT '行业分类' AFTER `logo_url`,
    ADD COLUMN `contact_phone` VARCHAR(20) NULL COMMENT '联系电话' AFTER `industry`,
    ADD COLUMN `contact_email` VARCHAR(100) NULL COMMENT '联系邮箱' AFTER `contact_phone`,
    ADD COLUMN `address` VARCHAR(300) NULL COMMENT '地址' AFTER `contact_email`,
    ADD COLUMN `description` VARCHAR(500) NULL COMMENT '企业简介' AFTER `address`;