package com.puyuanmaoshan.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.BillingLedger;
import com.puyuanmaoshan.platform.entity.RechargeOrder;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.BillingLedgerService;
import com.puyuanmaoshan.platform.service.RechargeOrderService;
import com.puyuanmaoshan.platform.service.RechargeOrderWorkflowService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final AccountWalletService accountWalletService;
    private final BillingLedgerService billingLedgerService;
    private final RechargeOrderService rechargeOrderService;
    private final RechargeOrderWorkflowService rechargeOrderWorkflowService;

    public AccountController(AccountWalletService accountWalletService,
                             BillingLedgerService billingLedgerService,
                             RechargeOrderService rechargeOrderService,
                             RechargeOrderWorkflowService rechargeOrderWorkflowService) {
        this.accountWalletService = accountWalletService;
        this.billingLedgerService = billingLedgerService;
        this.rechargeOrderService = rechargeOrderService;
        this.rechargeOrderWorkflowService = rechargeOrderWorkflowService;
    }

    @GetMapping("/balance")
    public ApiResponse<ApiModels.BalanceResponse> balance(@RequestHeader("X-Tenant-Id") String tenantId,
                                                           @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, parsedTenantId)
                .one();

        long tokenBalance = wallet == null || wallet.getTokenBalance() == null ? 0L : wallet.getTokenBalance();
        ApiModels.BalanceResponse data = new ApiModels.BalanceResponse(tokenBalance, 0.0, 5.0, 0.0, "2099-12-31");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-balance"));
    }

    @GetMapping("/ledger")
    public ApiResponse<ApiModels.LedgerPageResponse> ledger(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                                             @RequestParam(name = "entry_type", required = false) String entryType,
                                                             @RequestHeader("X-Tenant-Id") String tenantId,
                                                             @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Page<BillingLedger> pager = new Page<>(page, pageSize);

        Page<BillingLedger> ledgerPage = billingLedgerService.lambdaQuery()
                .eq(BillingLedger::getTenantId, parsedTenantId)
                .eq(entryType != null && !entryType.isBlank(), BillingLedger::getEntryType, entryType)
                .orderByDesc(BillingLedger::getOccurredAt)
                .page(pager);

        List<ApiModels.LedgerItem> list = ledgerPage.getRecords().stream().map(item -> new ApiModels.LedgerItem(
                item.getBizNo(),
                item.getEntryType(),
                item.getDirection(),
                item.getTokenAmount() == null ? 0L : item.getTokenAmount(),
                item.getCashAmount() == null ? BigDecimal.ZERO : item.getCashAmount(),
                item.getBalanceAfter() == null ? 0L : item.getBalanceAfter(),
                item.getPluginId(),
                item.getOccurredAt() == null ? null : item.getOccurredAt().toString()
        )).toList();

        ApiModels.LedgerPageResponse data = new ApiModels.LedgerPageResponse(list, page, pageSize, ledgerPage.getTotal());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-ledger"));
    }

    @PostMapping("/recharge/orders")
    public ApiResponse<ApiModels.RechargeOrderResponse> createRechargeOrder(
            @Valid @RequestBody ApiModels.CreateRechargeOrderRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-recharge-create");

        ApiModels.RechargeOrderResponse data = rechargeOrderWorkflowService.createOrder(
                parsedTenantId,
                request,
                idempotencyKey,
                resolvedRequestId
        );
        return ApiResponse.ok(data, resolvedRequestId);
    }

    @GetMapping("/recharge/orders")
    public ApiResponse<Map<String, Object>> queryRechargeOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "pay_status", required = false) String payStatus,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Page<RechargeOrder> pager = new Page<>(page, pageSize);
        Page<RechargeOrder> orderPage = rechargeOrderService.lambdaQuery()
                .eq(RechargeOrder::getTenantId, parsedTenantId)
                .eq(payStatus != null && !payStatus.isBlank(), RechargeOrder::getPayStatus, payStatus)
                .orderByDesc(RechargeOrder::getCreatedAt)
                .page(pager);

        List<Map<String, Object>> list = orderPage.getRecords().stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("order_no", item.getOrderNo());
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
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-recharge-list"));
    }

    @PostMapping("/recharge/orders/{order_no}/confirm")
    public ApiResponse<Map<String, Object>> confirmRechargeOrder(
            @PathVariable("order_no") String orderNo,
            @Valid @RequestBody ApiModels.RechargeConfirmRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-recharge-confirm");

        Map<String, Object> data = rechargeOrderWorkflowService.confirmOrder(
                parsedTenantId,
                orderNo,
                request,
                idempotencyKey,
                resolvedRequestId
        );
        return ApiResponse.ok(data, resolvedRequestId);
    }
}