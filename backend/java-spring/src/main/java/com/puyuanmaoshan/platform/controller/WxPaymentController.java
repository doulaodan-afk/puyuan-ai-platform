package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.WxPaymentDtos;
import com.puyuanmaoshan.platform.service.RechargeOrderWorkflowService;
import com.puyuanmaoshan.platform.service.WxPaymentService;
import com.puyuanmaoshan.platform.service.impl.WxPaymentServiceImpl;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 微信支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment/wx")
public class WxPaymentController {

    private final WxPaymentService wxPaymentService;
    private final WxPaymentServiceImpl wxPaymentServiceImpl;
    private final RechargeOrderWorkflowService rechargeOrderWorkflowService;

    public WxPaymentController(
            WxPaymentService wxPaymentService,
            WxPaymentServiceImpl wxPaymentServiceImpl,
            RechargeOrderWorkflowService rechargeOrderWorkflowService) {
        this.wxPaymentService = wxPaymentService;
        this.wxPaymentServiceImpl = wxPaymentServiceImpl;
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
     *
     * 微信支付 V3 回调会发送以下 HTTP Headers：
     * - Wechatpay-Signature: 签名值
     * - Wechatpay-Timestamp: 时间戳
     * - Wechatpay-Nonce: 随机串
     * - Wechatpay-Serial: 平台证书序列号
     * - Wechatpay-Signature-Type: 签名类型（RSA）
     */
    @PostMapping("/notify")
    public ResponseEntity<String> handleNotify(
            @RequestBody String rawBody,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String wechatpaySerial,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String wechatpaySignature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String wechatpayTimestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String wechatpayNonce) {

        try {
            log.info("收到微信支付回调通知");

            // 尝试解析通知请求体
            WxPaymentDtos.WxPayNotifyRequest notifyRequest = parseNotifyRequest(rawBody);

            // 解密回调数据
            WxPaymentDtos.WxPayNotifyData notifyData;
            if (wechatpaySerial != null && wechatpaySignature != null) {
                // 真实模式：使用 SDK 解密
                notifyData = wxPaymentServiceImpl.decryptNotify(
                        notifyRequest, wechatpaySerial, wechatpaySignature,
                        wechatpayTimestamp, wechatpayNonce, rawBody);
            } else {
                // Mock 模式或缺少验证头：直接使用请求体中的数据
                notifyData = extractNotifyDataFromRequest(notifyRequest);
            }

            // 处理支付回调
            boolean success = wxPaymentService.handlePaymentNotify(notifyData);

            if (success) {
                // 微信要求返回 HTTP 200 + {"code":"SUCCESS","message":"成功"}
                return ResponseEntity.ok("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"code\":\"FAIL\",\"message\":\"处理失败\"}");
            }
        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}");
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
     * 解析通知请求体（简易 JSON 解析，生产环境应使用 ObjectMapper）
     */
    private WxPaymentDtos.WxPayNotifyRequest parseNotifyRequest(String rawBody) {
        // 在 Mock 模式或测试场景下，body 可能不是标准 V3 格式
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(rawBody, WxPaymentDtos.WxPayNotifyRequest.class);
        } catch (Exception e) {
            log.warn("解析微信通知请求体失败，使用空对象: {}", e.getMessage());
            return new WxPaymentDtos.WxPayNotifyRequest();
        }
    }

    /**
     * 从通知请求中提取数据（Mock 模式降级）
     */
    private WxPaymentDtos.WxPayNotifyData extractNotifyDataFromRequest(WxPaymentDtos.WxPayNotifyRequest notifyRequest) {
        WxPaymentDtos.WxPayNotifyData data = new WxPaymentDtos.WxPayNotifyData();
        if (notifyRequest.getResource() != null) {
            // 无法解密，返回空数据
            data.setOutTradeNo("unknown");
            data.setTradeState("UNKNOWN");
        } else {
            data.setOutTradeNo("mock_" + System.currentTimeMillis());
            data.setTradeState("SUCCESS");
            data.setTransactionId("mock_txn_" + System.currentTimeMillis());
        }
        return data;
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