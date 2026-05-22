-- 多租户人员模型重构 - 数据库迁移脚本
-- 版本：2.0
-- 日期：2026-05-19
-- 说明：将 user 表的 tenant_id 和 tenant_role 迁移到 tenant_user 表，实现多对多关系

USE puyuan_ai_mvp;

-- ========================================
-- 1. 创建 tenant_user 表
-- ========================================

CREATE TABLE IF NOT EXISTS `tenant_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：boss/designer/design_assistant/pattern_maker',
  `invited_by` BIGINT DEFAULT NULL COMMENT '邀请人 user_id',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_tenant_id (`tenant_id`),
  INDEX idx_role (`role`),
  INDEX idx_status (`status`),
  INDEX idx_invited_by (`invited_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用户关联表';

-- ========================================
-- 2. 迁移现有数据
-- ========================================

-- 将 user 表中的 tenant_id 和 tenant_role 数据迁移到 tenant_user 表
INSERT IGNORE INTO `tenant_user` (`tenant_id`, `user_id`, `role`, `status`, `created_at`)
SELECT
    ua.tenant_id,
    ua.id AS user_id,
    'designer' AS role,
    'active' AS status,
    ua.created_at
FROM `user_account` ua
WHERE ua.tenant_id > 0
  AND NOT EXISTS (
    SELECT 1 FROM `tenant_user` tu
    WHERE tu.tenant_id = ua.tenant_id AND tu.user_id = ua.id
  );

-- ========================================
-- 3. 验证迁移结果
-- ========================================

SELECT '迁移验证' AS '';
SELECT
    '用户总数' AS '统计项',
    COUNT(*) AS '数量'
FROM `user_account`
UNION ALL
SELECT
    '有租户的用户数',
    COUNT(DISTINCT user_id)
FROM `tenant_user`
UNION ALL
SELECT
    '租户用户关联记录数',
    COUNT(*)
FROM `tenant_user`
UNION ALL
SELECT
    '涉及的租户数',
    COUNT(DISTINCT tenant_id)
FROM `tenant_user`
UNION ALL
SELECT
    'boss 角色用户数',
    COUNT(DISTINCT user_id)
FROM `tenant_user`
WHERE role = 'boss';

-- ========================================
-- 4. 备份旧数据（可选，建议手动备份完整数据库）
-- ========================================

-- 创建备份表（如果需要回滚）
-- 注意：由于表结构可能已变化，备份功能暂时注释
-- CREATE TABLE IF NOT EXISTS `user_account_backup` LIKE `user_account`;
-- INSERT INTO `user_account_backup` SELECT * FROM `user_account`;

-- ========================================
-- 5. 删除旧字段（谨慎操作！）
-- ========================================

-- 检查字段是否存在后再删除
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'tenant_role'
);

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `user_account` DROP COLUMN `tenant_role`',
    'SELECT "tenant_role 字段不存在，跳过删除" AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tenant_id 字段保留，但不再用于单一租户绑定
-- 改为用于记录最后登录的租户，可后续优化

-- ========================================
-- 6. 创建视图（方便查询）
-- ========================================

-- 租户用户详情视图
CREATE OR REPLACE VIEW `v_tenant_user_detail` AS
SELECT
    tu.id AS tenant_user_id,
    tu.tenant_id,
    t.name AS tenant_name,
    tu.user_id,
    ua.mobile,
    ua.nickname,
    tu.role,
    tu.status,
    tu.invited_by,
    inviter.nickname AS inviter_name,
    tu.created_at AS joined_at
FROM `tenant_user` tu
JOIN `tenant` t ON tu.tenant_id = t.id
JOIN `user_account` ua ON tu.user_id = ua.id
LEFT JOIN `user_account` inviter ON tu.invited_by = inviter.id
WHERE tu.status = 'active';

-- 用户所属租户视图
CREATE OR REPLACE VIEW `v_user_tenants` AS
SELECT
    ua.id AS user_id,
    ua.mobile,
    ua.nickname,
    tu.tenant_id,
    t.name AS tenant_name,
    tu.role,
    tu.status AS membership_status
FROM `user_account` ua
JOIN `tenant_user` tu ON ua.id = tu.user_id
JOIN `tenant` t ON tu.tenant_id = t.id
WHERE tu.status = 'active';

-- ========================================
-- 7. 更新 tenant 表（添加 member_count 字段）
-- ========================================

-- 检查并添加 member_count 字段
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
      AND TABLE_NAME = 'tenant'
      AND COLUMN_NAME = 'member_count'
);

