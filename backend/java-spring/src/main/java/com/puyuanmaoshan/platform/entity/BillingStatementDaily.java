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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("billing_statement_daily")
public class BillingStatementDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("token_in")
    private Long tokenIn;

    @TableField("token_out")
    private Long tokenOut;

    @TableField("call_count")
    private Integer callCount;

    @TableField("amount_recharge")
    private BigDecimal amountRecharge;

    @TableField("amount_refund")
    private BigDecimal amountRefund;

    @TableField("generated_at")
    private LocalDateTime generatedAt;
}
