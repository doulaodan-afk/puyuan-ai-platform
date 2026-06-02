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
@TableName("plugin_deployment_task")
public class PluginDeploymentTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plugin_id")
    private String pluginId;

    @TableField("docker_image")
    private String dockerImage;

    @TableField("env_vars")
    private String envVars;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}