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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("design_requirement")
public class DesignRequirement {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("title")
    private String title;

    @TableField("raw_images")
    private String rawImages; // JSON string

    @TableField("raw_videos")
    private String rawVideos; // JSON string

    @TableField("raw_audio_url")
    private String rawAudioUrl;

    @TableField("raw_text")
    private String rawText;

    @TableField("conversation_history")
    private String conversationHistory; // JSON string

    @TableField("ai_summary")
    private String aiSummary; // JSON string

    @TableField("designer_approved")
    private Integer designerApproved; // 0-未确认,1-确认发布,2-转助理

    @TableField("assistant_id")
    private Long assistantId;

    @TableField("status")
    private String status; // draft/assistant_processing/released/completed/cancelled

    @TableField("total_token_cost")
    private Integer totalTokenCost;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}