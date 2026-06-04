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
 * 场景-模型绑定实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_scene_model")
public class AiSceneModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scene_id")
    private Long sceneId;

    @TableField("provider_id")
    private Long providerId;

    @TableField("model_id")
    private String modelId;

    @TableField("is_primary")
    private Boolean isPrimary;

    @TableField("is_fallback")
    private Boolean isFallback;

    @TableField("priority")
    private Integer priority;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
