-- 面料商入驻申请表
CREATE TABLE IF NOT EXISTS `supplier_registration` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `company_name` VARCHAR(128) NOT NULL COMMENT '公司名称',
  `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人姓名',
  `contact_mobile` VARCHAR(32) NOT NULL COMMENT '联系人手机号',
  `business_license` VARCHAR(500) COMMENT '营业执照图片URL',
  `address` VARCHAR(255) COMMENT '公司地址',
  `fabric_categories` JSON COMMENT '面料品类数组',
  `description` TEXT COMMENT '公司介绍',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `reject_reason` TEXT COMMENT '驳回原因',
  `tenant_id` BIGINT DEFAULT 0 COMMENT '审核通过后关联的租户ID',
  `user_id` BIGINT DEFAULT 0 COMMENT '审核通过后关联的用户ID',
  `admin_id` BIGINT DEFAULT 0 COMMENT '审核人ID',
  `reviewed_at` DATETIME COMMENT '审核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (`status`),
  INDEX idx_contact_mobile (`contact_mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面料商入驻申请表';

-- 面料商合作表
CREATE TABLE IF NOT EXISTS `supplier_collaboration` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `merchant_tenant_id` BIGINT NOT NULL COMMENT '商家租户ID',
  `supplier_tenant_id` BIGINT NOT NULL COMMENT '面料商租户ID',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/accepted/rejected/blocked',
  `invited_by` BIGINT DEFAULT 0 COMMENT '邀请人user_id',
  `responded_by` BIGINT DEFAULT 0 COMMENT '响应人user_id',
  `responded_at` DATETIME COMMENT '响应时间',
  `block_reason` TEXT COMMENT '屏蔽原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_merchant_supplier (`merchant_tenant_id`, `supplier_tenant_id`),
  INDEX idx_merchant_tenant (`merchant_tenant_id`),
  INDEX idx_supplier_tenant (`supplier_tenant_id`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面料商合作表';