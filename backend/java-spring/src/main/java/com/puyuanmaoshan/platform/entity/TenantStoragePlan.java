package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租户存储套餐绑定表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tenant_storage_plan")
public class TenantStoragePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("plan_id")
    private Long planId;

    @TableField("tenant_bucket_id")
    private Long tenantBucketId;

    @TableField("plan_status")
    private String planStatus;

    @TableField("effective_date")
    private LocalDate effectiveDate;

    @TableField("expire_date")
    private LocalDate expireDate;

    @TableField("auto_renew")
    private Boolean autoRenew;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
