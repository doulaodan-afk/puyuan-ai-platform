package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.PluginInvokeLog;
import com.puyuanmaoshan.platform.entity.RechargeOrder;
import com.puyuanmaoshan.platform.service.PluginInvokeLogService;
import com.puyuanmaoshan.platform.service.RechargeOrderService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/billing")
public class AdminBillingController {
    private static final BigDecimal COST_PER_TOKEN = new BigDecimal("0.0008");

    private final PluginInvokeLogService pluginInvokeLogService;
    private final RechargeOrderService rechargeOrderService;

    public AdminBillingController(PluginInvokeLogService pluginInvokeLogService,
                                  RechargeOrderService rechargeOrderService) {
        this.pluginInvokeLogService = pluginInvokeLogService;
        this.rechargeOrderService = rechargeOrderService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<ApiModels.BillingDashboardResponse> dashboard(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<PluginInvokeLog> invokeLogs = pluginInvokeLogService.lambdaQuery().list();
        long totalCalls = invokeLogs.size();
        long totalTokenUsed = invokeLogs.stream().mapToLong(item -> item.getTokenUsed() == null ? 0 : item.getTokenUsed()).sum();

        BigDecimal totalRechargeAmount = rechargeOrderService.lambdaQuery()
                .eq(RechargeOrder::getPayStatus, "paid")
                .list()
                .stream()
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal estimatedCost = BigDecimal.valueOf(totalTokenUsed).multiply(COST_PER_TOKEN);
        double grossMarginRate;
        if (totalRechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            grossMarginRate = 0D;
        } else {
            BigDecimal margin = totalRechargeAmount.subtract(estimatedCost);
            grossMarginRate = margin.divide(totalRechargeAmount, 4, RoundingMode.HALF_UP).doubleValue();
        }

        ApiModels.BillingDashboardResponse data = new ApiModels.BillingDashboardResponse(
                totalCalls,
                totalTokenUsed,
                totalRechargeAmount,
                grossMarginRate
        );
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-billing-dashboard"));
    }

    @GetMapping("/recharge-orders")
    public ApiResponse<Map<String, Object>> rechargeOrders(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                                           @RequestParam(name = "tenant_id", required = false) Long tenantId,
                                                           @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Page<RechargeOrder> pager = new Page<>(page, pageSize);
        Page<RechargeOrder> orderPage = rechargeOrderService.lambdaQuery()
                .eq(tenantId != null, RechargeOrder::getTenantId, tenantId)
                .orderByDesc(RechargeOrder::getCreatedAt)
                .page(pager);

        List<Map<String, Object>> list = orderPage.getRecords().stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("order_no", item.getOrderNo());
            row.put("tenant_id", item.getTenantId());
            row.put("amount", item.getAmount());
            row.put("token_grant", item.getTokenGrant());
            row.put("pay_channel", item.getPayChannel());
            row.put("pay_status", item.getPayStatus());
            row.put("paid_at", item.getPaidAt() == null ? null : item.getPaidAt().toString());
            row.put("created_at", item.getCreatedAt() == null ? null : item.getCreatedAt().toString());
            return row;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("page", page);
        data.put("page_size", pageSize);
        data.put("total", orderPage.getTotal());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-recharge-orders"));
    }
}
