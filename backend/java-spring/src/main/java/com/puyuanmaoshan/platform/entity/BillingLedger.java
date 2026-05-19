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
@TableName("billing_ledger")
public class BillingLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("biz_no")
    private String bizNo;

    @TableField("request_id")
    private String requestId;

    @TableField("entry_type")
    private String entryType;

    @TableField("direction")
    private String direction;

    @TableField("token_amount")
    private Long tokenAmount;

    @TableField("cash_amount")
    private BigDecimal cashAmount;

    @TableField("balance_after")
    private Long balanceAfter;

    @TableField("plugin_id")
    private String pluginId;

    @TableField("status")
    private String status;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
