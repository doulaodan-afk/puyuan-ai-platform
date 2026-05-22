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
@TableName("tenant")
public class Tenant {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("name")
    private String name;

    @TableField("status")
    private Integer status;

    @TableField("level")
    private String level;

    @TableField("tenant_type")
    private String tenantType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
