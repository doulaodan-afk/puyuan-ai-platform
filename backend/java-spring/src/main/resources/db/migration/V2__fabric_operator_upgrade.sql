-- ============================================================
-- V2: 面料特供商角色升级迁移
-- 1. fabric_library 新增 tenant_id / creator_id 字段
-- 2. 创建 requirement_fabric 关联表
-- ============================================================

-- Step 1: fabric_library 新增字段
ALTER TABLE fabric_library
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT COMMENT '所属商家租户ID（面料特供商所在工作室）' AFTER supplier_tenant_id,
    ADD COLUMN IF NOT EXISTS creator_id BIGINT COMMENT '上传者用户ID（面料特供商本人）' AFTER tenant_id,
    ADD INDEX IF NOT EXISTS idx_fl_tenant (tenant_id),
    ADD INDEX IF NOT EXISTS idx_fl_creator (creator_id);

-- Step 2: 历史数据平滑迁移 — 将已有的 supplier_tenant_id 数据回填 tenant_id
-- （对于外部面料商租户的面料，tenant_id 保持 NULL；后续面料特供商创建的数据会写入 tenant_id + creator_id）
-- UPDATE fabric_library SET tenant_id = supplier_tenant_id WHERE tenant_id IS NULL;

-- Step 3: 创建需求-面料关联表
CREATE TABLE IF NOT EXISTS requirement_fabric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id BIGINT NOT NULL COMMENT '设计需求ID',
    fabric_id BIGINT NOT NULL COMMENT '面料库条目ID (→ fabric_library.id)',
    fabric_supplier_id BIGINT COMMENT '面料特供商用户ID',
    quantity DECIMAL(10,2) COMMENT '用量（米）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_req_fabric (requirement_id, fabric_id),
    INDEX idx_rf_requirement (requirement_id),
    INDEX idx_rf_fabric (fabric_id),
    INDEX idx_rf_supplier (fabric_supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计需求-面料关联表';
