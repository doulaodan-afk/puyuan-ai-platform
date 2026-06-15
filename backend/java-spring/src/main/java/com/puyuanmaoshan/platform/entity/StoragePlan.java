package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储套餐模板表
 */
@Data
@NoArgsConstructor
@TableName("storage_plan")
public class StoragePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plan_name")
    private String planName;

    @TableField("plan_code")
    private String planCode;

    @TableField("plan_level")
    private Integer planLevel;

    @TableField("storage_quota_gb")
    private Double storageQuotaGb;

    @TableField("max_file_count")
    private Long maxFileCount;

    @TableField("max_file_size_mb")
    private Integer maxFileSizeMb;

    @TableField("monthly_traffic_gb")
    private Double monthlyTrafficGb;

    @TableField("monthly_cdn_traffic_gb")
    private Double monthlyCdnTrafficGb;

    @TableField("monthly_get_requests")
    private Long monthlyGetRequests;

    @TableField("monthly_put_requests")
    private Long monthlyPutRequests;

    @TableField("base_price")
    private java.math.BigDecimal basePrice;

    @TableField("storage_price_per_gb")
    private java.math.BigDecimal storagePricePerGb;

    @TableField("traffic_price_per_gb")
    private java.math.BigDecimal trafficPricePerGb;

    @TableField("request_price_per_10k")
    private java.math.BigDecimal requestPricePer10k;

    @TableField("free_trial_days")
    private Integer freeTrialDays;

    @TableField("status")
    private Boolean status;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("description")
    private String description;

    @TableField("features_json")
    private String featuresJson;

    @TableField(value = "created_at", insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER, updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
