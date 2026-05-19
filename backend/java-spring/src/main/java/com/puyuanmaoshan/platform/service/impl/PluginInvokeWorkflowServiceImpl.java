package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.entity.BillingLedger;
import com.puyuanmaoshan.platform.entity.IdempotencyRecord;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.PluginInvokeLog;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.service.BillingLedgerService;
import com.puyuanmaoshan.platform.service.IdempotencyRecordService;
import com.puyuanmaoshan.platform.service.PluginInvokeLogService;
import com.puyuanmaoshan.platform.service.PluginInvokeWorkflowService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.util.BizNoUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PluginInvokeWorkflowServiceImpl implements PluginInvokeWorkflowService {
    private static final BigDecimal CASH_PER_TOKEN = new BigDecimal("0.0012");

    private final PluginService pluginService;
    private final TenantPluginService tenantPluginService;
    private final AccountWalletService accountWalletService;
    private final BillingLedgerService billingLedgerService;
    private final PluginInvokeLogService pluginInvokeLogService;
    private final IdempotencyRecordService idempotencyRecordService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public PluginInvokeWorkflowServiceImpl(PluginService pluginService,
                                           TenantPluginService tenantPluginService,
                                           AccountWalletService accountWalletService,
                                           BillingLedgerService billingLedgerService,
                                           PluginInvokeLogService pluginInvokeLogService,
                                           IdempotencyRecordService idempotencyRecordService,
                                           AuditLogService auditLogService,
                                           ObjectMapper objectMapper) {
        this.pluginService = pluginService;
        this.tenantPluginService = tenantPluginService;
        this.accountWalletService = accountWalletService;
        this.billingLedgerService = billingLedgerService;
        this.pluginInvokeLogService = pluginInvokeLogService;
        this.idempotencyRecordService = idempotencyRecordService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiModels.PluginInvokeResponse invoke(long tenantId,
                                                 String pluginId,
                                                 Map<String, Object> payload,
                                                 String idempotencyKey,
                                                 String requestId) {
        Map<String, Object> safePayload = payload == null ? Collections.emptyMap() : payload;
        String requestHash = buildRequestHash(tenantId, pluginId, safePayload);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyRecordService.lambdaQuery()
                    .eq(IdempotencyRecord::getIdempotencyKey, idempotencyKey)
                    .one();
            if (existing != null) {
                return handleExistingIdempotency(existing, requestHash);
            }
        }

        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .eq(Plugin::getStatus, 1)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        TenantPlugin tenantPlugin = tenantPluginService.lambdaQuery()
                .eq(TenantPlugin::getTenantId, tenantId)
                .eq(TenantPlugin::getPluginId, pluginId)
                .one();
        if (tenantPlugin == null || !Objects.equals(tenantPlugin.getEnabled(), 1)) {
            throw new AppException(ErrorCode.FORBIDDEN, "plugin disabled for tenant");
        }

        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();
        if (wallet == null) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "wallet not found");
        }

        long tokenUsed = plugin.getDefaultTokenCost() == null ? 0L : plugin.getDefaultTokenCost();
        long beforeBalance = wallet.getTokenBalance() == null ? 0L : wallet.getTokenBalance();
        if (beforeBalance < tokenUsed) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "insufficient token balance");
        }

        Map<String, Object> result = simulatePluginCall(pluginId, safePayload);

        long afterBalance = beforeBalance - tokenUsed;
        wallet.setTokenBalance(afterBalance);
        accountWalletService.updateById(wallet);

        BigDecimal cashAmount = BigDecimal.valueOf(tokenUsed)
                .multiply(CASH_PER_TOKEN)
                .setScale(2, RoundingMode.HALF_UP);

        billingLedgerService.save(BillingLedger.builder()
                .tenantId(tenantId)
                .bizNo(BizNoUtil.nextNo("BIZ"))
                .requestId(requestId)
                .entryType("debit")
                .direction("out")
                .tokenAmount(tokenUsed)
                .cashAmount(cashAmount)
                .balanceAfter(afterBalance)
                .pluginId(pluginId)
                .status("success")
                .occurredAt(LocalDateTime.now())
                .build());

        pluginInvokeLogService.save(PluginInvokeLog.builder()
                .requestId(requestId)
                .tenantId(tenantId)
                .pluginId(pluginId)
                .modelVendor("openai")
                .tokenUsed((int) tokenUsed)
                .latencyMs(800)
                .resultCode(0)
                .riskLevel("low")
                .build());

        ApiModels.PluginInvokeResponse response = new ApiModels.PluginInvokeResponse(result, (int) tokenUsed, afterBalance);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            persistOrValidateIdempotency(idempotencyKey, requestHash, response);
        }

        auditLogService.save(AuditLog.builder()
                .tenantId(tenantId)
                .action("plugin_invoke")
                .targetType("plugin")
                .targetId(pluginId)
                .detailJson(writeJson(Map.of("token_used", tokenUsed, "request_id", requestId)))
                .build());

        return response;
    }

    private ApiModels.PluginInvokeResponse handleExistingIdempotency(IdempotencyRecord existing, String requestHash) {
        if (!"plugin_invoke".equals(existing.getScope())) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "idempotency key scope conflict");
        }
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "idempotency key payload conflict");
        }
        if (existing.getResponseBody() == null || existing.getResponseBody().isBlank()) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "duplicate idempotency key");
        }
        try {
            return objectMapper.readValue(existing.getResponseBody(), ApiModels.PluginInvokeResponse.class);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "duplicate idempotency key");
        }
    }

    private Map<String, Object> simulatePluginCall(String pluginId, Map<String, Object> payload) {
        if (Boolean.TRUE.equals(payload.get("simulate_fail"))) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "plugin invoke failed");
        }
        Map<String, Object> nestedPayload = payload.get("payload") instanceof Map<?, ?> map
                ? castToStringObjectMap(map)
                : Collections.emptyMap();
        if (Boolean.TRUE.equals(nestedPayload.get("simulate_fail"))) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "plugin invoke failed");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "plugin invocation success");
        result.put("plugin_id", pluginId);
        result.put("input", payload);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    private String buildRequestHash(long tenantId, String pluginId, Map<String, Object> payload) {
        return String.valueOf(Objects.hash(tenantId, pluginId, writeJson(payload)));
    }

    private void persistOrValidateIdempotency(String idempotencyKey,
                                              String requestHash,
                                              ApiModels.PluginInvokeResponse response) {
        try {
            idempotencyRecordService.save(IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .scope("plugin_invoke")
                    .requestHash(requestHash)
                    .responseBody(writeJson(response))
                    .status("success")
                    .expireAt(LocalDateTime.now().plusHours(1))
                    .build());
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord existing = idempotencyRecordService.lambdaQuery()
                    .eq(IdempotencyRecord::getIdempotencyKey, idempotencyKey)
                    .one();
            if (existing == null
                    || !"plugin_invoke".equals(existing.getScope())
                    || !Objects.equals(existing.getRequestHash(), requestHash)) {
                throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "duplicate idempotency key");
            }
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "serialize response failed");
        }
    }
}
