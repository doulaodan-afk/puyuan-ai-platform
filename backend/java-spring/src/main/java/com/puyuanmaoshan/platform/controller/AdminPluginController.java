package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.PluginLifecycleDtos.*;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.PluginLifecycleService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/plugins")
public class AdminPluginController {
    private final PluginService pluginService;
    private final TenantService tenantService;
    private final TenantPluginService tenantPluginService;
    private final PluginLifecycleService pluginLifecycleService;

    public AdminPluginController(PluginService pluginService,
                                 TenantService tenantService,
                                 TenantPluginService tenantPluginService,
                                 PluginLifecycleService pluginLifecycleService) {
        this.pluginService = pluginService;
        this.tenantService = tenantService;
        this.tenantPluginService = tenantPluginService;
        this.pluginLifecycleService = pluginLifecycleService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                                  @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Page<Plugin> pager = new Page<>(page, pageSize);
        Page<Plugin> pluginPage = pluginService.lambdaQuery()
                .orderByAsc(Plugin::getId)
                .page(pager);

        // 构建 list：兼容新字段 lifecycle_status 和旧字段 status
        List<Map<String, Object>> list = pluginPage.getRecords().stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("plugin_id", item.getPluginId());
            m.put("name", item.getName());
            m.put("version", item.getVersion());
            m.put("billing_type", item.getBillingType());
            m.put("default_token_cost", item.getDefaultTokenCost());
            m.put("description", item.getDescription());
            m.put("icon_url", item.getIconUrl());
            m.put("backend_api", item.getBackendApi());
            m.put("frontend_path", item.getFrontendPath());
            m.put("lifecycle_status", item.getLifecycleStatus() != null ? item.getLifecycleStatus() : "testing");
            m.put("review_status", item.getReviewStatus());
            // gray tenant count
            int grayTenantCount = 0;
            if (item.getGrayTenantIds() != null && !item.getGrayTenantIds().isBlank()) {
                grayTenantCount = item.getGrayTenantIds().split(",").length;
            }
            m.put("gray_tenant_count", grayTenantCount);
            // 新增生命周期字段
            m.put("created_by", item.getCreatedBy());
            m.put("tested_at", item.getTestedAt() != null ? item.getTestedAt().toString() : null);
            m.put("published_at", item.getPublishedAt() != null ? item.getPublishedAt().toString() : null);
            m.put("created_at", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
            m.put("updated_at", item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null);
            return m;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("page", page);
        data.put("page_size", pageSize);
        data.put("total", pluginPage.getTotal());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-list"));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ApiModels.CreatePluginRequest request,
                                                    @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Plugin existing = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, request.pluginId())
                .one();
        if (existing != null) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "plugin_id already exists");
        }

        Plugin plugin = Plugin.builder()
                .pluginId(request.pluginId())
                .name(request.name())
                .version(request.version())
                .backendApi(request.backendApi())
                .frontendEntry(request.frontendEntry())
                .billingType(request.billingType())
                .defaultTokenCost(0)
                .status(1)
                .lifecycleStatus("testing")
                .reviewStatus("pending")
                .build();
        pluginService.save(plugin);

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", request.pluginId());
        data.put("status", "created");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-create"));
    }

    @PatchMapping("/{plugin_id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("plugin_id") String pluginId,
                                                    @RequestBody ApiModels.UpdatePluginRequest request,
                                                    @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        if (request.name() != null) {
            plugin.setName(request.name());
        }
        if (request.version() != null) {
            plugin.setVersion(request.version());
        }
        if (request.backendApi() != null) {
            plugin.setBackendApi(request.backendApi());
        }
        if (request.frontendEntry() != null) {
            plugin.setFrontendEntry(request.frontendEntry());
        }
        if (request.billingType() != null) {
            plugin.setBillingType(request.billingType());
        }
        if (request.status() != null) {
            plugin.setStatus(request.status());
        }
        pluginService.updateById(plugin);

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("status", "updated");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-update"));
    }

    @PostMapping("/{plugin_id}/publish")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> publish(@PathVariable("plugin_id") String pluginId,
                                                     @Valid @RequestBody ApiModels.PublishPluginRequest request,
                                                     @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        String mode = request.mode() == null ? "" : request.mode().toLowerCase(Locale.ROOT);
        List<Long> tenantIds = new ArrayList<>();
        if ("all".equals(mode)) {
            tenantIds = tenantService.lambdaQuery()
                    .eq(Tenant::getStatus, 1)
                    .list()
                    .stream()
                    .map(Tenant::getId)
                    .toList();
        } else if ("partial".equals(mode)) {
            if (request.tenantIds() == null || request.tenantIds().isEmpty()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "tenant_ids required when mode is partial");
            }
            tenantIds = request.tenantIds();
        } else {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "mode must be all or partial");
        }

        for (Long tenantId : tenantIds) {
            TenantPlugin relation = tenantPluginService.lambdaQuery()
                    .eq(TenantPlugin::getTenantId, tenantId)
                    .eq(TenantPlugin::getPluginId, pluginId)
                    .one();
            if (relation == null) {
                relation = TenantPlugin.builder()
                        .tenantId(tenantId)
                        .pluginId(pluginId)
                        .enabled(1)
                        .configJson("{}")
                        .build();
                tenantPluginService.save(relation);
            } else {
                relation.setEnabled(1);
                tenantPluginService.updateById(relation);
            }
        }

        plugin.setReviewStatus("pass");
        pluginService.updateById(plugin);

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("mode", mode);
        data.put("tenant_count", tenantIds.size());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-publish"));
    }

    @DeleteMapping("/{plugin_id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("plugin_id") String pluginId,
                                                    @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        // Only allow delete if lifecycle_status is testing or disabled
        String status = plugin.getLifecycleStatus();
        if (!"testing".equals(status) && !"disabled".equals(status)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only plugins with status 'testing' or 'disabled' can be deleted");
        }

        plugin.setStatus(0);
        pluginService.updateById(plugin);

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("status", "deleted");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-delete"));
    }

    // ========== Lifecycle Management ==========

    @PostMapping("/upload")
    public ApiResponse<UploadPluginResponse> uploadPlugin(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "override_existing", defaultValue = "false") boolean overrideExisting,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        UploadPluginResponse response = pluginLifecycleService.uploadPlugin(file, overrideExisting, operatorId);
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-upload"));
    }

    @PostMapping("/{pluginId}/test")
    public ApiResponse<SandboxTestResponse> sandboxTest(
            @PathVariable("pluginId") String pluginId,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        SandboxTestResponse response = pluginLifecycleService.createSandboxTest(pluginId, operatorId);
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-test"));
    }

    @PostMapping("/{pluginId}/publish-full")
    public ApiResponse<Map<String, Object>> publishFull(
            @PathVariable("pluginId") String pluginId,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        pluginLifecycleService.publishPlugin(pluginId, operatorId);
        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("lifecycle_status", "enabled");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-publish-full"));
    }

    @PostMapping("/{pluginId}/offline")
    public ApiResponse<Map<String, Object>> offline(
            @PathVariable("pluginId") String pluginId,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        pluginLifecycleService.offlinePlugin(pluginId, operatorId);
        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("lifecycle_status", "disabled");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-offline"));
    }

    @PostMapping("/{pluginId}/gray")
    public ApiResponse<GrayPublishResponse> gray(
            @PathVariable("pluginId") String pluginId,
            @Valid @RequestBody GrayPublishRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        GrayPublishResponse response = pluginLifecycleService.grayPublish(pluginId, request.grayTenantIds(), operatorId);
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-gray"));
    }

    @GetMapping("/{pluginId}/status")
    public ApiResponse<PluginStatusResponse> getStatus(
            @PathVariable("pluginId") String pluginId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        PluginStatusResponse response = pluginLifecycleService.getPluginStatus(pluginId);
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-status"));
    }

    @PostMapping("/{pluginId}/deploy")
    public ApiResponse<DeploymentTaskResponse> deploy(
            @PathVariable("pluginId") String pluginId,
            @Valid @RequestBody DeployPluginRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        DeploymentTaskResponse response = pluginLifecycleService.deployBackend(pluginId, request, operatorId);
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-deploy"));
    }

    @PostMapping("/deploy/{taskId}")
    public ApiResponse<Map<String, Object>> executeDeployTask(
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "X-Operator-Id", required = false, defaultValue = "0") Long operatorId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Map<String, Object> result = pluginLifecycleService.executeDeployTask(taskId, operatorId);
        return ApiResponse.ok(result, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-execute-deploy"));
    }

    @GetMapping("/tenants")
    public ApiResponse<Map<String, Object>> listTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Page<Tenant> pager = new Page<>(page, pageSize);
        var query = tenantService.lambdaQuery().eq(Tenant::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            query.like(Tenant::getName, keyword);
        }
        Page<Tenant> tenantPage = query.page(pager);

        List<Map<String, Object>> list = tenantPage.getRecords().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("tenant_id", t.getId());
            m.put("tenant_name", t.getName());
            m.put("tenant_code", t.getTenantCode());
            return m;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("page", page);
        data.put("page_size", pageSize);
        data.put("total", tenantPage.getTotal());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-tenants"));
    }
}