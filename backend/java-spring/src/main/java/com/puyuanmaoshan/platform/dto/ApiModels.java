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
            @NotBlank @JsonProperty("mobile") String mobile,
            @NotBlank @JsonProperty("verify_code") String verifyCode
    ) {}

    public record WxLoginRequest(
            @NotBlank String code,
            WxUserInfo userInfo
    ) {
        public WxUserInfo getUserInfo() {
            return userInfo;
        }

        public record WxUserInfo(
                String nickName,
                String avatarUrl,
                String gender,
                String city,
                String province,
                String country
        ) {
            public String getNickName() {
                return nickName;
            }

            public String getAvatarUrl() {
                return avatarUrl;
            }

            public String getGender() {
                return gender;
            }

            public String getCity() {
                return city;
            }

            public String getProvince() {
                return province;
            }

            public String getCountry() {
                return country;
            }
        }
    }

    public record LoginResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("user_id") long userId,
            @JsonProperty("tenant_id") long tenantId,
            @JsonProperty("role_code") String roleCode,
            @JsonProperty("tenants") List<TenantDtos.UserTenant> tenants
    ) {}

    public record ProfileResponse(
            @JsonProperty("tenant_id") long tenantId,
            @JsonProperty("tenant_code") String tenantCode,
            @JsonProperty("tenant_name") String tenantName,
            @JsonProperty("tenant_status") int tenantStatus,
            @JsonProperty("logo_url") String logoUrl,
            @JsonProperty("user_id") long userId,
            @JsonProperty("role_code") String roleCode
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

    public record UpdatePluginModelRequest(
            @JsonProperty("ai_model") String aiModel
    ) {}

    public record AiModelItem(
            String id,
            @JsonProperty("owned_by") String ownedBy
    ) {}

    public record PublishPluginRequest(
            @NotBlank String mode,
            List<Long> tenantIds
    ) {}

    public record PricingConfigResponse(
            @JsonProperty("register_bonus_token") long registerBonusToken,
            @JsonProperty("token_ratio") int tokenRatio,
            @JsonProperty("cash_per_token") java.math.BigDecimal cashPerToken
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
