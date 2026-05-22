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
@TableName("member_role_audit_log")
public class MemberRoleAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("member_user_id")
    private Long memberUserId;

    @TableField("operator_user_id")
    private Long operatorUserId;

    @TableField("action")
    private String action;

    @TableField("old_role")
    private String oldRole;

    @TableField("new_role")
    private String newRole;

    @TableField("old_status")
    private String oldStatus;

    @TableField("new_status")
    private String newStatus;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public enum Action {
        INVITE("invite", "邀请成员"),
        ADD_ROLE("add_role", "添加角色"),
        REMOVE_ROLE("remove_role", "移除角色"),
        REMOVE_MEMBER("remove_member", "移除成员"),
        ENABLE("enable", "启用成员"),
        DISABLE("disable", "禁用成员");

        private final String code;
        private final String name;

        Action(String code, String name) {
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
}