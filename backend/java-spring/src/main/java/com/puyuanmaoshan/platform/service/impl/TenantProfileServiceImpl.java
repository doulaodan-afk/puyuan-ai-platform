package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.TenantDtos;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.mapper.TenantMapper;
import com.puyuanmaoshan.platform.service.OssService;
import com.puyuanmaoshan.platform.service.TenantProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantProfileServiceImpl implements TenantProfileService {

    private final TenantMapper tenantMapper;
    private final OssService ossService;

    @Override
    public TenantDtos.TenantProfileDetailResponse getTenantProfile(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户不存在");
        }
        return new TenantDtos.TenantProfileDetailResponse(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getLogoUrl(),
                tenant.getIndustry(),
                tenant.getContactPhone(),
                tenant.getContactEmail(),
                tenant.getAddress(),
                tenant.getDescription()
        );
    }

    @Override
    public void updateTenantProfile(Long tenantId, TenantDtos.UpdateTenantProfileRequest request) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户不存在");
        }

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.logoUrl() != null) {
            tenant.setLogoUrl(request.logoUrl());
        }
        if (request.industry() != null) {
            tenant.setIndustry(request.industry());
        }
        if (request.contactPhone() != null) {
            tenant.setContactPhone(request.contactPhone());
        }
        if (request.contactEmail() != null) {
            tenant.setContactEmail(request.contactEmail());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }
        if (request.description() != null) {
            tenant.setDescription(request.description());
        }

        tenantMapper.updateById(tenant);
    }

    @Override
    public String uploadLogo(Long tenantId, byte[] imageBytes, String fileName) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户不存在");
        }

        String ext = fileName.substring(fileName.lastIndexOf('.'));
        String objectKey = "tenant-logo/" + tenantId + "/" + UUID.randomUUID() + ext;

        String url = ossService.uploadBytes(imageBytes, objectKey);

        tenant.setLogoUrl(url);
        tenantMapper.updateById(tenant);

        return url;
    }
}