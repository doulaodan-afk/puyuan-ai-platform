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
@TableName("supplier_collaboration")
public class SupplierCollaboration {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("merchant_tenant_id")
    private Long merchantTenantId;

    @TableField("supplier_tenant_id")
    private Long supplierTenantId;

    @TableField("status")
    private String status; // pending/accepted/rejected/blocked

    @TableField("invited_by")
    private Long invitedBy;

    @TableField("responded_by")
    private Long respondedBy;

    @TableField("responded_at")
    private LocalDateTime respondedAt;

    @TableField("block_reason")
    private String blockReason;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}