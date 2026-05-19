package com.puyuanmaoshan.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<ApiModels.AuditPageResponse> list(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                                          @RequestParam(name = "tenant_id", required = false) Long tenantId,
                                                          @RequestParam(name = "action", required = false) String action,
                                                          @RequestParam(name = "start_at", required = false) String startAt,
                                                          @RequestParam(name = "end_at", required = false) String endAt,
                                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Page<AuditLog> pager = new Page<>(page, pageSize);
        LocalDateTime startTime = parseDateTime(startAt);
        LocalDateTime endTime = parseDateTime(endAt);

        Page<AuditLog> auditPage = auditLogService.lambdaQuery()
                .eq(tenantId != null, AuditLog::getTenantId, tenantId)
                .eq(action != null && !action.isBlank(), AuditLog::getAction, action)
                .ge(startTime != null, AuditLog::getCreatedAt, startTime)
                .le(endTime != null, AuditLog::getCreatedAt, endTime)
                .orderByDesc(AuditLog::getCreatedAt)
                .page(pager);

        List<ApiModels.AuditItemResponse> list = auditPage.getRecords().stream().map(item ->
                new ApiModels.AuditItemResponse(
                        item.getId() == null ? 0L : item.getId(),
                        item.getTenantId(),
                        item.getOperatorId(),
                        item.getAction(),
                        item.getTargetType(),
                        item.getTargetId(),
                        item.getDetailJson(),
                        item.getCreatedAt() == null ? null : item.getCreatedAt().toString()
                )).toList();

        ApiModels.AuditPageResponse data = new ApiModels.AuditPageResponse(list, page, pageSize, auditPage.getTotal());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-audit-list"));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "datetime must be yyyy-MM-dd HH:mm:ss");
        }
    }
}
