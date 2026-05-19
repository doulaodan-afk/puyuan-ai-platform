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
@TableName("plugin_invoke_log")
public class PluginInvokeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("request_id")
    private String requestId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("plugin_id")
    private String pluginId;

    @TableField("model_vendor")
    private String modelVendor;

    @TableField("token_used")
    private Integer tokenUsed;

    @TableField("latency_ms")
    private Integer latencyMs;

    @TableField("result_code")
    private Integer resultCode;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
