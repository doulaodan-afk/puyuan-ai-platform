package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("design_task")
public class DesignTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("requirement_id")
    private Long requirementId;

    @TableField("task_type")
    private String taskType; // fabric/pattern

    @TableField("assignee_type")
    private String assigneeType; // supplier/pattern_service/internal

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("content")
    private String content; // JSON string

    @TableField("status")
    private String status; // draft/pending/accepted/shipped/delivered/rejected/done/cancelled

    @TableField("deadline")
    private LocalDateTime deadline;

    @TableField("result_url")
    private String resultUrl;

    @TableField("fabric_task_id")
    private Long fabricTaskId;

    @TableField("logistics_company")
    private String logisticsCompany;

    @TableField("logistics_tracking_no")
    private String logisticsTrackingNo;

    @TableField("logistics_status")
    private String logisticsStatus; // pending/shipped/delivered

    @TableField("offline_logistics_note")
    private String offlineLogisticsNote;

    @TableField("shipped_at")
    private LocalDateTime shippedAt;

    @TableField("delivered_at")
    private LocalDateTime deliveredAt;

    @TableField("notified_at")
    private LocalDateTime notifiedAt;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}