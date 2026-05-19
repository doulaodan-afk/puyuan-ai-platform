package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.entity.BillingLedger;
import com.puyuanmaoshan.platform.entity.IdempotencyRecord;
import com.puyuanmaoshan.platform.entity.RechargeOrder;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.service.BillingLedgerService;
import com.puyuanmaoshan.platform.service.IdempotencyRecordService;
import com.puyuanmaoshan.platform.service.RechargeOrderService;
import com.puyuanmaoshan.platform.service.RechargeOrderWorkflowService;
import com.puyuanmaoshan.platform.util.BizNoUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class RechargeOrderWorkflowServiceImpl implements RechargeOrderWorkflowService {
    private static final BigDecimal TOKEN_RATIO = new BigDecimal("2000");

    private final RechargeOrderService rechargeOrderService;
    private final IdempotencyRecordService idempotencyRecordService;
    private final AccountWalletService accountWalletService;
    private final BillingLedgerService billingLedgerService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public RechargeOrderWorkflowServiceImpl(RechargeOrderService rechargeOrderService,
                                            IdempotencyRecordService idempotencyRecordService,
                                            AccountWalletService accountWalletService,
                                            BillingLedgerService billingLedgerService,
                                            AuditLogService auditLogService,
                                            ObjectMapper objectMapper) {
        this.rechargeOrderService = rechargeOrderService;
        this.idempotencyRecordService = idempotencyRecordService;
        this.accountWalletService = accountWalletService;
        this.billingLedgerService = billingLedgerService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiModels.RechargeOrderResponse createOrder(long tenantId,
                                                        ApiModels.CreateRechargeOrderRequest request,
                                                        String idempotencyKey,
                                                        String requestId) {
        String requestHash = buildRequestHash(tenantId, request.amount(), request.payChannel());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyRecordService.lambdaQuery()
                    .eq(IdempotencyRecord::getIdempotencyKey, idempotencyKey)
                    .one();
            if (existing != null) {
                return handleExistingCreate(existing, requestHash);
            }
        }

        String orderNo = BizNoUtil.nextNo("RC");
        long tokenGrant = request.amount().multiply(TOKEN_RATIO).longValue();
        RechargeOrder order = RechargeOrder.builder()
                .orderNo(orderNo)
                .tenantId(tenantId)
                .amount(request.amount())
                .tokenGrant(tokenGrant)
                .payChannel(request.payChannel())
                .payStatus("created")
                .build();
        rechargeOrderService.save(order);

        ApiModels.RechargeOrderResponse response = new ApiModels.RechargeOrderResponse(
                orderNo,
                request.amount(),
                tokenGrant,
                "created"
        );

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            persistOrValidateIdempotency(idempotencyKey, "recharge_order_create", requestHash, response, 24);
        }

        auditLogService.save(AuditLog.builder()
                .tenantId(tenantId)
                .action("recharge_order_create")
                .targetType("recharge_order")
                .targetId(orderNo)
                .detailJson(writeJson(Map.of("amount", request.amount(), "token_grant", tokenGrant, "request_id", requestId)))
                .build());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmOrder(long tenantId,
                                            String orderNo,
                                            ApiModels.RechargeConfirmRequest request,
                                            String idempotencyKey,
                                            String requestId) {
        String requestHash = buildRequestHash(tenantId, orderNo, request.payTxnNo(), request.payResult());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyRecordService.lambdaQuery()
                    .eq(IdempotencyRecord::getIdempotencyKey, idempotencyKey)
                    .one();
            if (existing != null) {
                return handleExistingConfirm(existing, requestHash);
            }
        }

        RechargeOrder order = rechargeOrderService.lambdaQuery()
                .eq(RechargeOrder::getTenantId, tenantId)
                .eq(RechargeOrder::getOrderNo, orderNo)
                .one();
        if (order == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "order not found");
        }

        String payResult = request.payResult() == null ? "" : request.payResult().toLowerCase(Locale.ROOT);
        boolean paid = "paid".equals(payResult) || "success".equals(payResult) || "ok".equals(payResult);
        boolean alreadyPaid = "paid".equalsIgnoreCase(order.getPayStatus());

        if (paid) {
            order.setPayStatus("paid");
            if (!alreadyPaid) {
                order.setPaidAt(LocalDateTime.now());
            }
            rechargeOrderService.updateById(order);

            if (!alreadyPaid) {
                AccountWallet wallet = accountWalletService.lambdaQuery()
                        .eq(AccountWallet::getTenantId, tenantId)
                        .one();
                if (wallet == null) {
                    wallet = AccountWallet.builder()
                            .tenantId(tenantId)
                            .tokenBalance(0L)
                            .cashBalance(BigDecimal.ZERO)
                            .frozenToken(0L)
                            .status(1)
                            .build();
                    accountWalletService.save(wallet);
                }

                long beforeToken = wallet.getTokenBalance() == null ? 0L : wallet.getTokenBalance();
                long tokenGrant = order.getTokenGrant() == null ? 0L : order.getTokenGrant();
                long afterToken = beforeToken + tokenGrant;

                wallet.setTokenBalance(afterToken);
                wallet.setCashBalance((wallet.getCashBalance() == null ? BigDecimal.ZERO : wallet.getCashBalance())
                        .add(order.getAmount() == null ? BigDecimal.ZERO : order.getAmount()));
                accountWalletService.updateById(wallet);

                billingLedgerService.save(BillingLedger.builder()
                        .tenantId(tenantId)
                        .bizNo(BizNoUtil.nextNo("BIZ"))
                        .requestId(requestId)
                        .entryType("recharge")
                        .direction("in")
                        .tokenAmount(tokenGrant)
                        .cashAmount(order.getAmount() == null ? BigDecimal.ZERO : order.getAmount())
                        .balanceAfter(afterToken)
                        .status("success")
                        .occurredAt(LocalDateTime.now())
                        .build());
            }
        } else {
            order.setPayStatus("failed");
            rechargeOrderService.updateById(order);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("order_no", orderNo);
        response.put("pay_result", request.payResult());
        response.put("pay_status", order.getPayStatus());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            persistOrValidateIdempotency(idempotencyKey, "recharge_order_confirm", requestHash, response, 24);
        }

        auditLogService.save(AuditLog.builder()
                .tenantId(tenantId)
                .action("recharge_order_confirm")
                .targetType("recharge_order")
                .targetId(orderNo)
                .detailJson(writeJson(Map.of(
                        "pay_result", request.payResult(),
                        "pay_status", order.getPayStatus(),
                        "idempotent_replay", alreadyPaid,
                        "request_id", requestId
                )))
                .build());

        return response;
    }

    private ApiModels.RechargeOrderResponse handleExistingCreate(IdempotencyRecord existing, String requestHash) {
        if (!"recharge_order_create".equals(existing.getScope())) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "idempotency key scope conflict");
        }
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "idempotency key payload conflict");
        }
        try {
            return objectMapper.readValue(existing.getResponseBody(), ApiModels.RechargeOrderResponse.class);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "duplicate idempotency key");
        }
    }

    private Map<String, Object> handleExistingConfirm(IdempotencyRecord existing, String requestHash) {
        if (!"recharge_order_confirm".equals(existing.getScope())) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "idempotency key scope conflict");
        }
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "idempotency key payload conflict");
        }
        try {
            return objectMapper.readValue(existing.getResponseBody(), objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (Exception ex) {
            throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "duplicate idempotency key");
        }
    }

    private void persistOrValidateIdempotency(String idempotencyKey,
                                              String scope,
                                              String requestHash,
                                              Object response,
                                              int expireHours) {
        try {
            idempotencyRecordService.save(IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .scope(scope)
                    .requestHash(requestHash)
                    .responseBody(writeJson(response))
                    .status("success")
                    .expireAt(LocalDateTime.now().plusHours(expireHours))
                    .build());
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord existing = idempotencyRecordService.lambdaQuery()
                    .eq(IdempotencyRecord::getIdempotencyKey, idempotencyKey)
                    .one();
            if (existing == null || !Objects.equals(existing.getScope(), scope) || !Objects.equals(existing.getRequestHash(), requestHash)) {
                throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT, "duplicate idempotency key");
            }
        }
    }

    private String buildRequestHash(Object... values) {
        return String.valueOf(Objects.hash(values));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "serialize response failed");
        }
    }
}