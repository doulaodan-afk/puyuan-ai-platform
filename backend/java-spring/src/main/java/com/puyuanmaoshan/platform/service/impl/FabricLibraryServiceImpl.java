package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.FabricLibrary;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.mapper.FabricLibraryMapper;
import com.puyuanmaoshan.platform.mapper.TenantMapper;
import com.puyuanmaoshan.platform.service.FabricLibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FabricLibraryServiceImpl implements FabricLibraryService {
    private static final Logger logger = LoggerFactory.getLogger(FabricLibraryServiceImpl.class);

    private final FabricLibraryMapper fabricLibraryMapper;
    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;

    public FabricLibraryServiceImpl(FabricLibraryMapper fabricLibraryMapper,
                                     TenantMapper tenantMapper,
                                     ObjectMapper objectMapper) {
        this.fabricLibraryMapper = fabricLibraryMapper;
        this.tenantMapper = tenantMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DesignAssistantDtos.FabricLibraryListResponse getFabricLibraryList(Long supplierTenantId, String category,
                                                                                 boolean onlyVisible, int page, int size) {
        try {
            LambdaQueryWrapper<FabricLibrary> wrapper = new LambdaQueryWrapper<>();

            if (supplierTenantId != null) {
                wrapper.eq(FabricLibrary::getSupplierTenantId, supplierTenantId);
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
                List<String> images = objectMapper.readValue(
                    fabric.getImages() != null ? fabric.getImages() : "[]",
                    new TypeReference<List<String>>() {}
                );
                Map<String, Object> specs = objectMapper.readValue(
                    fabric.getSpecs() != null ? fabric.getSpecs() : "{}",
                    new TypeReference<Map<String, Object>>() {}
                );

                result.add(new DesignAssistantDtos.FabricInfo(
                    fabric.getId(), fabric.getSupplierTenantId(), fabric.getName(),
                    fabric.getCategory(), images, fabric.getVideoUrl(),
                    specs, fabric.getPricePerMeter(), fabric.getStockStatus(),
                    fabric.getIsVisible(), fabric.getCreatedAt(), fabric.getUpdatedAt()
                ));
            }

            return new DesignAssistantDtos.FabricLibraryListResponse(result, pageObj.getTotal(), page, size);

        } catch (Exception e) {
            logger.error("Get fabric library list failed", e);
            throw new RuntimeException("获取面料列表失败: " + e.getMessage());
        }
    }

    @Override
    public DesignAssistantDtos.FabricInfo getFabricDetail(Long fabricId, Long tenantId) {
        try {
            FabricLibrary fabric = fabricLibraryMapper.selectById(fabricId);
            if (fabric == null) {
                throw new RuntimeException("面料不存在");
            }

            // 检查权限：只能查看自己的面料或公开的面料
            if (!fabric.getSupplierTenantId().equals(tenantId) && fabric.getIsVisible() != 1) {
                throw new RuntimeException("无权访问此面料");
            }

            List<String> images = objectMapper.readValue(
                fabric.getImages() != null ? fabric.getImages() : "[]",
                new TypeReference<List<String>>() {}
            );
            Map<String, Object> specs = objectMapper.readValue(
                fabric.getSpecs() != null ? fabric.getSpecs() : "{}",
                new TypeReference<Map<String, Object>>() {}
            );

            return new DesignAssistantDtos.FabricInfo(
                fabric.getId(), fabric.getSupplierTenantId(), fabric.getName(),
                fabric.getCategory(), images, fabric.getVideoUrl(),
                specs, fabric.getPricePerMeter(), fabric.getStockStatus(),
                fabric.getIsVisible(), fabric.getCreatedAt(), fabric.getUpdatedAt()
            );

        } catch (Exception e) {
            logger.error("Get fabric detail failed", e);
            throw new RuntimeException("获取面料详情失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse createFabric(DesignAssistantDtos.CreateFabricRequest request, Long tenantId, Long userId) {
        try {
            // 检查租户类型
            Tenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                return DesignAssistantDtos.ResponseHelper.error("租户不存在");
            }
            // 只有面料商可以创建面料
            if (!"supplier".equals(tenant.getTenantType())) {
                return DesignAssistantDtos.ResponseHelper.error("只有面料商可以添加面料");
            }

            String imagesJson = objectMapper.writeValueAsString(request.images() != null ? request.images() : new ArrayList<>());
            String specsJson = objectMapper.writeValueAsString(request.specs() != null ? request.specs() : new ArrayList<>());

            FabricLibrary fabric = FabricLibrary.builder()
                    .supplierTenantId(tenantId)
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
    public DesignAssistantDtos.CommonResponse updateFabric(DesignAssistantDtos.UpdateFabricRequest request, Long tenantId, Long userId) {
        try {
            FabricLibrary fabric = fabricLibraryMapper.selectById(request.id());
            if (fabric == null) {
                return DesignAssistantDtos.ResponseHelper.error("面料不存在");
            }
            if (!fabric.getSupplierTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("无权操作此面料");
            }

            if (request.name() != null) {
                fabric.setName(request.name());
            }
            if (request.category() != null) {
                fabric.setCategory(request.category());
            }
            if (request.images() != null) {
                fabric.setImages(objectMapper.writeValueAsString(request.images()));
            }
            if (request.videoUrl() != null) {
                fabric.setVideoUrl(request.videoUrl());
            }
            if (request.specs() != null) {
                fabric.setSpecs(objectMapper.writeValueAsString(request.specs()));
            }
            if (request.pricePerMeter() != null) {
                fabric.setPricePerMeter(request.pricePerMeter());
            }
            if (request.stockStatus() != null) {
                fabric.setStockStatus(request.stockStatus());
            }
            if (request.isVisible() != null) {
                fabric.setIsVisible(request.isVisible());
            }

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
            if (!fabric.getSupplierTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("无权操作此面料");
            }

            fabricLibraryMapper.deleteById(fabricId);

            return DesignAssistantDtos.ResponseHelper.success("面料已下架");

        } catch (Exception e) {
            logger.error("Delete fabric failed", e);
            return DesignAssistantDtos.ResponseHelper.error("删除失败: " + e.getMessage());
        }
    }
}