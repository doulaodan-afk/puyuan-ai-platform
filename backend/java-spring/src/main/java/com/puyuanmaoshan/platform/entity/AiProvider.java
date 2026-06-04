package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 提供商配置实体
 * <p>
 * api_key 字段保留向后兼容（单 Key 场景），
 * 新增 api_keys 字段支持多 Key 轮询（JSON 数组格式，如 ["key1","key2","key3"]）。
 * 当 api_keys 有值时优先使用多 Key 轮询，否则回退到 api_key 单 Key 模式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_provider")
public class AiProvider {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("display_name")
    private String displayName;

    @TableField("base_url")
    private String baseUrl;

    /** 单 API Key（向后兼容） */
    @TableField("api_key")
    private String apiKey;

    /**
     * 多 API Key（JSON 数组字符串，如 '["sk-xxx1","sk-xxx2","sk-xxx3"]'）
     * 优先于 api_key 使用，支持轮询与故障转移
     */
    @TableField("api_keys")
    private String apiKeys;

    /**
     * 多 Key 轮询策略：round_robin / least_loaded / random
     * 默认 round_robin
     */
    @TableField("key_strategy")
    private String keyStrategy;

    /** 每个 Key 的最大 RPM（requests per minute），默认 0 表示不限制 */
    @TableField("key_max_rpm")
    private Integer keyMaxRpm;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("priority")
    private Integer priority;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
