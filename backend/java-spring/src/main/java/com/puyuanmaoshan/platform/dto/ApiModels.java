package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {}

    public record LoginRequest(
            @NotBlank String mobile,
            @NotBlank String verifyCode
    ) {}

    public record LoginResponse(
            String accessToken,
            long expiresIn,
            long userId,
            long tenantId,
            String roleCode
    ) {}

    public record ProfileResponse(
            long tenantId,
            String tenantCode,
            String tenantName,
            int tenantStatus,
            long userId,
            String roleCode
    ) {}

    public record BalanceResponse(
            long tokenBalance,
            double storageUsedGb,
            double storageFreeQuotaGb,
            double storageExtraGb,
            String expireDate
    ) {}

    public record CreateRechargeOrderRequest(
            @NotNull BigDecimal amount,
            @NotBlank String payChannel
    ) {}

    public record RechargeConfirmRequest(
            @NotBlank String payTxnNo,
            @NotBlank String payResult
    ) {}

    public record RechargeOrderResponse(
            String orderNo,
            BigDecimal amount,
            long tokenGrant,
            String payStatus
    ) {}

    public record LedgerItem(
            String bizNo,
            String entryType,
            String direction,
            long tokenAmount,
            BigDecimal cashAmount,
            long balanceAfter,
            String pluginId,
            String occurredAt
    ) {}

    public record LedgerPageResponse(
            List<LedgerItem> list,
            int page,
            int pageSize,
            long total
    ) {}

    public record PluginItem(
            String pluginId,
            String name,
            String version,
            String billingType,
            boolean enabled
    ) {}

    public record PluginInvokeRequest(Map<String, Object> payload) {}

    public record PluginInvokeResponse(
            Map<String, Object> result,
            int tokenUsed,
            long balanceRemaining
    ) {}

    public record DailyStatementResponse(
            String statDate,
            long tokenIn,
            long tokenOut,
            int callCount,
            BigDecimal amountRecharge,
            BigDecimal amountRefund
    ) {}

    public record MonthlyStatementResponse(
            String month,
            long tokenIn,
            long tokenOut,
            int callCount,
            BigDecimal amountRecharge,
            BigDecimal amountRefund
    ) {}

    public record TenantItemResponse(
            long tenantId,
            String tenantCode,
            String tenantName,
            int status,
            String level
    ) {}

    public record TenantPageResponse(
            List<TenantItemResponse> list,
            int page,
            int pageSize,
            long total
    ) {}

    public record UpdateTenantLevelRequest(
            @NotBlank String level
    ) {}

    public record CreatePluginRequest(
            @NotBlank String pluginId,
            @NotBlank String name,
            @NotBlank String version,
            @NotBlank String backendApi,
            String frontendEntry,
            @NotBlank String billingType
    ) {}

    public record UpdatePluginRequest(
            String name,
            String version,
            String backendApi,
            String frontendEntry,
            String billingType,
            Integer status
    ) {}

    public record PublishPluginRequest(
            @NotBlank String mode,
            List<Long> tenantIds
    ) {}

    public record PricingConfigResponse(
            @JsonProperty("token_price_per_1k") BigDecimal tokenPricePer1k,
            @JsonProperty("storage_price_per_gb_month") BigDecimal storagePricePerGbMonth,
            @JsonProperty("free_token_quota_month") long freeTokenQuotaMonth,
            @JsonProperty("free_storage_quota_gb") double freeStorageQuotaGb
    ) {}

    public record BillingDashboardResponse(
            long totalCalls,
            long totalTokenUsed,
            BigDecimal totalRechargeAmount,
            double grossMarginRate
    ) {}

    public record AuditItemResponse(
            long id,
            Long tenantId,
            Long operatorId,
            String action,
            String targetType,
            String targetId,
            String detailJson,
            String createdAt
    ) {}

    public record AuditPageResponse(
            List<AuditItemResponse> list,
            int page,
            int pageSize,
            long total
    ) {}
}
