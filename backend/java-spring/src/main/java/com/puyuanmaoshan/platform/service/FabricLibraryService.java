package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;

public interface FabricLibraryService {

    /**
     * 获取面料库列表
     * @param tenantId 所属工作室ID（必填，用于租户级隔离）
     * @param creatorId 面料特供商用户ID（可选，用于用户级隔离；为null则返回租户内所有面料）
     */
    DesignAssistantDtos.FabricLibraryListResponse getFabricLibraryList(
            Long tenantId, Long creatorId, String category, boolean onlyVisible, int page, int size);

    /** 获取面料详情 */
    DesignAssistantDtos.FabricInfo getFabricDetail(Long fabricId, Long tenantId, Long userId);

    /** 添加面料（面料特供商操作） */
    DesignAssistantDtos.CommonResponse createFabric(
            DesignAssistantDtos.CreateFabricRequest request, Long tenantId, Long userId);

    /** 编辑面料（仅上传者本人可操作） */
    DesignAssistantDtos.CommonResponse updateFabric(
            DesignAssistantDtos.UpdateFabricRequest request, Long tenantId, Long userId);

    /** 下架面料（仅上传者本人可操作） */
    DesignAssistantDtos.CommonResponse deleteFabric(Long fabricId, Long tenantId, Long userId);
}
