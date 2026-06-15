-- ============================================================
-- V1.4: 租户存储空间管理与计费体系
-- 
-- 设计目标：类比"电表"模式，平台给每个租户分配独立的七牛云Bucket空间，
-- 对存储使用量进行计量和收费管理。
--
-- 核心概念：
--   tenant_bucket    - 租户存储空间分配（类比电表）
--   storage_plan     - 存储套餐模板（定价方案）
--   tenant_storage_plan - 租户订购的存储套餐（绑定关系）
--   storage_billing_record - 存储计费记录（每月账单）
--   storage_usage_log - 存储用量快照日志（每日计量）
-- ============================================================

-- -------------------------------------------------------
-- 1. 租户存储空间分配表（核心：类比"电表"安装）
-- 平台为每个租户分配独立的七牛云Bucket
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_bucket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '关联租户ID',
    bucket_name VARCHAR(128) NOT NULL COMMENT '七牛云Bucket名称（全局唯一）',
    bucket_region VARCHAR(32) NOT NULL DEFAULT 'z0' COMMENT '存储区域(z0华东/z1华北/z2华南/na0北美/as0东南亚)',
    bucket_domain VARCHAR(256) DEFAULT NULL COMMENT 'CDN加速域名',
    bucket_private TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否私有空间(0公开/1私有)',
    access_key_encrypted TEXT DEFAULT NULL COMMENT '该空间专用AccessKey（加密存储）',
    secret_key_encrypted TEXT DEFAULT NULL COMMENT '该空间专用SecretKey（加密存储）',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态(active/creating/suspended/deleting/deleted)',
    notes VARCHAR(512) DEFAULT NULL COMMENT '备注说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    UNIQUE INDEX uk_bucket_name (bucket_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户存储空间分配表（类比电表）';

-- -------------------------------------------------------
-- 2. 存储套餐模板表（定价方案）
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS storage_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    plan_name VARCHAR(64) NOT NULL COMMENT '套餐名称（如"基础版""标准版""旗舰版"）',
    plan_code VARCHAR(32) NOT NULL COMMENT '套餐编码(如 free/basic/standard/enterprise)',
    plan_level INT NOT NULL DEFAULT 1 COMMENT '套餐等级(1-10,数值越大越高档)',
    
    -- 存储配额
    storage_quota_gb DOUBLE NOT NULL DEFAULT 10 COMMENT '存储配额(GB)',
    max_file_count BIGINT DEFAULT NULL COMMENT '最大文件数上限(NULL=不限)',
    max_file_size_mb INT DEFAULT NULL COMMENT '单个文件最大大小(MB)',
    
    -- 流量配额
    monthly_traffic_gb DOUBLE NOT NULL DEFAULT 100 COMMENT '月外网流出流量配额(GB)',
    monthly_cdn_traffic_gb DOUBLE DEFAULT 50 COMMENT '月CDN回源流量配额(GB)',
    
    -- 请求次数配额
    monthly_get_requests BIGINT DEFAULT 100000 COMMENT '月GET请求次数配额',
    monthly_put_requests BIGINT DEFAULT 10000 COMMENT '月PUT请求次数配额',
    
    -- 计费定价
    base_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础月费(元)',
    storage_price_per_gb DECIMAL(10,4) NOT NULL DEFAULT 0.1000 COMMENT '超额存储单价(元/GB/月)',
    traffic_price_per_gb DECIMAL(10,4) NOT NULL DEFAULT 0.5000 COMMENT '超额流量单价(元/GB)',
    request_price_per_10k DECIMAL(10,4) NOT NULL DEFAULT 0.0100 COMMENT '超额请求单价(元/万次)',
    
    -- 其他
    free_trial_days INT DEFAULT 0 COMMENT '免费试用天数',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0禁用/1启用)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    description TEXT DEFAULT NULL COMMENT '套餐描述',
    features_json JSON DEFAULT NULL COMMENT '套餐特性列表(JSON数组)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_plan_code (plan_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储套餐模板表';

