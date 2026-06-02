package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.SupplierDtos;

public interface SupplierRegistrationService {

    // 面料商提交入驻申请
    SupplierDtos.RegisterResponse register(SupplierDtos.RegisterRequest request);

    // 获取入驻申请列表
    SupplierDtos.RegistrationListResponse getRegistrations(int page, int size);

    // 审核入驻申请
    SupplierDtos.ReviewResponse reviewRegistration(Long registrationId, SupplierDtos.ReviewRequest request, Long adminId);
}