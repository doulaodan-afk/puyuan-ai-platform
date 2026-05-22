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
@TableName("tenant_role_config")
public class TenantRoleConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("description")
    private String description;

    @TableField("permissions")
    private String permissions;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_system")
    private Integer isSystem;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}