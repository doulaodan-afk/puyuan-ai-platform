package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 存储计费记录表（月度账单）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("storage_billing_record")
public class StorageBillingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("tenant_bucket_id")
    private Long tenantBucketId;

    @TableField("bill_period")
    private String billPeriod;

    @TableField("standard_storage_gb")
    private BigDecimal standardStorageGb;

    @TableField("line_storage_gb")
    private BigDecimal lineStorageGb;

    @TableField("archive_storage_gb")
    private BigDecimal archiveStorageGb;

    @TableField("standard_file_count")
    private Long standardFileCount;

    @TableField("external_traffic_gb")
    private BigDecimal externalTrafficGb;

    @TableField("cdn_traffic_gb")
    private BigDecimal cdnTrafficGb;

    @TableField("get_requests")
    private Long getRequests;

    @TableField("put_requests")
    private Long putRequests;

    @TableField("quota_storage_gb")
    private BigDecimal quotaStorageGb;

    @TableField("quota_traffic_gb")
    private BigDecimal quotaTrafficGb;

    @TableField("base_fee")
    private BigDecimal baseFee;

    @TableField("storage_overage_fee")
    private BigDecimal storageOverageFee;

    @TableField("traffic_overage_fee")
    private BigDecimal trafficOverageFee;

    @TableField("request_overage_fee")
    private BigDecimal requestOverageFee;

    @TableField("total_fee")
    private BigDecimal totalFee;

    @TableField("bill_status")
    private String billStatus;

    @TableField("calculated_at")
    private LocalDateTime calculatedAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
