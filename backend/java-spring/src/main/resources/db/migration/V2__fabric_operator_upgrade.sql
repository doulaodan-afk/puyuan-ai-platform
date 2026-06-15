-- ============================================================
-- V2: 面料特供商角色升级迁移
-- 1. fabric_library 新增 tenant_id / creator_id 字段
-- 2. 创建 requirement_fabric 关联表
-- ============================================================
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS 语法，
-- 此 migration 曾导致 Flyway 验证失败（success=0）。
-- 已在服务器上手动执行完毕，此处保留为空操作占位。
-- ============================================================

-- Step 1: fabric_library 新增字段（已手动执行）
-- ALTER TABLE fabric_library
--     ADD COLUMN tenant_id BIGINT COMMENT '所属商家租户ID（面料特供商所在工作室）' AFTER supplier_tenant_id,
--     ADD COLUMN creator_id BIGINT COMMENT '上传者用户ID（面料特供商本人）' AFTER tenant_id,
--     ADD INDEX idx_fl_tenant (tenant_id),
--     ADD INDEX idx_fl_creator (creator_id);

-- Step 2: 历史数据平滑迁移（不需要回填）
-- UPDATE fabric_library SET tenant_id = supplier_tenant_id WHERE tenant_id IS NULL;

-- Step 3: 创建需求-面料关联表（已手动执行）
-- CREATE TABLE IF NOT EXISTS requirement_fabric (
--     id BIGINT PRIMARY KEY AUTO_INCREMENT,
--     requirement_id BIGINT NOT NULL COMMENT '设计需求ID',
--     fabric_id BIGINT NOT NULL COMMENT '面料库条目ID (→ fabric_library.id)',
--     fabric_supplier_id BIGINT COMMENT '面料特供商用户ID',
--     quantity DECIMAL(10,2) COMMENT '用量（米）',
--     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     UNIQUE KEY uk_req_fabric (requirement_id, fabric_id),
--     INDEX idx_rf_requirement (requirement_id),
--     INDEX idx_rf_fabric (fabric_id),
--     INDEX idx_rf_supplier (fabric_supplier_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计需求-面料关联表';

-- 空操作占位，确保 Flyway 记录此版本为已成功应用
SELECT 1;
