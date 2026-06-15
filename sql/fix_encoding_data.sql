-- ========================================
-- 数据库乱码数据修复 SQL（独立执行用）
-- 在 MySQL 中执行: source fix_encoding_data.sql
-- ========================================

-- 1. 检查当前字符集配置
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';

-- 2. 修改数据库默认字符集
ALTER DATABASE puyuan_ai_mvp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 修改所有关键表的字符集
ALTER TABLE `tenant` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ai_scene` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ai_scene_model` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ai_model` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ai_provider` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. 尝试修复被错误编码的中文数据
-- 原理：如果数据在插入时被当作 latin1 存储，需要先转回 latin1 再转 utf8mb4
-- 这个方法只修复确实损坏的数据，对正常数据无影响

UPDATE tenant 
SET name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
WHERE name != CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4);

UPDATE ai_scene 
SET scene_name = CONVERT(BINARY CONVERT(scene_name USING latin1) USING utf8mb4)
WHERE scene_name != CONVERT(BINARY CONVERT(scene_name USING latin1) USING utf8mb4);

UPDATE ai_scene 
SET scene_description = CONVERT(BINARY CONVERT(scene_description USING latin1) USING utf8mb4)
WHERE scene_description != CONVERT(BINARY CONVERT(scene_description USING latin1) USING utf8mb4);

UPDATE ai_model 
SET model_name = CONVERT(BINARY CONVERT(model_name USING latin1) USING utf8mb4)
WHERE model_name != CONVERT(BINARY CONVERT(model_name USING latin1) USING utf8mb4);

-- 5. 验证修复结果
SELECT '=== 租户数据验证 ===' AS checkpoint;
SELECT id, tenant_code, name, status FROM tenant ORDER BY id;

SELECT '=== 场景数据验证 ===' AS checkpoint;
SELECT id, scene_code, scene_name FROM ai_scene ORDER BY id;
