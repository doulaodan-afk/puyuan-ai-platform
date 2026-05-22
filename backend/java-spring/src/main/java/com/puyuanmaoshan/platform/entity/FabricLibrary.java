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
@TableName("fabric_library")
public class FabricLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("supplier_tenant_id")
    private Long supplierTenantId;

    @TableField("name")
    private String name;

    @TableField("category")
    private String category;

    @TableField("images")
    private String images; // JSON string

    @TableField("video_url")
    private String videoUrl;

    @TableField("specs")
    private String specs; // JSON string

    @TableField("price_per_meter")
    private BigDecimal pricePerMeter;

    @TableField("stock_status")
    private String stockStatus; // in_stock/out_of_stock

    @TableField("is_visible")
    private Integer isVisible;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}