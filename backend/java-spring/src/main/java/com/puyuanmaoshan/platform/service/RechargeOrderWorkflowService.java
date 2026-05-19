package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.ApiModels;

import java.util.Map;

public interface RechargeOrderWorkflowService {
    ApiModels.RechargeOrderResponse createOrder(long tenantId,
                                                ApiModels.CreateRechargeOrderRequest request,
                                                String idempotencyKey,
                                                String requestId);

    Map<String, Object> confirmOrder(long tenantId,
                                     String orderNo,
                                     ApiModels.RechargeConfirmRequest request,
                                     String idempotencyKey,
                                     String requestId);
}
