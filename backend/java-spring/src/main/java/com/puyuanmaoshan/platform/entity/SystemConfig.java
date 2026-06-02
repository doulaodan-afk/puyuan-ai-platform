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
@TableName("system_config")
public class SystemConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_group")
    private String configGroup;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
