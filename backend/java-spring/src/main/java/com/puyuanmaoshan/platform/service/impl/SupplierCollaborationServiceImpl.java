package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.dto.SupplierDtos;
import com.puyuanmaoshan.platform.entity.SupplierCollaboration;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.mapper.SupplierCollaborationMapper;
import com.puyuanmaoshan.platform.service.SupplierCollaborationService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupplierCollaborationServiceImpl extends ServiceImpl<SupplierCollaborationMapper, SupplierCollaboration>
    implements SupplierCollaborationService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierCollaborationServiceImpl.class);

    private final TenantService tenantService;
    private final UserAccountService userAccountService;

    public SupplierCollaborationServiceImpl(TenantService tenantService,
                                           UserAccountService userAccountService) {
        this.tenantService = tenantService;
        this.userAccountService = userAccountService;
    }

    @Override
    public SupplierDtos.SupplierListResponse getAvailableSuppliers(Long tenantId, int page, int size) {
        try {
            // 获取所有供应商租户
            LambdaQueryWrapper<Tenant> tenantWrapper = new LambdaQueryWrapper<>();
            tenantWrapper.eq(Tenant::getTenantType, "supplier");
            tenantWrapper.eq(Tenant::getStatus, 1);

            List<Tenant> suppliers = tenantService.list(tenantWrapper);

            // 排除已合作的供应商
            List<SupplierCollaboration> existingCollaborations = this.list(
                new LambdaQueryWrapper<SupplierCollaboration>()
                    .eq(SupplierCollaboration::getMerchantTenantId, tenantId)
            );

            Set<Long> existingSupplierIds = existingCollaborations.stream()
                .map(SupplierCollaboration::getSupplierTenantId)
                .collect(Collectors.toSet());

            List<SupplierDtos.SupplierInfo> result = new ArrayList<>();
            for (Tenant supplier : suppliers) {
                if (!existingSupplierIds.contains(supplier.getId())) {
                    result.add(new SupplierDtos.SupplierInfo(
                        supplier.getId(),
                        supplier.getName(),
                        supplier.getTenantCode(),
                        List.of(), // 面料品类暂时为空，后续可从 fabric_library 表获取
                        supplier.getCreatedAt()
                    ));
                }
            }

            // 分页处理
            long total = result.size();
            int offset = (page - 1) * size;
            List<SupplierDtos.SupplierInfo> pagedResult = result.stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());

            return new SupplierDtos.SupplierListResponse(pagedResult, total);

        } catch (Exception e) {
            logger.error("Get available suppliers failed", e);
            throw new RuntimeException("获取可合作供应商列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SupplierDtos.CommonResponse inviteCollaboration(Long tenantId, Long userId, SupplierDtos.InviteCollaborationRequest request) {
        try {
            // 检查是否已存在合作记录
            SupplierCollaboration existing = this.getOne(
                new LambdaQueryWrapper<SupplierCollaboration>()
                    .eq(SupplierCollaboration::getMerchantTenantId, tenantId)
                    .eq(SupplierCollaboration::getSupplierTenantId, request.supplierTenantId())
            );

            if (existing != null) {
                return SupplierDtos.ResponseHelper.error("已存在合作记录");
            }

            SupplierCollaboration collaboration = SupplierCollaboration.builder()
                .merchantTenantId(tenantId)
                .supplierTenantId(request.supplierTenantId())
                .status("pending")
                .invitedBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

            this.save(collaboration);

            return SupplierDtos.ResponseHelper.success("合作邀请已发送");

        } catch (Exception e) {
            logger.error("Invite collaboration failed", e);
            throw new RuntimeException("发送合作邀请失败: " + e.getMessage());
        }
    }

    @Override
    public SupplierDtos.CollaborationListResponse getCollaborations(Long tenantId, String status, int page, int size) {
        try {
            LambdaQueryWrapper<SupplierCollaboration> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SupplierCollaboration::getMerchantTenantId, tenantId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(SupplierCollaboration::getStatus, status);
            }
            wrapper.orderByDesc(SupplierCollaboration::getCreatedAt);

            long total = this.count(wrapper);
            int offset = (page - 1) * size;
            wrapper.last("LIMIT " + offset + ", " + size);

            List<SupplierCollaboration> collaborations = this.list(wrapper);

            List<SupplierDtos.CollaborationInfo> result = new ArrayList<>();
            for (SupplierCollaboration collaboration : collaborations) {
                // 获取供应商名称
                Tenant supplier = tenantService.getById(collaboration.getSupplierTenantId());
                String supplierName = supplier != null ? supplier.getName() : "未知供应商";

                // 获取邀请人姓名
                UserAccount inviter = userAccountService.getById(collaboration.getInvitedBy());
                String inviterName = inviter != null ? inviter.getNickname() : "未知用户";

                // 获取响应人姓名
                String responderName = null;
                if (collaboration.getRespondedBy() != null && collaboration.getRespondedBy() > 0) {
                    UserAccount responder = userAccountService.getById(collaboration.getRespondedBy());
                    responderName = responder != null ? responder.getNickname() : "未知用户";
                }

                result.add(new SupplierDtos.CollaborationInfo(
                    collaboration.getId(),
                    collaboration.getMerchantTenantId(),
                    collaboration.getSupplierTenantId(),
                    supplierName,
                    collaboration.getStatus(),
                    collaboration.getInvitedBy(),
                    inviterName,
                    collaboration.getRespondedBy(),
                    responderName,
                    collaboration.getRespondedAt(),
                    collaboration.getCreatedAt()
                ));
            }

            return new SupplierDtos.CollaborationListResponse(result, total);

        } catch (Exception e) {
            logger.error("Get collaborations failed", e);
            throw new RuntimeException("获取合作列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SupplierDtos.CommonResponse respondCollaboration(Long collaborationId, Long userId, SupplierDtos.RespondCollaborationRequest request) {
        try {
            SupplierCollaboration collaboration = this.getById(collaborationId);
            if (collaboration == null) {
                throw new RuntimeException("合作记录不存在");
            }

            if (!"pending".equals(collaboration.getStatus())) {
                throw new RuntimeException("该合作记录已处理");
            }

            if ("accept".equals(request.action())) {
                collaboration.setStatus("accepted");
            } else if ("reject".equals(request.action())) {
                collaboration.setStatus("rejected");
            } else {
                throw new RuntimeException("无效的响应操作");
            }

            collaboration.setRespondedBy(userId);
            collaboration.setRespondedAt(LocalDateTime.now());
            this.updateById(collaboration);

            return SupplierDtos.ResponseHelper.success("已响应合作邀请");

        } catch (Exception e) {
            logger.error("Respond collaboration failed", e);
            throw new RuntimeException("响应合作邀请失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SupplierDtos.CommonResponse blockCollaboration(Long collaborationId, Long tenantId, Long userId, SupplierDtos.BlockCollaborationRequest request) {
        try {
            SupplierCollaboration collaboration = this.getById(collaborationId);
            if (collaboration == null || !collaboration.getMerchantTenantId().equals(tenantId)) {
                throw new RuntimeException("合作记录不存在或无权操作");
            }

            if ("blocked".equals(collaboration.getStatus())) {
                return SupplierDtos.ResponseHelper.success("已屏蔽该供应商");
            }

            collaboration.setStatus("blocked");
            collaboration.setBlockReason(request.reason());
            this.updateById(collaboration);

            return SupplierDtos.ResponseHelper.success("已屏蔽该供应商");

        } catch (Exception e) {
            logger.error("Block collaboration failed", e);
            throw new RuntimeException("屏蔽供应商失败: " + e.getMessage());
        }
    }
}