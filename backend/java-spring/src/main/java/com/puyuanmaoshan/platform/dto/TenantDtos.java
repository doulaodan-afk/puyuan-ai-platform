package com.puyuanmaoshan.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class TenantDtos {
    private TenantDtos() {}

    // ====== 登录响应（多租户支持） ======

    public record LoginResponse(
            String accessToken,
            long expiresIn,
            long userId,
            String mobile,
            String nickname,
            List<UserTenant> tenants
    ) {}

    public record UserTenant(
            long tenantId,
            String tenantName,
            String tenantCode,
            String role,
            boolean isDefault
    ) {}

    // ====== 成员信息 ======

    public record MemberInfo(
            long userId,
            String mobile,
            String nickname,
            String avatarUrl,
            String role,
            String status,
            Long invitedBy,
            String inviterName,
            LocalDateTime joinedAt
    ) {}

    public record MemberListResponse(
            List<MemberInfo> members,
            long total,
            int memberCount
    ) {}

    // ====== 邀请成员 ======

    public record InviteMemberRequest(
            @NotBlank String mobile,
            @NotBlank String role  // designer/design_assistant/pattern_maker
    ) {}

    // ====== 修改角色 ======

    public record UpdateRoleRequest(
            @NotBlank String role
    ) {}

    // ====== 通用响应 ======

    public record CommonResponse(
            boolean success,
            String message,
            Object data
    ) {}

    public static class ResponseHelper {
        public static CommonResponse success() {
            return new CommonResponse(true, "操作成功", null);
        }

        public static CommonResponse success(String message) {
            return new CommonResponse(true, message, null);
        }

        public static CommonResponse success(Object data) {
            return new CommonResponse(true, "操作成功", data);
        }

        public static CommonResponse success(String message, Object data) {
            return new CommonResponse(true, message, data);
        }

        public static CommonResponse error(String message) {
            return new CommonResponse(false, message, null);
        }
    }

    // ====== 切换租户 ======

    public record SwitchTenantRequest(
            @NotNull Long tenantId
    ) {}

    public record SwitchTenantResponse(
            String accessToken,
            long expiresIn,
            long userId,
            long tenantId,
            String tenantName,
            String role
    ) {}
}