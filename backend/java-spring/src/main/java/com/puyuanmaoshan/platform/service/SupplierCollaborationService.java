package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.SupplierDtos;

public interface SupplierCollaborationService {

    // 获取可合作的面料商列表
    SupplierDtos.SupplierListResponse getAvailableSuppliers(Long tenantId, int page, int size);

    // 邀请面料商合作
    SupplierDtos.CommonResponse inviteCollaboration(Long tenantId, Long userId, SupplierDtos.InviteCollaborationRequest request);

    // 获取合作列表
    SupplierDtos.CollaborationListResponse getCollaborations(Long tenantId, String status, int page, int size);

    // 面料商响应合作邀请
    SupplierDtos.CommonResponse respondCollaboration(Long collaborationId, Long userId, SupplierDtos.RespondCollaborationRequest request);

    // 屏蔽合作
    SupplierDtos.CommonResponse blockCollaboration(Long collaborationId, Long tenantId, Long userId, SupplierDtos.BlockCollaborationRequest request);
}