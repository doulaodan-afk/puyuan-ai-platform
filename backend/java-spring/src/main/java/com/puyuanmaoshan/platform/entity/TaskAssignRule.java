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
@TableName("task_assign_rule")
public class TaskAssignRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_name")
    private String ruleName;

    @TableField("keyword")
    private String keyword;

    @TableField("target_tenant_id")
    private Long targetTenantId;

    @TableField("task_type")
    private String taskType; // fabric/pattern

    @TableField("priority")
    private Integer priority;

    @TableField("enabled")
    private Integer enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}