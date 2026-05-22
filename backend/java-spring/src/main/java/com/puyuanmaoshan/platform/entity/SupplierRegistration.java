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
@TableName("supplier_registration")
public class SupplierRegistration {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_name")
    private String companyName;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_mobile")
    private String contactMobile;

    @TableField("business_license")
    private String businessLicense;

    @TableField("address")
    private String address;

    @TableField("fabric_categories")
    private String fabricCategories; // JSON string

    @TableField("description")
    private String description;

    @TableField("status")
    private String status; // pending/approved/rejected

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("admin_id")
    private Long adminId;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}