SET @sql = IF(@col_exists > 0,
    'SELECT "member_count 字段已存在" AS result',
    'ALTER TABLE `tenant` ADD COLUMN `member_count` INT DEFAULT 1 COMMENT "成员数量"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 初始化成员数量
UPDATE `tenant` t
SET member_count = (
    SELECT COUNT(*)
    FROM `tenant_user` tu
    WHERE tu.tenant_id = t.id AND tu.status = 'active'
);

-- ========================================
-- 8. 创建触发器（自动更新成员数量）
-- ========================================

DELIMITER //

DROP TRIGGER IF EXISTS `tr_tenant_user_insert`//

CREATE TRIGGER `tr_tenant_user_insert`
AFTER INSERT ON `tenant_user`
FOR EACH ROW
BEGIN
    IF NEW.status = 'active' THEN
        UPDATE `tenant`
        SET member_count = member_count + 1
        WHERE id = NEW.tenant_id;
    END IF;
END//

DROP TRIGGER IF EXISTS `tr_tenant_user_update`//

CREATE TRIGGER `tr_tenant_user_update`
AFTER UPDATE ON `tenant_user`
FOR EACH ROW
BEGIN
    -- 从 inactive 变为 active
    IF NEW.status = 'active' AND OLD.status != 'active' THEN
        UPDATE `tenant` SET member_count = member_count + 1 WHERE id = NEW.tenant_id;
    END IF;
    -- 从 active 变为 inactive 或 deleted
    IF OLD.status = 'active' AND NEW.status != 'active' THEN
        UPDATE `tenant` SET member_count = member_count - 1 WHERE id = OLD.tenant_id;
    END IF;
END//

DROP TRIGGER IF EXISTS `tr_tenant_user_delete`//

CREATE TRIGGER `tr_tenant_user_delete`
AFTER DELETE ON `tenant_user`
FOR EACH ROW
BEGIN
    IF OLD.status = 'active' THEN
        UPDATE `tenant` SET member_count = member_count - 1 WHERE id = OLD.tenant_id;
    END IF;
END//

DELIMITER ;

-- ========================================
-- 9. 插入测试数据（验证多租户功能）
-- ========================================

-- 查找已有的用户和租户
SET @boss_user_id = (SELECT id FROM `user_account` WHERE role_code = 'merchant_owner' LIMIT 1);
SET @designer_user_id = (SELECT id FROM `user_account` WHERE role_code = 'merchant_editor' AND id != @boss_user_id LIMIT 1);
SET @tenant_a_id = (SELECT id FROM `tenant` WHERE tenant_code = 'MERCHANT_A' LIMIT 1);
SET @tenant_b_id = (SELECT id FROM `tenant` WHERE tenant_code = 'MERCHANT_B' LIMIT 1);

-- 如果有用户和租户，添加多租户测试数据
-- 用户A (boss) 同时是租户A和租户B的老板
INSERT IGNORE INTO `tenant_user` (`tenant_id`, `user_id`, `role`, `status`)
VALUES
(@tenant_a_id, @boss_user_id, 'boss', 'active'),
(@tenant_b_id, @boss_user_id, 'boss', 'active');

-- 用户B (designer) 同时是租户A和租户B的设计师
INSERT IGNORE INTO `tenant_user` (`tenant_id`, `user_id`, `role`, `invited_by`, `status`)
VALUES
(@tenant_a_id, @designer_user_id, 'designer', @boss_user_id, 'active'),
(@tenant_b_id, @designer_user_id, 'designer', @boss_user_id, 'active');

-- ========================================
-- 10. 迁移完成检查
-- ========================================

SELECT '════════════════════════════════════════' AS '';
SELECT '迁移完成！请确认以下数据：' AS '';
SELECT '════════════════════════════════════════' AS '';

SELECT
    '用户' AS '类型',
    ua.id AS 'ID',
    ua.mobile AS '手机号',
    ua.nickname AS '昵称',
    GROUP_CONCAT(CONCAT(t.name, '(', tu.role, ')') SEPARATOR ', ') AS '所属工作室'
FROM `user_account` ua
JOIN `tenant_user` tu ON ua.id = tu.user_id
JOIN `tenant` t ON tu.tenant_id = t.id
WHERE tu.status = 'active'
GROUP BY ua.id
ORDER BY ua.id
LIMIT 20;

-- 显示每个工作室的成员
SELECT
    '工作室' AS '类型',
    t.id AS 'ID',
    t.name AS '工作室名称',
    t.member_count AS '成员数',
    GROUP_CONCAT(CONCAT(ua.nickname, '(', tu.role, ')') SEPARATOR ', ') AS '成员列表'
FROM `tenant` t
LEFT JOIN `tenant_user` tu ON t.id = tu.tenant_id AND tu.status = 'active'
LEFT JOIN `user_account` ua ON tu.user_id = ua.id
GROUP BY t.id
ORDER BY t.id;

SELECT '════════════════════════════════════════' AS '';
SELECT '迁移脚本执行完毕！' AS '';
SELECT '注意事项：' AS '';
SELECT '1. 原 user_account 表的 tenant_role 字段已删除' AS '';
SELECT '2. tenant_id 字段保留但不再用于单一绑定' AS '';
SELECT '3. 所有租户-用户关系已迁移到 tenant_user 表' AS '';
SELECT '4. tenant 表新增 member_count 字段自动统计成员数' AS '';
SELECT '════════════════════════════════════════' AS '';

-- 备份表信息（如需回滚，使用：INSERT INTO user_account SELECT * FROM user_account_backup）
SELECT CONCAT('备份表 user_account_backup 包含 ', COUNT(*), ' 条记录') AS '备份信息'
FROM `user_account_backup`;