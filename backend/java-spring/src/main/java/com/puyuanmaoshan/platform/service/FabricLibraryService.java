package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;

import java.util.List;

public interface FabricLibraryService {

    // 获取面料库列表
    DesignAssistantDtos.FabricLibraryListResponse getFabricLibraryList(Long supplierTenantId, String category,
                                                                         boolean onlyVisible, int page, int size);

    // 获取面料详情
    DesignAssistantDtos.FabricInfo getFabricDetail(Long fabricId, Long tenantId);

    // 添加面料
    DesignAssistantDtos.CommonResponse createFabric(DesignAssistantDtos.CreateFabricRequest request, Long tenantId, Long userId);

    // 编辑面料
    DesignAssistantDtos.CommonResponse updateFabric(DesignAssistantDtos.UpdateFabricRequest request, Long tenantId, Long userId);

    // 下架面料
    DesignAssistantDtos.CommonResponse deleteFabric(Long fabricId, Long tenantId, Long userId);
}