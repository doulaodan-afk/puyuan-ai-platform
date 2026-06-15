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
 * 设计需求 ↔ 面料库关联表
 * 一款设计衣服可以关联多个面料，每个面料来自不同的面料特供商
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("requirement_fabric")
public class RequirementFabric {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设计需求ID */
    @TableField("requirement_id")
    private Long requirementId;

    /** 面料库条目ID (→ fabric_library.id) */
    @TableField("fabric_id")
    private Long fabricId;

    /** 面料特供商用户ID（冗余，便于按供应商查询） */
    @TableField("fabric_supplier_id")
    private Long fabricSupplierId;

    /** 用量（米） */
    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
