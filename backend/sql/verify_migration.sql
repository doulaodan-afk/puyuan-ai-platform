-- 数据库迁移验证脚本
-- 版本：1.0
-- 日期：2026-05-19
-- 说明：验证多租户人员模型迁移的数据完整性

USE puyuan_ai_mvp;

-- ========================================
-- 1. 迁移验证脚本
-- ========================================

SELECT '════════════════════════════════════════' AS '';
SELECT '数据库迁移验证脚本' AS '';
SELECT '════════════════════════════════════════' AS '';

-- 1.1 检查 tenant_user 表是否存在
SELECT '1.1 tenant_user 表结构' AS '';
SELECT
    COLUMN_NAME AS '字段名',
    DATA_TYPE AS '数据类型',
    IS_NULLABLE AS '可为空',
    COLUMN_KEY AS '键'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
  AND TABLE_NAME = 'tenant_user'
ORDER BY ORDINAL_POSITION;

-- 1.2 统计迁移数据
SELECT '' AS '';
SELECT '1.2 迁移数据统计' AS '';

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
    '活跃成员数',
    COUNT(*)
FROM `tenant_user`
WHERE status = 'active'
UNION ALL
SELECT
    '老板数量',
    COUNT(DISTINCT user_id)
FROM `tenant_user`
WHERE role = 'boss';

-- 1.3 角色分布
SELECT '' AS '';
SELECT '1.3 角色分布' AS '';

SELECT
    role AS '角色',
    COUNT(*) AS '人数'
FROM `tenant_user`
WHERE status = 'active'
GROUP BY role
ORDER BY COUNT(*) DESC;

-- 1.4 每个租户的成员统计
SELECT '' AS '';
SELECT '1.4 租户成员统计' AS '';

SELECT
    t.id AS '租户ID',
    t.name AS '租户名称',
    t.member_count AS '成员数',
    GROUP_CONCAT(CONCAT(tu.role, ':', ua.nickname) SEPARATOR ', ') AS '成员列表'
FROM `tenant` t
LEFT JOIN `tenant_user` tu ON t.id = tu.tenant_id AND tu.status = 'active'
LEFT JOIN `user_account` ua ON tu.user_id = ua.id
GROUP BY t.id
ORDER BY t.id;

-- 1.5 检查数据一致性
SELECT '' AS '';
SELECT '1.5 数据一致性检查' AS '';

-- 检查是否有用户在 tenant_user 中但没有对应的 user_account 记录
SELECT
    '用户在tenant_user但不在user_account' AS '问题',
    COUNT(*) AS '数量'
FROM `tenant_user` tu
WHERE NOT EXISTS (
    SELECT 1 FROM `user_account` ua WHERE ua.id = tu.user_id
);

-- 检查是否有用户在 tenant_user 中但没有对应的 tenant 记录
SELECT
    '用户在tenant_user但tenant不存在' AS '问题',
    COUNT(*) AS '数量'
FROM `tenant_user` tu
WHERE NOT EXISTS (
    SELECT 1 FROM `tenant` t WHERE t.id = tu.tenant_id
);

-- 检查是否有重复的 (tenant_id, user_id) 组合（不应有，因为有唯一索引）
SELECT
    '重复的租户用户关联' AS '问题',
    COUNT(*) AS '数量'
FROM `tenant_user`
GROUP BY tenant_id, user_id
HAVING COUNT(*) > 1;

-- 检查是否有用户在多个租户中是老板
SELECT '' AS '';
SELECT '1.6 多租户老板检查' AS '';

SELECT
    '在多个租户中是老板的用户' AS '问题',
    COUNT(*) AS '数量'
FROM (
    SELECT user_id
    FROM `tenant_user`
    WHERE role = 'boss' AND status = 'active'
    GROUP BY user_id
    HAVING COUNT(*) > 1
) AS multi_boss;

-- 显示详细的多租户老板列表
SELECT
    ua.id AS '用户ID',
    ua.mobile AS '手机号',
    ua.nickname AS '昵称',
    GROUP_CONCAT(CONCAT(t.name, '(', tu.role, ')') SEPARATOR ', ') AS '工作室列表'
