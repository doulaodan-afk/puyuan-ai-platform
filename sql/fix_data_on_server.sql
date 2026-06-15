SET NAMES utf8mb4;

-- 租户名称修复（直接用 UTF-8 hex 值）
-- 濮院毛衫平台 = E6BFAEE999A2E6AF9BE8A1ABE5B9B3E58FB0
UPDATE tenant SET name = 0xE6BFAEE999A2E6AF9BE8A1ABE5B9B3E58FB0 WHERE id = 2001;

-- 毛衫商家示范店 = E6AF9BE8A1ABE59586E5AEB6E7A4BAE88C83E5BA97
UPDATE tenant SET name = 0xE6AF9BE8A1ABE59586E5AEB6E7A4BAE88C83E5BA97 WHERE id = 2002;

-- 场景名称修复
UPDATE ai_scene SET scene_name = 0xE5AFB9E8AF9D WHERE scene_code = 'chat';          -- 对话
UPDATE ai_scene SET scene_name = 0xE69198E8A681 WHERE scene_code = 'summarize';      -- 摘要
UPDATE ai_scene SET scene_name = 0xE59BBEE78987E7949FE68890 WHERE scene_code = 'image_gen';    -- 图片生成
UPDATE ai_scene SET scene_name = 0xE59BBEE78987E79086E8A7A3 WHERE scene_code = 'image_understand'; -- 图片理解
UPDATE ai_scene SET scene_name = 0xE8A786E9A291E79086E8A7A3 WHERE scene_code = 'video_understand'; -- 视频理解
UPDATE ai_scene SET scene_name = 0xE8AFADE99FB3E8BDACE69687E5AD97 WHERE scene_code = 'speech_to_text'; -- 语音转文字

-- 验证
SELECT '=== tenants ===' AS checkpoint;
SELECT id, tenant_code, name, HEX(name) FROM tenant;
SELECT '=== ai_scene ===' AS checkpoint;
SELECT id, scene_code, scene_name, HEX(scene_name) FROM ai_scene;
