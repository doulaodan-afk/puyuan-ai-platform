package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    // ====== 租户企业信息 ======

    public record UpdateTenantProfileRequest(
            @Size(min = 1, max = 100, message = "企业名称长度为1-100个字符")
            @JsonProperty("name") String name,

            @JsonProperty("logo_url") String logoUrl,

            @Size(max = 50, message = "行业分类最长50个字符")
            @JsonProperty("industry") String industry,

            @Size(max = 20, message = "联系电话最长20个字符")
            @JsonProperty("contact_phone") String contactPhone,

            @Email(message = "邮箱格式不正确")
            @JsonProperty("contact_email") String contactEmail,

            @Size(max = 300, message = "地址最长300个字符")
            @JsonProperty("address") String address,

            @Size(max = 500, message = "简介最长500个字符")
            @JsonProperty("description") String description
    ) {}

    public record TenantProfileDetailResponse(
            @JsonProperty("tenant_id") long tenantId,
            @JsonProperty("tenant_code") String tenantCode,
            @JsonProperty("tenant_name") String tenantName,
            @JsonProperty("tenant_status") int tenantStatus,
            @JsonProperty("logo_url") String logoUrl,
            @JsonProperty("industry") String industry,
            @JsonProperty("contact_phone") String contactPhone,
            @JsonProperty("contact_email") String contactEmail,
            @JsonProperty("address") String address,
            @JsonProperty("description") String description
    ) {}

    public record UploadLogoResponse(
            @JsonProperty("url") String url
    ) {}

    // ====== 创建租户/工作室 ======

    public record CreateTenantRequest(
            @NotBlank @Size(min = 2, max = 100, message = "工作室名称长度为2-100个字符")
            @JsonProperty("tenantName") String tenantName
    ) {}

    public record CreateTenantResponse(
            @JsonProperty("tenantId") long tenantId,
            @JsonProperty("tenantName") String tenantName,
            @JsonProperty("tenantCode") String tenantCode,
            @JsonProperty("role") String role
    ) {}
}