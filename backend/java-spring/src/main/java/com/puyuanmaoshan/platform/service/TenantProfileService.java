package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.TenantDtos;

public interface TenantProfileService {

    TenantDtos.TenantProfileDetailResponse getTenantProfile(Long tenantId);

    void updateTenantProfile(Long tenantId, TenantDtos.UpdateTenantProfileRequest request);

    String uploadLogo(Long tenantId, byte[] imageBytes, String fileName);
}