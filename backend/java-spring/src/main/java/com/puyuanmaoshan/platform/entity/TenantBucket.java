package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租户存储空间分配表（类比电表）
 * 平台为每个租户分配独立的七牛云Bucket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tenant_bucket")
public class TenantBucket {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("bucket_region")
    private String bucketRegion;

    @TableField("bucket_domain")
    private String bucketDomain;

    @TableField("bucket_private")
    private Boolean bucketPrivate;

    @TableField("access_key_encrypted")
    private String accessKeyEncrypted;

    @TableField("secret_key_encrypted")
    private String secretKeyEncrypted;

    @TableField("status")
    private String status;

    @TableField("notes")
    private String notes;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