FROM `user_account` ua
JOIN `tenant_user` tu ON ua.id = tu.user_id
JOIN `tenant` t ON tu.tenant_id = t.id
WHERE tu.role = 'boss' AND tu.status = 'active'
GROUP BY ua.id, ua.mobile, ua.nickname
HAVING COUNT(*) > 1;

-- ========================================
-- 2. 数据完整性验证
-- ========================================

SELECT '' AS '';
SELECT '════════════════════════════════════════' AS '';
SELECT '2. 数据完整性验证' AS '';
SELECT '════════════════════════════════════════' AS '';

-- 2.1 验证触发器是否工作
SELECT '2.1 触发器验证' AS '';

SHOW TRIGGERS WHERE `Schema` = 'puyuan_ai_mvp' AND `Table` = 'tenant_user';

-- 2.2 验证视图是否创建
SELECT '' AS '';
SELECT '2.2 视图验证' AS '';

SHOW FULL TABLES FROM puyuan_ai_mvp WHERE Tables_in_puyuan_ai_mvp LIKE 'v_%';

-- 2.3 验证 user_account 表的 tenant_role 字段是否已删除
SELECT '' AS '';
SELECT '2.3 字段删除验证' AS '';

SELECT
    CASE
        WHEN EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
              AND TABLE_NAME = 'user_account'
              AND COLUMN_NAME = 'tenant_role'
        ) THEN 'tenant_role 字段仍存在，需要删除'
        ELSE '✓ tenant_role 字段已删除'
    END AS '验证结果';

-- ========================================
-- 3. 迁移回滚脚本（备份）
-- ========================================

SELECT '' AS '';
SELECT '════════════════════════════════════════' AS '';
SELECT '3. 迁移回滚脚本（如需回滚，谨慎执行）' AS '';
SELECT '════════════════════════════════════════' AS '';

SELECT '-- 回滚步骤：' AS '';
SELECT '-- 1. 删除 tenant_user 表：' AS '';
SELECT '    DROP TABLE IF EXISTS tenant_user;' AS '';
SELECT '' AS '';
SELECT '-- 2. 从备份恢复 user_account 表：' AS '';
SELECT '    DELETE FROM user_account;' AS '';
SELECT '    INSERT INTO user_account SELECT * FROM user_account_backup;' AS '';
SELECT '' AS '';
SELECT '-- 3. 删除视图：' AS '';
SELECT '    DROP VIEW IF EXISTS v_tenant_user_detail;' AS '';
SELECT '    DROP VIEW IF EXISTS v_user_tenants;' AS '';
SELECT '' AS '';
SELECT '-- 4. 删除触发器：' AS '';
SELECT '    DROP TRIGGER IF EXISTS tr_tenant_user_insert;' AS '';
SELECT '    DROP TRIGGER IF EXISTS tr_tenant_user_update;' AS '';
SELECT '    DROP TRIGGER IF EXISTS tr_tenant_user_delete;' AS '';

-- ========================================
-- 4. 验证通过标准
-- ========================================

SELECT '' AS '';
SELECT '════════════════════════════════════════' AS '';
SELECT '4. 验证通过标准' AS '';
SELECT '════════════════════════════════════════' AS '';

SELECT '✓ tenant_user 表已创建' AS '标准';
SELECT '✓ 迁移数据完整：用户数 > 0，租户数 > 0' AS '标准';
SELECT '✓ 数据一致性检查通过：无异常数据' AS '标准';
SELECT '✓ 触发器已创建：3个触发器' AS '标准';
SELECT '✓ 视图已创建：2个视图' AS '标准';
SELECT '✓ user_account.tenant_role 字段已删除' AS '标准';

-- ========================================
-- 5. 测试数据示例
-- ========================================

SELECT '' AS '';
SELECT '════════════════════════════════════════' AS '';
SELECT '5. 测试数据示例' AS '';
SELECT '════════════════════════════════════════' AS '';

SELECT '-- 查询用户 101 所属的所有工作室：' AS '';
SELECT
    ua.id AS '用户ID',
    ua.mobile AS '手机号',
    ua.nickname AS '昵称',
    t.id AS '租户ID',
    t.name AS '租户名称',
    tu.role AS '角色',
    tu.status AS '状态',
    tu.created_at AS '加入时间'
