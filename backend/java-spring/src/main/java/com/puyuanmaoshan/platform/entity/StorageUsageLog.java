package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 存储用量快照日志表（每日计量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("storage_usage_log")
public class StorageUsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("tenant_bucket_id")
    private Long tenantBucketId;

    @TableField("snapshot_date")
    private LocalDate snapshotDate;

    @TableField("standard_storage_bytes")
    private Long standardStorageBytes;

    @TableField("line_storage_bytes")
    private Long lineStorageBytes;

    @TableField("archive_storage_bytes")
    private Long archiveStorageBytes;

    @TableField("standard_file_count")
    private Long standardFileCount;

    @TableField("line_file_count")
    private Long lineFileCount;

    @TableField("archive_file_count")
    private Long archiveFileCount;

    @TableField("external_flux_bytes")
    private Long externalFluxBytes;

    @TableField("cdn_flux_bytes")
    private Long cdnFluxBytes;

    @TableField("get_requests")
    private Long getRequests;

    @TableField("put_requests")
    private Long putRequests;

    @TableField("fetch_status")
    private String fetchStatus;

    @TableField("fetch_error")
    private String fetchError;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
