package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/plugins")
public class AdminPluginController {
    private final PluginService pluginService;
    private final TenantService tenantService;
    private final TenantPluginService tenantPluginService;

    public AdminPluginController(PluginService pluginService,
                                 TenantService tenantService,
                                 TenantPluginService tenantPluginService) {
        this.pluginService = pluginService;
        this.tenantService = tenantService;
        this.tenantPluginService = tenantPluginService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                                  @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Page<Plugin> pager = new Page<>(page, pageSize);
        Page<Plugin> pluginPage = pluginService.lambdaQuery()
                .orderByAsc(Plugin::getId)
                .page(pager);

        List<ApiModels.PluginItem> list = pluginPage.getRecords().stream().map(item -> new ApiModels.PluginItem(
                item.getPluginId(),
                item.getName(),
                item.getVersion(),
                item.getBillingType(),
                item.getStatus() != null && item.getStatus() == 1
        )).toList();

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

        plugin.setStatus(0);
        pluginService.updateById(plugin);

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("status", "deleted");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-delete"));
    }
}
