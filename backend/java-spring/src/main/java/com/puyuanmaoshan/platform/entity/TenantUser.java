package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tenant_user")
public class TenantUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    /**
     * 角色：boss/designer/design_assistant/pattern_maker
     */
    @TableField("role")
    private String role;

    /**
     * 邀请人 user_id
     */
    @TableField("invited_by")
    private Long invitedBy;

    /**
     * 状态：active/inactive
     */
    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 角色枚举
     */
    public enum Role {
        BOSS("boss", "老板"),
        DESIGNER("designer", "设计师"),
        DESIGN_ASSISTANT("design_assistant", "设计助理"),
        PATTERN_MAKER("pattern_maker", "版师"),
        OPERATOR("operator", "运营"),
        VIEWER("viewer", "查看者");

        private final String code;
        private final String name;

        Role(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public static Role fromCode(String code) {
            for (Role role : values()) {
                if (role.code.equals(code)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown role: " + code);
        }
    }

    /**
     * 状态枚举
     */
    public enum Status {
        ACTIVE("active", "活跃"),
        INACTIVE("inactive", "停用");

        private final String code;
        private final String name;

        Status(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 检查是否为老板
     */
    public boolean isBoss() {
        return Role.BOSS.code.equals(this.role);
    }

    /**
     * 检查是否有权限（老板拥有所有权限）
     */
    public boolean hasPermission(String requiredRole) {
        if (isBoss()) {
            return true;
        }
        return this.role.equals(requiredRole);
    }

    /**
     * 检查是否活跃
     */
    public boolean isActive() {
        return Status.ACTIVE.code.equals(this.status);
    }
}