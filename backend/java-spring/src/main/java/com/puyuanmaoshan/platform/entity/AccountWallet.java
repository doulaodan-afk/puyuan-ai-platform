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
@TableName("account_wallet")
public class AccountWallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("token_balance")
    private Long tokenBalance;

    @TableField("cash_balance")
    private BigDecimal cashBalance;

    @TableField("frozen_token")
    private Long frozenToken;

    @TableField("status")
    private Integer status;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
