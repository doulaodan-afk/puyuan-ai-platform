-- ============================================================
-- 数据库迁移: AI 场景驱动模型管理架构
-- 创建日期: 2026-06-04
-- 说明: 创建 ai_provider, ai_scene, ai_scene_model 三张表
--       实现场景驱动的模型管理，支持主/备用模型切换
-- ============================================================

-- 1. AI 提供商配置表
DROP TABLE IF EXISTS `ai_provider`;
CREATE TABLE `ai_provider` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `name` VARCHAR(64) NOT NULL COMMENT '提供商标识（如 siliconflow）',
    `display_name` VARCHAR(128) NOT NULL COMMENT '显示名称（如 硅基流动）',
    `base_url` VARCHAR(512) NOT NULL COMMENT 'API 基础地址',
    `api_key` VARCHAR(1024) NOT NULL COMMENT 'API Key（加密存储）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数字越小越优先）',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_enabled_priority` (`enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 提供商配置';

-- 2. AI 场景定义表
DROP TABLE IF EXISTS `ai_scene`;
CREATE TABLE `ai_scene` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `scene_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '场景编码（如 chat, summarize, image_gen）',
    `scene_name` VARCHAR(128) NOT NULL COMMENT '场景名称（如 对话, 摘要, 图片生成）',
    `api_type` VARCHAR(64) NOT NULL COMMENT 'API 类型（chat_completion, image_generation, speech_to_text, video_understanding 等）',
    `scene_description` VARCHAR(512) DEFAULT NULL COMMENT '场景描述',
    `recommendation_prompt` TEXT DEFAULT NULL COMMENT 'AI 推荐模型的自定义提示词（可选）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_scene_code` (`scene_code`),
    INDEX `idx_api_type` (`api_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 场景定义';

-- 3. 场景-模型绑定表
DROP TABLE IF EXISTS `ai_scene_model`;
CREATE TABLE `ai_scene_model` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `scene_id` BIGINT NOT NULL COMMENT '场景 ID',
    `provider_id` BIGINT NOT NULL COMMENT '提供商 ID',
    `model_id` VARCHAR(256) NOT NULL COMMENT '模型标识（如 deepseek-ai/DeepSeek-V3）',
    `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主模型 0-否 1-是',
    `is_fallback` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否备用模型 0-否 1-是',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数字越小越优先）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_scene_primary` (`scene_id`, `is_primary`),
    INDEX `idx_scene_fallback` (`scene_id`, `is_fallback`),
    INDEX `idx_provider_model` (`provider_id`, `model_id`),
    CONSTRAINT `fk_scene_model_scene` FOREIGN KEY (`scene_id`) REFERENCES `ai_scene`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_scene_model_provider` FOREIGN KEY (`provider_id`) REFERENCES `ai_provider`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景-模型绑定';

-- ============================================================
-- 预设场景数据
-- ============================================================

INSERT INTO `ai_scene` (`scene_code`, `scene_name`, `api_type`, `scene_description`) VALUES
('chat', '对话', 'chat_completion', '通用对话场景，适用于问答、闲聊、内容生成等'),
('summarize', '摘要', 'chat_completion', '文本摘要场景，适用于长文本提炼、关键信息提取等'),
('image_gen', '图片生成', 'image_generation', 'AI 图片生成场景，适用于商品图、海报、插画等'),
('image_understand', '图片理解', 'image_understanding', '图片理解场景，适用于图片描述、OCR、物体识别等'),
('video_understand', '视频理解', 'video_understanding', '视频理解场景，适用于视频内容分析、关键帧提取等'),
('speech_to_text', '语音转文字', 'speech_to_text', '语音转文字场景，适用于语音识别、字幕生成等');

-- ============================================================
-- 预设提供商（硅基流动 - 需替换真实 API Key）
-- ============================================================
INSERT INTO `ai_provider` (`name`, `display_name`, `base_url`, `api_key`, `enabled`, `priority`, `description`) VALUES
('siliconflow', '硅基流动', 'https://api.siliconflow.cn/v1', '', 1, 0, '硅基流动 AI 平台，提供多种开源模型。请在前端管理页面填入真实 API Key。');

-- ============================================================
-- 预设场景-模型绑定（默认绑定硅基流动的推荐模型）
-- 注意：以下绑定基于 siliconflow 的 provider_id=1（即上面刚插入的记录）
-- ============================================================
INSERT INTO `ai_scene_model` (`scene_id`, `provider_id`, `model_id`, `is_primary`, `is_fallback`, `priority`) VALUES
((SELECT id FROM ai_scene WHERE scene_code = 'chat'), 1, 'deepseek-ai/DeepSeek-V3', 1, 0, 0),
((SELECT id FROM ai_scene WHERE scene_code = 'summarize'), 1, 'deepseek-ai/DeepSeek-R1', 1, 0, 0),
((SELECT id FROM ai_scene WHERE scene_code = 'image_gen'), 1, 'stabilityai/stable-diffusion-3-5-large', 1, 0, 0),
((SELECT id FROM ai_scene WHERE scene_code = 'image_understand'), 1, 'Qwen/Qwen2.5-VL-72B-Instruct', 1, 0, 0),
((SELECT id FROM ai_scene WHERE scene_code = 'speech_to_text'), 1, 'FunAudioLLM/SenseVoiceSmall', 1, 0, 0);
