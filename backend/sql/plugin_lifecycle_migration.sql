-- =============================================
-- Plugin Lifecycle Management Migration
-- =============================================

-- Plugin 表新增字段
ALTER TABLE plugin
  ADD COLUMN lifecycle_status VARCHAR(20) DEFAULT 'testing' COMMENT 'testing/enabled/disabled/gray';

ALTER TABLE plugin
  ADD COLUMN frontend_path VARCHAR(500) COMMENT '前端资源路径（CDN 或相对路径）';

ALTER TABLE plugin
  ADD COLUMN backend_deploy_config TEXT COMMENT '后端部署配置 JSON';

ALTER TABLE plugin
  ADD COLUMN gray_tenant_ids TEXT COMMENT '灰度发布的租户 ID 列表，逗号分隔';

ALTER TABLE plugin
  ADD COLUMN created_by BIGINT COMMENT '创建人 ID';

ALTER TABLE plugin
  ADD COLUMN tested_at DATETIME COMMENT '沙箱测试时间';

ALTER TABLE plugin
  ADD COLUMN published_at DATETIME COMMENT '正式发布时间';

-- 迁移原有的 INT status 到新字段
UPDATE plugin SET lifecycle_status = CASE WHEN status = 1 THEN 'enabled' ELSE 'disabled' END WHERE lifecycle_status IS NULL;
UPDATE plugin SET lifecycle_status = 'enabled' WHERE status = 1 AND lifecycle_status IS NULL;
UPDATE plugin SET lifecycle_status = 'disabled' WHERE status = 0 AND lifecycle_status IS NULL;

-- 添加索引
ALTER TABLE plugin ADD INDEX idx_lifecycle_status (lifecycle_status);

-- =============================================
-- 新增插件部署任务表
-- =============================================
CREATE TABLE IF NOT EXISTS plugin_deployment_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plugin_id VARCHAR(64) NOT NULL COMMENT '插件唯一标识',
  docker_image VARCHAR(512) COMMENT 'Docker 镜像地址',
  env_vars TEXT COMMENT '环境变量 JSON',
  status VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/running/success/failed',
  error_message TEXT COMMENT '错误信息',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plugin_id (plugin_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件部署任务表';