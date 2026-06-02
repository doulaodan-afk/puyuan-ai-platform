package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.SupplierDtos;
import com.puyuanmaoshan.platform.service.SupplierCollaborationService;
import com.puyuanmaoshan.platform.service.SupplierRegistrationService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier")
public class SupplierController {
    private final SupplierRegistrationService supplierRegistrationService;
    private final SupplierCollaborationService supplierCollaborationService;

    public SupplierController(SupplierRegistrationService supplierRegistrationService,
                               SupplierCollaborationService supplierCollaborationService) {
        this.supplierRegistrationService = supplierRegistrationService;
        this.supplierCollaborationService = supplierCollaborationService;
    }

    // 面料商入驻申请（无需登录）
    @PostMapping("/register")
    public ApiResponse<SupplierDtos.RegisterResponse> register(
            @Valid @RequestBody SupplierDtos.RegisterRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        SupplierDtos.RegisterResponse response = supplierRegistrationService.register(request);
        return ApiResponse.ok(response, requestId);
    }

    // 获取可合作的供应商列表
    @GetMapping("/available")
    public ApiResponse<SupplierDtos.SupplierListResponse> getAvailableSuppliers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        Long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        SupplierDtos.SupplierListResponse response = supplierCollaborationService.getAvailableSuppliers(parsedTenantId, page, size);
        return ApiResponse.ok(response, requestId);
    }

    // 邀请面料商合作
    @PostMapping("/collaboration/invite")
    public ApiResponse<SupplierDtos.CommonResponse> inviteCollaboration(
            @Valid @RequestBody SupplierDtos.InviteCollaborationRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        Long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Long parsedUserId = RequestContextUtil.parseUserId(userId);
        SupplierDtos.CommonResponse response = supplierCollaborationService.inviteCollaboration(parsedTenantId, parsedUserId, request);
        return ApiResponse.ok(response, requestId);
    }

    // 获取合作列表
    @GetMapping("/collaboration/list")
    public ApiResponse<SupplierDtos.CollaborationListResponse> getCollaborations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        Long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        SupplierDtos.CollaborationListResponse response = supplierCollaborationService.getCollaborations(parsedTenantId, status, page, size);
        return ApiResponse.ok(response, requestId);
    }

    // 面料商响应合作邀请
    @PutMapping("/collaboration/respond/{id}")
    public ApiResponse<SupplierDtos.CommonResponse> respondCollaboration(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDtos.RespondCollaborationRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        Long parsedUserId = RequestContextUtil.parseUserId(userId);
        SupplierDtos.CommonResponse response = supplierCollaborationService.respondCollaboration(id, parsedUserId, request);
        return ApiResponse.ok(response, requestId);
    }

    // 屏蔽合作
    @PutMapping("/collaboration/block/{id}")
    public ApiResponse<SupplierDtos.CommonResponse> blockCollaboration(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDtos.BlockCollaborationRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        Long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Long parsedUserId = RequestContextUtil.parseUserId(userId);
        SupplierDtos.CommonResponse response = supplierCollaborationService.blockCollaboration(id, parsedTenantId, parsedUserId, request);
        return ApiResponse.ok(response, requestId);
    }
}