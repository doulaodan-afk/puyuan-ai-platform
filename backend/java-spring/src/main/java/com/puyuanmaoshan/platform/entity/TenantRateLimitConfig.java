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

/**
 * 租户限流配置实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tenant_rate_limit_config")
public class TenantRateLimitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID（0 表示默认配置） */
    @TableField("tenant_id")
    private Long tenantId;

    /** 套餐类型：default / premium / enterprise */
    @TableField("plan_type")
    private String planType;

    /** 每分钟最大请求数 */
    @TableField("max_rpm")
    private Integer maxRpm;

    /** 突发系数 */
    @TableField("burst_multiplier")
    private BigDecimal burstMultiplier;

    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
