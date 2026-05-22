package com.puyuanmaoshan.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class UserDtos {
    private UserDtos() {}

    // ========== User Profile DTOs ==========

    public record UserProfileResponse(
            Long id,
            String nickname,
            String avatarUrl,
            String mobile,
            String phone,
            String email,
            Boolean wechatBound,
            String wechatOpenid,
            String wechatUnionid,
            String roleCode,
            Integer status
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Nickname is required")
            @Size(min = 1, max = 50, message = "Nickname must be between 1 and 50 characters")
            String nickname,

            String avatarUrl,

            @Email(message = "Invalid email format")
            String email
    ) {}

    public record BindPhoneRequest(
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
            String phone,

            @NotBlank(message = "Verification code is required")
            String verifyCode
    ) {}

    public record BindWechatRequest(
            @NotBlank(message = "WeChat code is required")
            String code
    ) {}

    public record UploadAvatarResponse(
            String url
    ) {}

    // ========== Tenant Member DTOs ==========

    public record TenantMemberResponse(
            Long id,
            Long userId,
            String nickname,
            String avatarUrl,
            String mobile,
            String phone,
            String email,
            String roleCode,
            String roleName,
            String status,
            Long createdBy,
            String creatorName,
            String createdAt
    ) {}

    public record TenantMemberListResponse(
            List<TenantMemberResponse> members,
            Long total
    ) {}

    public record InviteMemberRequest(
            @NotBlank(message = "Phone or email is required")
            @Pattern(regexp = "^(1[3-9]\\d{9}|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$",
                    message = "Invalid phone or email format")
            String contact,

            @NotBlank(message = "Role is required")
            String roleCode,

            String remark
    ) {}

    public record UpdateMemberRoleRequest(
            @NotBlank(message = "Role is required")
            String roleCode
    ) {}

    public record UpdateMemberStatusRequest(
            @NotBlank(message = "Status is required")
            String status
    ) {}

    // ========== Role Configuration DTOs ==========

    public record RoleConfigResponse(
            Long id,
            String roleCode,
            String roleName,
            String description,
            List<String> permissions,
            Integer sortOrder,
            Boolean isSystem
    ) {}

    // ========== Member Audit Log DTOs ==========

    public record MemberAuditLogResponse(
            Long id,
            Long memberUserId,
            String memberName,
            Long operatorUserId,
            String operatorName,
            String action,
            String actionName,
            String oldRole,
            String newRole,
            String oldStatus,
            String newStatus,
            String remark,
            String createdAt
    ) {}

    public record MemberAuditLogListResponse(
            List<MemberAuditLogResponse> logs,
            Long total
    ) {}

    // ========== Pagination ==========

    public record PageRequest(
            @Min(value = 1, message = "Page must be at least 1")
            Integer page,

            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            Integer pageSize,

            String keyword,
            String status,
            String roleCode
    ) {
        public PageRequest {
            if (page == null) page = 1;
            if (pageSize == null) pageSize = 20;
        }
    }

    // ========== Password Change DTOs ==========

    public record ChangePasswordRequest(
            @NotBlank(message = "Old password is required")
            String oldPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
            String newPassword,

            @NotBlank(message = "Confirm password is required")
            String confirmPassword
    ) {}

    // ========== Account Security DTOs ==========

    public record AccountSecurityResponse(
            Boolean hasPassword,
            Boolean hasPhone,
            Boolean hasWechat,
            Boolean hasEmail,
            String lastLoginTime,
            String lastLoginIp,
            Integer loginDeviceCount
    ) {}

    public record SecuritySettingsRequest(
            Boolean loginNotification,
            Boolean paymentNotification,
            Boolean memberChangeNotification
    ) {}

    // ========== Login Log DTOs ==========

    public record LoginLogResponse(
            Long id,
            String loginTime,
            String loginIp,
            String deviceType,
            String deviceInfo,
            String location,
            Boolean isSuccess,
            String failReason
    ) {}

    public record LoginLogListResponse(
            List<LoginLogResponse> logs,
            Long total
    ) {}
}