-- -------------------------------------------------------
-- 3. 租户存储套餐绑定表（租户订购关系）
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_storage_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '关联租户ID',
    plan_id BIGINT NOT NULL COMMENT '关联套餐ID',
    tenant_bucket_id BIGINT NOT NULL COMMENT '关联存储空间ID',
    plan_status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态(active/expired/cancelled/upgraded)',
    effective_date DATE NOT NULL COMMENT '生效日期',
    expire_date DATE DEFAULT NULL COMMENT '到期日期(NULL=永不过期)',
    auto_renew TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动续费',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_bucket_id (tenant_bucket_id),
    INDEX idx_status_effective (plan_status, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户存储套餐绑定表';

-- -------------------------------------------------------
-- 4. 存储计费记录表（每月账单）
-- 基于七牛云统计接口数据汇总生成
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS storage_billing_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    tenant_bucket_id BIGINT NOT NULL COMMENT '存储空间ID',
    bill_period VARCHAR(7) NOT NULL COMMENT '账单周期(YYYY-MM)',
    
    -- 存储用量（从七牛云space接口获取）
    standard_storage_gb DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '标准存储量(GB)',
    line_storage_gb DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '低频存储量(GB)',
    archive_storage_gb DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '归档存储量(GB)',
    standard_file_count BIGINT NOT NULL DEFAULT 0 COMMENT '标准文件数',
    
    -- 流量用量（从七牛云blob_io接口获取）
    external_traffic_gb DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '外网流出流量(GB)',
    cdn_traffic_gb DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'CDN回源流量(GB)',
    
    -- 请求次数（从七牛云blob_io/rs_put接口获取）
    get_requests BIGINT NOT NULL DEFAULT 0 COMMENT 'GET请求次数',
    put_requests BIGINT NOT NULL DEFAULT 0 COMMENT 'PUT请求次数',
    
    -- 配额信息
    quota_storage_gb DECIMAL(10,2) NOT NULL DEFAULT 10 COMMENT '存储配额(GB)',
    quota_traffic_gb DECIMAL(10,2) NOT NULL DEFAULT 100 COMMENT '流量配额(GB)',
    
    -- 计费金额
    base_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础月费',
    storage_overage_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '超额存储费',
    traffic_overage_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '超额流量费',
    request_overage_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '超额请求费',
    total_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总费用',
    
    -- 状态
    bill_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态(pending/calculated/paid/overdue/cancelled)',
    calculated_at DATETIME DEFAULT NULL COMMENT '计算时间',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_bucket_id (tenant_bucket_id),
    INDEX idx_bill_period (bill_period),
    INDEX idx_bill_status (bill_status),
    UNIQUE INDEX uk_tenant_bucket_period (tenant_id, tenant_bucket_id, bill_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储计费记录表（月度账单）';

-- -------------------------------------------------------
-- 5. 存储用量快照日志表（每日计量数据）
-- 每日从七牛云获取统计数据进行快照
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS storage_usage_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    tenant_bucket_id BIGINT NOT NULL COMMENT '存储空间ID',
    snapshot_date DATE NOT NULL COMMENT '快照日期',
    
    -- 存储（当日最新值）
    standard_storage_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '标准存储量(字节)',
    line_storage_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '低频存储量(字节)',
    archive_storage_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '归档存储量(字节)',
    standard_file_count BIGINT NOT NULL DEFAULT 0 COMMENT '标准文件数',
    line_file_count BIGINT NOT NULL DEFAULT 0 COMMENT '低频文件数',
    archive_file_count BIGINT NOT NULL DEFAULT 0 COMMENT '归档文件数',
    
    -- 流量（当日累计）
    external_flux_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '外网流出流量(字节)',
    cdn_flux_bytes BIGINT NOT NULL DEFAULT 0 COMMENT 'CDN回源流量(字节)',
    
    -- 请求次数（当日累计）
    get_requests BIGINT NOT NULL DEFAULT 0 COMMENT 'GET请求次数',
    put_requests BIGINT NOT NULL DEFAULT 0 COMMENT 'PUT请求次数',
    
    -- 元数据
    fetch_status VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '数据获取状态(success/failed/partial)',
    fetch_error TEXT DEFAULT NULL COMMENT '获取失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_bucket_id (tenant_bucket_id),
    INDEX idx_snapshot_date (snapshot_date),
    UNIQUE INDEX uk_tenant_bucket_date (tenant_id, tenant_bucket_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储用量快照日志表（每日计量）';

-- -------------------------------------------------------
-- 6. 预置默认存储套餐数据
-- -------------------------------------------------------
INSERT INTO storage_plan (plan_name, plan_code, plan_level, storage_quota_gb, monthly_traffic_gb, monthly_cdn_traffic_gb,
    monthly_get_requests, monthly_put_requests, base_price, storage_price_per_gb, traffic_price_per_gb, request_price_per_10k,
    free_trial_days, status, sort_order, description, features_json)
VALUES
('免费版', 'free', 1, 10, 50, 10, 100000, 10000, 0.00, 0.10, 0.50, 0.01, 0, 1, 1,
 '适合个人开发者和小型项目，提供基础的存储能力',
 '["10GB 存储空间","50GB 月流量","10GB CDN回源","标准存储","基础文件管理"]'),

('标准版', 'standard', 2, 100, 500, 100, 500000, 50000, 99.00, 0.08, 0.40, 0.01, 7, 1, 2,
 '适合成长型商家，提供充足的存储和流量配额',
 '["100GB 存储空间","500GB 月流量","100GB CDN回源","标准+低频存储","文件生命周期管理","每日统计报表"]'),

('企业版', 'enterprise', 3, 1024, 5000, 1000, 2000000, 200000, 499.00, 0.06, 0.30, 0.01, 7, 1, 3,
 '适合大型企业，提供海量存储和高流量支持',
 '["1TB 存储空间","5TB 月流量","1TB CDN回源","全存储类型支持","自定义域名","实时统计报表","专属技术支持"]'),

('旗舰版', 'premium', 4, 5120, 20000, 5000, 10000000, 1000000, 1999.00, 0.04, 0.20, 0.005, 0, 1, 4,
 '适合大规模商业平台，提供极致存储能力',
 '["5TB 存储空间","20TB 月流量","5TB CDN回源","全存储类型","多Bucket管理","高级安全策略","7x24专属服务"]');
