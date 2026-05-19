package com.puyuanmaoshan.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class AdminTenantController {
    private final TenantService tenantService;
    private final AuditLogService auditLogService;

    public AdminTenantController(TenantService tenantService,
                                 AuditLogService auditLogService) {
        this.tenantService = tenantService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<ApiModels.TenantPageResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Page<Tenant> pager = new Page<>(page, pageSize);
        Page<Tenant> tenantPage = tenantService.lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(Tenant::getTenantCode, keyword)
                        .or()
                        .like(Tenant::getName, keyword))
                .orderByAsc(Tenant::getId)
                .page(pager);

        List<ApiModels.TenantItemResponse> tenants = tenantPage.getRecords().stream().map(item ->
                new ApiModels.TenantItemResponse(
                        item.getId(),
                        item.getTenantCode(),
                        item.getName(),
                        item.getStatus() == null ? 0 : item.getStatus(),
                        item.getLevel()
                )).toList();

        ApiModels.TenantPageResponse data = new ApiModels.TenantPageResponse(tenants, page, pageSize, tenantPage.getTotal());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-tenant-list"));
    }

    @PostMapping("/{tenant_id}/freeze")
    public ApiResponse<Map<String, Object>> freeze(@PathVariable("tenant_id") long tenantId,
                                                    @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return switchStatus(tenantId, 0, requestId);
    }

    @PostMapping("/{tenant_id}/unfreeze")
    public ApiResponse<Map<String, Object>> unfreeze(@PathVariable("tenant_id") long tenantId,
                                                      @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return switchStatus(tenantId, 1, requestId);
    }

    @PutMapping("/{tenant_id}/level")
    public ApiResponse<Map<String, Object>> updateLevel(@PathVariable("tenant_id") long tenantId,
                                                         @Valid @RequestBody ApiModels.UpdateTenantLevelRequest request,
                                                         @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setLevel(request.level());
        tenantService.updateById(tenant);

        auditLogService.save(AuditLog.builder()
                .tenantId(tenantId)
                .action("tenant_level_update")
                .targetType("tenant")
                .targetId(String.valueOf(tenantId))
                .detailJson("{\"level\":\"" + request.level() + "\"}")
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("tenant_id", tenantId);
        data.put("level", tenant.getLevel());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-tenant-level"));
    }

    private ApiResponse<Map<String, Object>> switchStatus(long tenantId, int status, String requestId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setStatus(status);
        tenantService.updateById(tenant);

        auditLogService.save(AuditLog.builder()
                .tenantId(tenantId)
                .action(status == 0 ? "tenant_freeze" : "tenant_unfreeze")
                .targetType("tenant")
                .targetId(String.valueOf(tenantId))
                .detailJson("{\"status\":" + status + "}")
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("tenant_id", tenantId);
        data.put("status", status == 0 ? "frozen" : "active");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-tenant-switch"));
    }

    private Tenant getTenantOrThrow(long tenantId) {
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "tenant not found");
        }
        return tenant;
    }
}