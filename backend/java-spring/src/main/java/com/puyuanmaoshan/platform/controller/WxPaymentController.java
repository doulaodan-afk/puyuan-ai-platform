package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.WxPaymentDtos;
import com.puyuanmaoshan.platform.service.RechargeOrderWorkflowService;
import com.puyuanmaoshan.platform.service.WxPaymentService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 微信支付控制器
 */
@RestController
@RequestMapping("/api/v1/payment/wx")
public class WxPaymentController {

    private final WxPaymentService wxPaymentService;
    private final RechargeOrderWorkflowService rechargeOrderWorkflowService;

    public WxPaymentController(
            WxPaymentService wxPaymentService,
            RechargeOrderWorkflowService rechargeOrderWorkflowService) {
        this.wxPaymentService = wxPaymentService;
        this.rechargeOrderWorkflowService = rechargeOrderWorkflowService;
    }

    /**
     * 创建微信支付预下单
     */
    @PostMapping("/prepay")
    public ApiResponse<WxPaymentDtos.MiniappPaymentParams> createPrepay(
            @Valid @RequestBody CreateWxPrepayRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-wx-prepay");

        // 1. 创建充值订单
        ApiModels.RechargeOrderResponse orderResponse = rechargeOrderWorkflowService.createOrder(
                parsedTenantId,
                new ApiModels.CreateRechargeOrderRequest(request.amount, "wechat"),
                null,
                resolvedRequestId
        );

        // 2. 生成微信预支付参数
        WxPaymentDtos.MiniappPaymentParams prepayParams = wxPaymentService.createPrepay(
                parsedTenantId,
                orderResponse.orderNo(),
                request.amount.multiply(BigDecimal.valueOf(100)).longValue(), // 元转分
                request.packageName
        );

        // 3. 包装返回（附加订单号）
        WxPaymentDtos.MiniappPaymentParams result = new WxPaymentDtos.MiniappPaymentParams();
        result.setAppId(prepayParams.getAppId());
        result.setTimeStamp(prepayParams.getTimeStamp());
        result.setNonceStr(prepayParams.getNonceStr());
        result.setPackageValue(prepayParams.getPackageValue());
        result.setSignType(prepayParams.getSignType());
        result.setPaySign(prepayParams.getPaySign());
        result.setPackageId(prepayParams.getPackageId());

        return ApiResponse.ok(result, resolvedRequestId);
    }

    /**
     * 微信支付回调通知
     */
    @PostMapping("/notify")
    public ApiResponse<String> handleNotify(
            @RequestBody WxPaymentDtos.WxPayNotifyRequest notifyRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-wx-notify");

        // TODO: 解密回调数据
        // WxPaymentDtos.WxPayNotifyData notifyData = decryptNotifyData(notifyRequest);

        // Mock 模式下直接使用模拟数据
        WxPaymentDtos.WxPayNotifyData notifyData = new WxPaymentDtos.WxPayNotifyData();
        notifyData.setOutTradeNo("mock_" + System.currentTimeMillis());
        notifyData.setTradeState("SUCCESS");
        notifyData.setTransactionId("mock_txn_" + System.currentTimeMillis());
        notifyData.setAttach("tenant_2001");

        // 处理支付回调
        boolean success = wxPaymentService.handlePaymentNotify(notifyData);

        if (success) {
            return ApiResponse.ok("SUCCESS", resolvedRequestId);
        } else {
            return ApiResponse.error(500, "处理支付回调失败", null);
        }
    }

    /**
     * Mock 支付成功接口（用于开发测试）
     */
    @PostMapping("/mock/success")
    public ApiResponse<String> mockPaymentSuccess(
            @RequestBody MockPaymentRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-wx-mock-success");

        // 创建模拟回调数据
        WxPaymentDtos.WxPayNotifyData notifyData = new WxPaymentDtos.WxPayNotifyData();
        notifyData.setOutTradeNo(request.orderNo);
        notifyData.setTradeState("SUCCESS");
        notifyData.setTransactionId("mock_txn_" + System.currentTimeMillis());
        notifyData.setAttach("mock_attach");

        // 处理支付回调
        boolean success = wxPaymentService.handlePaymentNotify(notifyData);

        if (success) {
            return ApiResponse.ok("支付成功", resolvedRequestId);
        } else {
            return ApiResponse.error(500, "处理支付失败", null);
        }
    }

    /**
     * 查询订单状态
     */
    @GetMapping("/order/{orderNo}/status")
    public ApiResponse<String> queryOrderStatus(
            @PathVariable String orderNo,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-wx-query-status");

        String status = wxPaymentService.queryOrderStatus(orderNo);
        return ApiResponse.ok(status, resolvedRequestId);
    }

    /**
     * 创建微信预下单请求
     */
    public record CreateWxPrepayRequest(
            @NotNull BigDecimal amount,
            @NotBlank String packageName
    ) {}

    /**
     * Mock 支付请求
     */
    public record MockPaymentRequest(
            @NotBlank String orderNo
    ) {}
}
