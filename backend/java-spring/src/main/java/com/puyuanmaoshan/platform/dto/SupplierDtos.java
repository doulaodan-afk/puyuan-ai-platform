package com.puyuanmaoshan.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

public final class SupplierDtos {
    private SupplierDtos() {}

    // ====== 入驻申请 ======
    public record RegisterRequest(
            @NotBlank String companyName,
            @NotBlank String contactName,
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String contactMobile,
            String businessLicense,
            String address,
            List<String> fabricCategories,
            String description
    ) {}

    public record RegisterResponse(
            Long registrationId,
            String status
    ) {}

    // ====== 入驻申请列表 ======
    public record RegistrationItem(
            Long id,
            String companyName,
            String contactName,
            String contactMobile,
            List<String> fabricCategories,
            String status,
            LocalDateTime createdAt
    ) {}

    public record RegistrationListResponse(
            List<RegistrationItem> registrations,
            long total
    ) {}

    // ====== 审核申请 ======
    public record ReviewRequest(
            @NotBlank String action, // approve/reject
            String rejectReason
    ) {}

    public record ReviewResponse(
            Long tenantId,
            Long userId,
            String message
    ) {}

    // ====== 合作管理 ======
    public record SupplierInfo(
            Long tenantId,
            String tenantName,
            String tenantCode,
            List<String> fabricCategories,
            LocalDateTime createdAt
    ) {}

    public record SupplierListResponse(
            List<SupplierInfo> suppliers,
            long total
    ) {}

    public record InviteCollaborationRequest(
            @NotNull Long supplierTenantId
    ) {}

    public record CollaborationInfo(
            Long id,
            Long merchantTenantId,
            Long supplierTenantId,
            String supplierName,
            String status,
            Long invitedBy,
            String inviterName,
            Long respondedBy,
            String responderName,
            LocalDateTime respondedAt,
            LocalDateTime createdAt
    ) {}

    public record CollaborationListResponse(
            List<CollaborationInfo> collaborations,
            long total
    ) {}

    public record RespondCollaborationRequest(
            @NotBlank String action, // accept/reject
            String reason
    ) {}

    public record BlockCollaborationRequest(
            String reason
    ) {}

    // ====== 通用响应 ======
    public record CommonResponse(
            boolean success,
            String message
    ) {}

    public static class ResponseHelper {
        public static CommonResponse success(String message) {
            return new CommonResponse(true, message);
        }

        public static CommonResponse error(String message) {
            return new CommonResponse(false, message);
        }
    }
}