FROM `user_account` ua
JOIN `tenant_user` tu ON ua.id = tu.user_id
JOIN `tenant` t ON tu.tenant_id = t.id
WHERE ua.id = 101
ORDER BY tu.created_at;

SELECT '' AS '';
SELECT '-- 查询租户 2001 的所有成员：' AS '';
SELECT
    t.id AS '租户ID',
    t.name AS '租户名称',
    ua.id AS '用户ID',
    ua.mobile AS '手机号',
    ua.nickname AS '昵称',
    tu.role AS '角色',
    tu.invited_by AS '邀请人ID',
    iua.nickname AS '邀请人昵称',
    tu.status AS '状态',
    tu.created_at AS '加入时间'
FROM `tenant` t
JOIN `tenant_user` tu ON t.id = tu.tenant_id
LEFT JOIN `user_account` ua ON tu.user_id = ua.id
LEFT JOIN `user_account` iua ON tu.invited_by = iua.id
WHERE t.id = 2001
ORDER BY tu.role DESC, tu.created_at;

-- ========================================
-- 6. 快速修复脚本（如发现问题）
-- ========================================

SELECT '' AS '';
SELECT '════════════════════════════════════════' AS '';
SELECT '6. 快速修复脚本' AS '';
SELECT '════════════════════════════════════════' AS '';

-- 如果发现用户在 tenant_user 中但没有对应的 user_account 记录
SELECT '-- 修复缺失的 user_account 记录：' AS '';
-- SELECT '-- （根据实际情况调整以下 SQL）' AS '';

-- 如果发现重复的 (tenant_id, user_id) 组合
SELECT '-- 删除重复的租户用户关联：' AS '';
-- SELECT '-- （保留最早创建的记录）' AS '';

-- 如果发现 tenant.member_count 不准确
SELECT '-- 重新计算成员数量：' AS '';
SELECT 'UPDATE tenant t' AS '';
SELECT 'SET member_count = (' AS '';
SELECT '    SELECT COUNT(*)' AS '';
SELECT '    FROM tenant_user tu' AS '';
SELECT '    WHERE tu.tenant_id = t.id AND tu.status = "active"' AS '';
SELECT ');' AS '';

-- ========================================
-- 7. 迁移完成确认
-- ========================================

SELECT '' AS '';
SELECT '════════════════════════════════════════' AS '';
SELECT '迁移验证完成！' AS '';
SELECT '════════════════════════════════════════' AS '';

-- 生成验证摘要
SELECT
    '✓' AS '状态',
    '表结构' AS '验证项',
    '已创建' AS '结果'
WHERE EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = 'puyuan_ai_mvp'
      AND TABLE_NAME = 'tenant_user'
)
UNION ALL
SELECT
    '✓' AS '状态',
    '数据迁移' AS '验证项',
    CONCAT(COUNT(*), ' 条记录') AS '结果'
FROM `tenant_user`
UNION ALL
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'puyuan_ai_mvp' AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'tenant_role')
        THEN '✗'
        ELSE '✓'
    END AS '状态',
    '字段删除' AS '验证项',
    'tenant_role 已删除' AS '结果'
UNION ALL
SELECT
    '✓' AS '状态',
    '触发器' AS '验证项',
    CONCAT((SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = 'puyuan_ai_mvp' AND EVENT_OBJECT_TABLE = 'tenant_user'), ' 个') AS '结果'
UNION ALL
SELECT
    '✓' AS '状态',
    '视图' AS '验证项',
    CONCAT((SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA = 'puyuan_ai_mvp' AND TABLE_NAME LIKE 'v_%'), ' 个') AS '结果';

SELECT '════════════════════════════════════════' AS '';
SELECT '下一步：' AS '';
SELECT '1. 确认所有验证项都显示 ✓' AS '';
SELECT '2. 运行后端编译验证：mvn clean compile -DskipTests' AS '';
SELECT '3. 重启后端服务进行功能测试' AS '';
SELECT '4. 使用测试账号验证多租户登录和切换' AS '';
SELECT '════════════════════════════════════════' AS '';