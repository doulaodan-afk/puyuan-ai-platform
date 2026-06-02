package com.puyuanmaoshan.platform.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.service.PluginInvokeWorkflowService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/plugins")
public class PluginController {
    private final PluginService pluginService;
    private final TenantPluginService tenantPluginService;
    private final PluginInvokeWorkflowService pluginInvokeWorkflowService;
    private final AuditLogService auditLogService;

    public PluginController(PluginService pluginService,
                            TenantPluginService tenantPluginService,
                            PluginInvokeWorkflowService pluginInvokeWorkflowService,
                            AuditLogService auditLogService) {
        this.pluginService = pluginService;
        this.tenantPluginService = tenantPluginService;
        this.pluginInvokeWorkflowService = pluginInvokeWorkflowService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<ApiModels.PluginItem>> list(@RequestHeader("X-Tenant-Id") String tenantId,
                                                         @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        List<Plugin> plugins = pluginService.lambdaQuery()
                .eq(Plugin::getStatus, 1)
                .orderByAsc(Plugin::getId)
                .list();

        Map<String, TenantPlugin> tenantPluginMap = tenantPluginService.lambdaQuery()
                .eq(TenantPlugin::getTenantId, parsedTenantId)
                .list()
                .stream()
                .collect(Collectors.toMap(TenantPlugin::getPluginId, Function.identity(), (a, b) -> a));

        List<ApiModels.PluginItem> data = plugins.stream().map(plugin -> {
            TenantPlugin tenantPlugin = tenantPluginMap.get(plugin.getPluginId());
            boolean enabled = tenantPlugin != null && Objects.equals(tenantPlugin.getEnabled(), 1);
            return new ApiModels.PluginItem(
                    plugin.getPluginId(),
                    plugin.getName(),
                    plugin.getVersion(),
                    plugin.getBillingType(),
                    enabled
            );
        }).toList();

        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-plugin-list"));
    }

    /**
     * 获取租户可见的插件（商家端应用市场）
     * 只返回 lifecycle_status = 'enabled' 的插件
     * 如果当前租户在灰度名单中，也返回 lifecycle_status = 'gray' 的插件
     */
    @GetMapping("/visible")
    public ApiResponse<List<VisiblePluginItem>> getVisiblePlugins(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        // 查询当前租户已启用/停用的插件记录
        Map<String, TenantPlugin> tenantPluginMap = tenantPluginService.lambdaQuery()
                .eq(TenantPlugin::getTenantId, parsedTenantId)
                .list()
                .stream()
                .collect(Collectors.toMap(TenantPlugin::getPluginId, Function.identity(), (a, b) -> a));

        List<Plugin> enabledPlugins = pluginService.lambdaQuery()
                .eq(Plugin::getStatus, 1)
                .eq(Plugin::getLifecycleStatus, "enabled")
                .orderByAsc(Plugin::getId)
                .list();

        List<Plugin> grayPlugins = pluginService.lambdaQuery()
                .eq(Plugin::getStatus, 1)
                .eq(Plugin::getLifecycleStatus, "gray")
                .list();

        final long tenantIdForFilter = parsedTenantId;
        List<Plugin> visibleGrayPlugins = grayPlugins.stream()
                .filter(p -> {
                    String grayIds = p.getGrayTenantIds();
                    if (grayIds == null || grayIds.isBlank()) return false;
                    String[] ids = grayIds.split(",");
                    for (String id : ids) {
                        try {
                            if (Long.parseLong(id.trim()) == tenantIdForFilter) return true;
                        } catch (NumberFormatException ignored) {}
                    }
                    return false;
                })
                .toList();

        java.util.List<VisiblePluginItem> result = new java.util.ArrayList<>();
        for (Plugin p : enabledPlugins) {
            TenantPlugin tp = tenantPluginMap.get(p.getPluginId());
            boolean tenantEnabled = tp != null && Objects.equals(tp.getEnabled(), 1);
            result.add(new VisiblePluginItem(
                    p.getPluginId(),
                    p.getName(),
                    p.getVersion(),
                    p.getBillingType(),
                    "enabled",
                    tenantEnabled
            ));
        }
        for (Plugin p : visibleGrayPlugins) {
            TenantPlugin tp = tenantPluginMap.get(p.getPluginId());
            boolean tenantEnabled = tp != null && Objects.equals(tp.getEnabled(), 1);
            result.add(new VisiblePluginItem(
                    p.getPluginId(),
                    p.getName(),
                    p.getVersion(),
                    p.getBillingType(),
                    "gray",
                    tenantEnabled
            ));
        }

        return ApiResponse.ok(result, RequestContextUtil.resolveRequestId(requestId, "req-plugin-visible"));
    }

    private record VisiblePluginItem(
            @JsonProperty("plugin_id") String pluginId,
            String name,
            String version,
            @JsonProperty("billing_type") String billingType,
            @JsonProperty("lifecycle_status") String lifecycleStatus,
            boolean enabled
    ) {}

    @PostMapping("/{plugin_id}/enable")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> enable(@PathVariable("plugin_id") String pluginId,
                                                    @RequestHeader("X-Tenant-Id") String tenantId,
                                                    @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return switchPlugin(pluginId, tenantId, requestId, 1);
    }

    @PostMapping("/{plugin_id}/disable")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> disable(@PathVariable("plugin_id") String pluginId,
                                                     @RequestHeader("X-Tenant-Id") String tenantId,
                                                     @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return switchPlugin(pluginId, tenantId, requestId, 0);
    }

    @PostMapping("/{plugin_id}/invoke")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ApiModels.PluginInvokeResponse> invoke(@PathVariable("plugin_id") String pluginId,
                                                               @RequestBody(required = false) Map<String, Object> payload,
                                                               @RequestHeader("X-Tenant-Id") String tenantId,
                                                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                               @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-plugin-invoke");

        ApiModels.PluginInvokeResponse data = pluginInvokeWorkflowService.invoke(
                parsedTenantId,
                pluginId,
                payload,
                idempotencyKey,
                resolvedRequestId
        );

        return ApiResponse.ok(data, resolvedRequestId);
    }

    private ApiResponse<Map<String, Object>> switchPlugin(String pluginId,
                                                           String tenantId,
                                                           String requestId,
                                                           int enabled) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .eq(Plugin::getStatus, 1)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        TenantPlugin tenantPlugin = tenantPluginService.lambdaQuery()
                .eq(TenantPlugin::getTenantId, parsedTenantId)
                .eq(TenantPlugin::getPluginId, pluginId)
                .one();
        if (tenantPlugin == null) {
            tenantPlugin = TenantPlugin.builder()
                    .tenantId(parsedTenantId)
                    .pluginId(pluginId)
                    .enabled(enabled)
                    .configJson("{}")
                    .build();
            tenantPluginService.save(tenantPlugin);
        } else {
            tenantPlugin.setEnabled(enabled);
            tenantPluginService.updateById(tenantPlugin);
        }

        auditLogService.save(AuditLog.builder()
                .tenantId(parsedTenantId)
                .action(enabled == 1 ? "plugin_enable" : "plugin_disable")
                .targetType("plugin")
                .targetId(pluginId)
                .detailJson("{\"enabled\":" + (enabled == 1) + "}")
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("enabled", enabled == 1);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-plugin-switch"));
    }
}