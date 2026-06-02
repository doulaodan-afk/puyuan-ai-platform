-- Security Features Migration
-- 添加密码字段和登录日志表

-- 1. 添加 password 字段到 user_account 表
-- 注意：MySQL 不支持 IF NOT EXISTS，需要先检查是否存在
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'password');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column already exists.''',
                   'ALTER TABLE user_account ADD COLUMN password VARCHAR(255) DEFAULT NULL COMMENT ''登录密码(SHA256加密)''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建 user_login_log 表
CREATE TABLE IF NOT EXISTS user_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    login_ip VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    device_type VARCHAR(50) DEFAULT NULL COMMENT '设备类型(PC/iOS/Android/Unknown)',
    device_info VARCHAR(255) DEFAULT NULL COMMENT '设备详情',
    location VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    is_success TINYINT(1) DEFAULT 1 COMMENT '是否成功(1=成功,0=失败)',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';
