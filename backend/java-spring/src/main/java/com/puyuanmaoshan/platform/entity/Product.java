package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("plugin")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plugin_id")
    private String productId;

    @TableField("name")
    private String productName;

    @TableField("version")
    private String version;

    @TableField("backend_api")
    private String backendApi;

    @TableField("frontend_entry")
    private String frontendEntry;

    @TableField("billing_type")
    private String billingType;

    @TableField("default_token_cost")
    private Integer defaultTokenCost;

    @TableField("status")
    private Integer status;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
