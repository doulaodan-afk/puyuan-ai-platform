package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.SupplierDtos;
import com.puyuanmaoshan.platform.service.SupplierRegistrationService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/supplier")
public class AdminSupplierController {
    private final SupplierRegistrationService supplierRegistrationService;

    public AdminSupplierController(SupplierRegistrationService supplierRegistrationService) {
        this.supplierRegistrationService = supplierRegistrationService;
    }

    // 获取入驻申请列表
    @GetMapping("/registrations")
    public ApiResponse<SupplierDtos.RegistrationListResponse> getRegistrations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        SupplierDtos.RegistrationListResponse response = supplierRegistrationService.getRegistrations(page, size);
        return ApiResponse.ok(response, requestId);
    }

    // 审核入驻申请
    @PutMapping("/registration/{id}/review")
    public ApiResponse<SupplierDtos.ReviewResponse> reviewRegistration(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDtos.ReviewRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        Long adminId = RequestContextUtil.parseUserId(userId);
        SupplierDtos.ReviewResponse response = supplierRegistrationService.reviewRegistration(id, request, adminId);
        return ApiResponse.ok(response, requestId);
    }
}