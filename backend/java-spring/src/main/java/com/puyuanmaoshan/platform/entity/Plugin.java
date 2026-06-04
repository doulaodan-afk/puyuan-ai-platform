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
public class Plugin {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plugin_id")
    private String pluginId;

    @TableField("name")
    private String name;

    @TableField("version")
    private String version;

    @TableField("backend_api")
    private String backendApi;

    @TableField("frontend_entry")
    private String frontendEntry;

    @TableField("description")
    private String description;

    @TableField("icon_url")
    private String iconUrl;

    @TableField("frontend_path")
    private String frontendPath;

    @TableField("lifecycle_status")
    private String lifecycleStatus;

    @TableField("created_by")
    private Long createdBy;

    @TableField("tested_at")
    private LocalDateTime testedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("gray_tenant_ids")
    private String grayTenantIds;

    @TableField("backend_deploy_config")
    private String backendDeployConfig;

    @TableField("billing_type")
    private String billingType;

    @TableField("default_token_cost")
    private Integer defaultTokenCost;

    @TableField("ai_model")
    private String aiModel;

    @TableField("status")
    private Integer status;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
