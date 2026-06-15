package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.FabricLibrary;
import com.puyuanmaoshan.platform.entity.TenantUser;
import com.puyuanmaoshan.platform.mapper.FabricLibraryMapper;
import com.puyuanmaoshan.platform.mapper.TenantUserMapper;
import com.puyuanmaoshan.platform.service.FabricLibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 面料库服务实现 —— 支持面料特供商用户级数据隔离
 *
 * 隔离规则：
 * - 创建面料时写入 tenant_id + creator_id
 * - 查询时：面料特供商只看自己上传的面料（WHERE tenant_id=? AND creator_id=?）
 * - 设计师查看时：可看同工作室所有特供商的面料（WHERE tenant_id=?）
 * - 编辑/下架时：校验 creator_id 是否为当前用户
 */
@Service
public class FabricLibraryServiceImpl implements FabricLibraryService {
    private static final Logger logger = LoggerFactory.getLogger(FabricLibraryServiceImpl.class);

    private final FabricLibraryMapper fabricLibraryMapper;
    private final TenantUserMapper tenantUserMapper;
    private final ObjectMapper objectMapper;

    public FabricLibraryServiceImpl(FabricLibraryMapper fabricLibraryMapper,
                                     TenantUserMapper tenantUserMapper,
                                     ObjectMapper objectMapper) {
        this.fabricLibraryMapper = fabricLibraryMapper;
        this.tenantUserMapper = tenantUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DesignAssistantDtos.FabricLibraryListResponse getFabricLibraryList(
            Long tenantId, Long creatorId, String category, boolean onlyVisible, int page, int size) {
        try {
            LambdaQueryWrapper<FabricLibrary> wrapper = new LambdaQueryWrapper<>();

            // 租户级隔离：只查本工作室的面料
            if (tenantId != null) {
                wrapper.eq(FabricLibrary::getTenantId, tenantId);
            }

            // 用户级隔离：如果传了 creatorId，只查该特供商的面料
            if (creatorId != null) {
                wrapper.eq(FabricLibrary::getCreatorId, creatorId);
            }

            if (category != null && !category.isEmpty()) {
                wrapper.like(FabricLibrary::getCategory, category);
            }
            if (onlyVisible) {
                wrapper.eq(FabricLibrary::getIsVisible, 1);
            }
            wrapper.orderByDesc(FabricLibrary::getCreatedAt);

            IPage<FabricLibrary> pageObj = fabricLibraryMapper.selectPage(new Page<>(page, size), wrapper);

            List<DesignAssistantDtos.FabricInfo> result = new ArrayList<>();
            for (FabricLibrary fabric : pageObj.getRecords()) {
                result.add(toFabricInfo(fabric, fabric.getTenantId()));
            }

            return new DesignAssistantDtos.FabricLibraryListResponse(result, pageObj.getTotal(), page, size);

        } catch (Exception e) {
            logger.error("Get fabric library list failed", e);
            throw new RuntimeException("获取面料列表失败: " + e.getMessage());
        }
    }

    @Override
    public DesignAssistantDtos.FabricInfo getFabricDetail(Long fabricId, Long tenantId, Long userId) {
        try {
            FabricLibrary fabric = fabricLibraryMapper.selectById(fabricId);
            if (fabric == null) {
                throw new RuntimeException("面料不存在");
            }

            // 权限检查：同工作室成员可看，或公开面料可见
            boolean sameTenant = fabric.getTenantId() != null && fabric.getTenantId().equals(tenantId);
            if (!sameTenant && fabric.getIsVisible() != 1) {
                throw new RuntimeException("无权访问此面料");
            }

            return toFabricInfo(fabric, fabric.getTenantId());

        } catch (Exception e) {
            logger.error("Get fabric detail failed", e);
            throw new RuntimeException("获取面料详情失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse createFabric(
            DesignAssistantDtos.CreateFabricRequest request, Long tenantId, Long userId) {
        try {
            // 校验用户是否属于该工作室
            TenantUser tu = tenantUserMapper.selectByUserIdAndTenantId(userId, tenantId);
            if (tu == null) {
                return DesignAssistantDtos.ResponseHelper.error("您不属于此工作室");
            }

            String imagesJson = objectMapper.writeValueAsString(
                    request.images() != null ? request.images() : new ArrayList<>());
            String specsJson = objectMapper.writeValueAsString(
                    request.specs() != null ? request.specs() : new ArrayList<>());

            FabricLibrary fabric = FabricLibrary.builder()
                    .supplierTenantId(tenantId)     // 保留兼容
                    .tenantId(tenantId)             // 所属工作室
                    .creatorId(userId)              // 上传者（面料特供商本人）
                    .name(request.name())
                    .category(request.category())
                    .images(imagesJson)
                    .videoUrl(request.videoUrl())
                    .specs(specsJson)
                    .pricePerMeter(request.pricePerMeter())
                    .stockStatus(request.stockStatus() != null ? request.stockStatus() : "in_stock")
                    .isVisible(1)
                    .createdAt(LocalDateTime.now())
                    .build();

            fabricLibraryMapper.insert(fabric);

            return DesignAssistantDtos.ResponseHelper.success("面料已添加");

        } catch (Exception e) {
            logger.error("Create fabric failed", e);
            return DesignAssistantDtos.ResponseHelper.error("添加失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse updateFabric(
            DesignAssistantDtos.UpdateFabricRequest request, Long tenantId, Long userId) {
        try {
            FabricLibrary fabric = fabricLibraryMapper.selectById(request.id());
            if (fabric == null) {
                return DesignAssistantDtos.ResponseHelper.error("面料不存在");
            }

            // 权限校验：只有上传者本人可以编辑
            if (fabric.getCreatorId() == null || !fabric.getCreatorId().equals(userId)) {
                return DesignAssistantDtos.ResponseHelper.error("无权操作此面料，仅上传者本人可编辑");
            }

            if (request.name() != null) fabric.setName(request.name());
            if (request.category() != null) fabric.setCategory(request.category());
            if (request.images() != null) fabric.setImages(objectMapper.writeValueAsString(request.images()));
            if (request.videoUrl() != null) fabric.setVideoUrl(request.videoUrl());
            if (request.specs() != null) fabric.setSpecs(objectMapper.writeValueAsString(request.specs()));
            if (request.pricePerMeter() != null) fabric.setPricePerMeter(request.pricePerMeter());
            if (request.stockStatus() != null) fabric.setStockStatus(request.stockStatus());
            if (request.isVisible() != null) fabric.setIsVisible(request.isVisible());

            fabric.setUpdatedAt(LocalDateTime.now());
            fabricLibraryMapper.updateById(fabric);

            return DesignAssistantDtos.ResponseHelper.success("面料已更新");

        } catch (Exception e) {
            logger.error("Update fabric failed", e);
            return DesignAssistantDtos.ResponseHelper.error("更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse deleteFabric(Long fabricId, Long tenantId, Long userId) {
        try {
            FabricLibrary fabric = fabricLibraryMapper.selectById(fabricId);
            if (fabric == null) {
                return DesignAssistantDtos.ResponseHelper.error("面料不存在");
            }

            // 权限校验：只有上传者本人可以下架
            if (fabric.getCreatorId() == null || !fabric.getCreatorId().equals(userId)) {
                return DesignAssistantDtos.ResponseHelper.error("无权操作此面料，仅上传者本人可下架");
            }

            fabricLibraryMapper.deleteById(fabricId);

            return DesignAssistantDtos.ResponseHelper.success("面料已下架");

        } catch (Exception e) {
            logger.error("Delete fabric failed", e);
            return DesignAssistantDtos.ResponseHelper.error("删除失败: " + e.getMessage());
        }
    }

    // ====== 内部工具方法 ======

    private DesignAssistantDtos.FabricInfo toFabricInfo(FabricLibrary fabric, Long tenantId) {
        try {
            List<String> images = objectMapper.readValue(
                    fabric.getImages() != null ? fabric.getImages() : "[]",
                    new TypeReference<List<String>>() {});
            Map<String, Object> specs = objectMapper.readValue(
                    fabric.getSpecs() != null ? fabric.getSpecs() : "{}",
                    new TypeReference<Map<String, Object>>() {});

            // 获取上传者名称
            String creatorName = null;
            if (fabric.getCreatorId() != null && tenantId != null) {
                TenantUser tu = tenantUserMapper.selectByUserIdAndTenantId(fabric.getCreatorId(), tenantId);
                if (tu != null) {
                    creatorName = TenantUser.Role.fromCode(tu.getRole()).getName();
                }
            }

            return new DesignAssistantDtos.FabricInfo(
                    fabric.getId(), fabric.getSupplierTenantId(), fabric.getName(),
                    fabric.getCategory(), images, fabric.getVideoUrl(),
                    specs, fabric.getPricePerMeter(), fabric.getStockStatus(),
                    fabric.getIsVisible(), fabric.getCreatedAt(), fabric.getUpdatedAt(),
                    fabric.getCreatorId(), creatorName
            );
        } catch (Exception e) {
            logger.error("Parse fabric info failed", e);
            return null;
        }
    }
}
