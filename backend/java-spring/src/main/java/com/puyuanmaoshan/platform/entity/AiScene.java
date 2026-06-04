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
 * AI 场景定义实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_scene")
public class AiScene {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("scene_name")
    private String sceneName;

    @TableField("api_type")
    private String apiType;

    @TableField("scene_description")
    private String sceneDescription;

    @TableField("recommendation_prompt")
    private String recommendationPrompt;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
