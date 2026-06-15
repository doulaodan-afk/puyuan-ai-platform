package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户存储空间管理相关 DTO
 * 基于七牛云 Kodo Bucket 管理 API:
 * - 创建Bucket: POST /mkbucketv3/<BucketName>/region/<Region>
 * - 删除Bucket: POST /drop/<BucketName>
 * - 获取Bucket列表: GET /buckets
 * - 获取Bucket域名: GET /v2/domains?tbl=<BucketName>
 */
public final class TenantBucketDtos {
    private TenantBucketDtos() {}

    /**
     * 创建存储空间请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBucketRequest {
        @JsonProperty("tenant_id")
        private Long tenantId;

        @JsonProperty("bucket_name")
        private String bucketName;

        @JsonProperty("bucket_region")
        @Builder.Default
        private String bucketRegion = "z0";

        @JsonProperty("bucket_private")
        @Builder.Default
        private Boolean bucketPrivate = false;

        @JsonProperty("plan_id")
        private Long planId;

        @JsonProperty("notes")
        private String notes;
    }

    /**
     * 更新存储空间请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBucketRequest {
        @JsonProperty("bucket_domain")
        private String bucketDomain;

        @JsonProperty("bucket_private")
        private Boolean bucketPrivate;

        @JsonProperty("status")
        private String status;

        @JsonProperty("notes")
        private String notes;
    }

    /**
     * 存储空间响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantBucketResponse {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("tenant_id")
        private Long tenantId;

        @JsonProperty("tenant_name")
        private String tenantName;

        @JsonProperty("bucket_name")
        private String bucketName;

        @JsonProperty("bucket_region")
        private String bucketRegion;

        @JsonProperty("bucket_region_label")
        private String bucketRegionLabel;

        @JsonProperty("bucket_domain")
        private String bucketDomain;

        @JsonProperty("bucket_private")
        private Boolean bucketPrivate;

        @JsonProperty("status")
        private String status;

        @JsonProperty("notes")
        private String notes;

        @JsonProperty("plan_name")
        private String planName;

        @JsonProperty("plan_code")
        private String planCode;

        @JsonProperty("storage_quota_gb")
        private Double storageQuotaGb;

        @JsonProperty("storage_used_gb")
        private Double storageUsedGb;

        @JsonProperty("monthly_traffic_gb")
        private Double monthlyTrafficGb;

        @JsonProperty("traffic_used_gb")
        private Double trafficUsedGb;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
    }

    /**
     * 存储套餐响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoragePlanResponse {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("plan_name")
        private String planName;

        @JsonProperty("plan_code")
        private String planCode;

        @JsonProperty("plan_level")
        private Integer planLevel;

        @JsonProperty("storage_quota_gb")
        private Double storageQuotaGb;

        @JsonProperty("max_file_count")
        private Long maxFileCount;

        @JsonProperty("max_file_size_mb")
        private Integer maxFileSizeMb;

        @JsonProperty("monthly_traffic_gb")
        private Double monthlyTrafficGb;

        @JsonProperty("monthly_cdn_traffic_gb")
        private Double monthlyCdnTrafficGb;

        @JsonProperty("monthly_get_requests")
        private Long monthlyGetRequests;

        @JsonProperty("monthly_put_requests")
        private Long monthlyPutRequests;

        @JsonProperty("base_price")
        private BigDecimal basePrice;

        @JsonProperty("storage_price_per_gb")
        private BigDecimal storagePricePerGb;

        @JsonProperty("traffic_price_per_gb")
        private BigDecimal trafficPricePerGb;

        @JsonProperty("request_price_per_10k")
        private BigDecimal requestPricePer10k;

        @JsonProperty("free_trial_days")
        private Integer freeTrialDays;

        @JsonProperty("status")
        private Boolean status;

        @JsonProperty("sort_order")
        private Integer sortOrder;

        @JsonProperty("description")
        private String description;

        @JsonProperty("features")
        private List<String> features;

        @JsonProperty("tenant_count")
        private Long tenantCount;
    }

    /**
     * 创建/更新套餐请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavePlanRequest {
        @JsonProperty("plan_name")
        private String planName;

        @JsonProperty("plan_code")
        private String planCode;

        @JsonProperty("plan_level")
        private Integer planLevel;

        @JsonProperty("storage_quota_gb")
        private Double storageQuotaGb;

        @JsonProperty("max_file_count")
        private Long maxFileCount;

        @JsonProperty("max_file_size_mb")
        private Integer maxFileSizeMb;

        @JsonProperty("monthly_traffic_gb")
        private Double monthlyTrafficGb;

        @JsonProperty("monthly_cdn_traffic_gb")
        private Double monthlyCdnTrafficGb;

        @JsonProperty("monthly_get_requests")
        private Long monthlyGetRequests;

        @JsonProperty("monthly_put_requests")
        private Long monthlyPutRequests;

        @JsonProperty("base_price")
        private BigDecimal basePrice;

        @JsonProperty("storage_price_per_gb")
        private BigDecimal storagePricePerGb;

        @JsonProperty("traffic_price_per_gb")
        private BigDecimal trafficPricePerGb;

        @JsonProperty("request_price_per_10k")
        private BigDecimal requestPricePer10k;

        @JsonProperty("free_trial_days")
        private Integer freeTrialDays;

        @JsonProperty("status")
        private Boolean status;

        @JsonProperty("sort_order")
        private Integer sortOrder;

        @JsonProperty("description")
        private String description;

        @JsonProperty("features")
        private List<String> features;
    }

    /**
     * 分配套餐请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignPlanRequest {
        @JsonProperty("tenant_id")
        private Long tenantId;

        @JsonProperty("tenant_bucket_id")
        private Long tenantBucketId;

        @JsonProperty("plan_id")
        private Long planId;

        @JsonProperty("auto_renew")
        @Builder.Default
        private Boolean autoRenew = false;
    }

    /**
     * 存储计费记录响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingRecordResponse {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("tenant_id")
        private Long tenantId;

        @JsonProperty("tenant_name")
        private String tenantName;

        @JsonProperty("tenant_bucket_id")
        private Long tenantBucketId;

        @JsonProperty("bucket_name")
        private String bucketName;

        @JsonProperty("bill_period")
        private String billPeriod;

        @JsonProperty("standard_storage_gb")
        private BigDecimal standardStorageGb;

        @JsonProperty("line_storage_gb")
        private BigDecimal lineStorageGb;

        @JsonProperty("archive_storage_gb")
        private BigDecimal archiveStorageGb;

        @JsonProperty("external_traffic_gb")
        private BigDecimal externalTrafficGb;

        @JsonProperty("cdn_traffic_gb")
        private BigDecimal cdnTrafficGb;

        @JsonProperty("get_requests")
        private Long getRequests;

        @JsonProperty("put_requests")
        private Long putRequests;

        @JsonProperty("quota_storage_gb")
        private BigDecimal quotaStorageGb;

        @JsonProperty("quota_traffic_gb")
        private BigDecimal quotaTrafficGb;

        @JsonProperty("base_fee")
        private BigDecimal baseFee;

        @JsonProperty("storage_overage_fee")
        private BigDecimal storageOverageFee;

        @JsonProperty("traffic_overage_fee")
        private BigDecimal trafficOverageFee;

        @JsonProperty("request_overage_fee")
        private BigDecimal requestOverageFee;

        @JsonProperty("total_fee")
        private BigDecimal totalFee;

        @JsonProperty("bill_status")
        private String billStatus;

        @JsonProperty("calculated_at")
        private LocalDateTime calculatedAt;

        @JsonProperty("paid_at")
        private LocalDateTime paidAt;
    }

    /**
     * 计费计算请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculateBillRequest {
        @JsonProperty("tenant_bucket_id")
        private Long tenantBucketId;

        @JsonProperty("bill_period")
        private String billPeriod;
    }

    /**
     * 存储用量快照响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageSnapshotResponse {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("snapshot_date")
        private LocalDate snapshotDate;

        @JsonProperty("standard_storage_gb")
        private Double standardStorageGb;

        @JsonProperty("line_storage_gb")
        private Double lineStorageGb;

        @JsonProperty("archive_storage_gb")
        private Double archiveStorageGb;

        @JsonProperty("standard_file_count")
        private Long standardFileCount;

        @JsonProperty("external_traffic_gb")
        private Double externalTrafficGb;

        @JsonProperty("cdn_traffic_gb")
        private Double cdnTrafficGb;

        @JsonProperty("get_requests")
        private Long getRequests;

        @JsonProperty("put_requests")
        private Long putRequests;

        @JsonProperty("fetch_status")
        private String fetchStatus;
    }

    // ==================== 凭证管理 DTO ====================

    /**
     * 凭证状态响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialsStatusResponse {
        @JsonProperty("configured")
        private boolean configured;

        @JsonProperty("has_access_key")
        private boolean hasAccessKey;

        @JsonProperty("has_secret_key")
        private boolean hasSecretKey;

        @JsonProperty("masked_access_key")
        private String maskedAccessKey;

        @JsonProperty("last_updated_at")
        private LocalDateTime lastUpdatedAt;
    }

    /**
     * 凭证测试/保存请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialsRequest {
        @JsonProperty("access_key")
        private String accessKey;

        @JsonProperty("secret_key")
        private String secretKey;
    }

    /**
     * 凭证测试结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialsTestResult {
        @JsonProperty("success")
        private boolean success;

        @JsonProperty("message")
        private String message;

        @JsonProperty("buckets")
        private List<String> buckets;

        @JsonProperty("bucket_count")
        private int bucketCount;

        @JsonProperty("latency_ms")
        private long latencyMs;
    }

    /**
     * 平台概览统计响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformStorageOverview {
        @JsonProperty("total_buckets")
        private Long totalBuckets;

        @JsonProperty("active_buckets")
        private Long activeBuckets;

        @JsonProperty("total_tenants")
        private Long totalTenants;

        @JsonProperty("total_storage_used_gb")
        private Double totalStorageUsedGb;

        @JsonProperty("total_traffic_gb")
        private Double totalTrafficGb;

        @JsonProperty("total_revenue")
        private BigDecimal totalRevenue;

        @JsonProperty("pending_bills")
        private Long pendingBills;
    }
}
