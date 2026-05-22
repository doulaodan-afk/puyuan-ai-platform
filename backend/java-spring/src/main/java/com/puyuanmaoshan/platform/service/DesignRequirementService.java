package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.DesignRequirement;

import java.util.List;

public interface DesignRequirementService {

    // 创建设计需求
    DesignRequirement createRequirement(DesignAssistantDtos.CreateRequirementRequest request, Long tenantId, Long userId);

    // AI 对话
    DesignAssistantDtos.ChatResponse chat(DesignAssistantDtos.ChatRequest request, Long tenantId, Long userId);

    // 生成 AI 总结
    DesignAssistantDtos.SummarizeResponse summarize(Long requirementId, Long tenantId);

    // 设计师确认并发布
    DesignAssistantDtos.CommonResponse confirmRequirement(Long requirementId, Long tenantId, Long userId);

    // 转给设计助理
    DesignAssistantDtos.CommonResponse transferToAssistant(DesignAssistantDtos.TransferToAssistantRequest request, Long tenantId, Long userId);

    // 获取需求列表
    List<DesignAssistantDtos.RequirementListItem> getRequirementList(Long tenantId, String status, int page, int size);

    // 获取需求详情
    DesignAssistantDtos.RequirementDetailResponse getRequirementDetail(Long requirementId, Long tenantId);
}