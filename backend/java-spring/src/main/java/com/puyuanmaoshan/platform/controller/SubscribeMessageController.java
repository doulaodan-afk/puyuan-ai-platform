package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.SubscribeMessageDtos;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订阅消息控制器
 */
@RestController
@RequestMapping("/api/v1/subscribe")
public class SubscribeMessageController {

    private final SubscribeMessageService subscribeMessageService;

    public SubscribeMessageController(SubscribeMessageService subscribeMessageService) {
        this.subscribeMessageService = subscribeMessageService;
    }

    /**
     * 发送余额不足提醒
     */
    @PostMapping("/balance/low")
    public ApiResponse<String> sendBalanceLowNotification(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Long userId = RequestContextUtil.getUserId();
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-sub-balance-low");

        // 发送订阅消息
        subscribeMessageService.sendBalanceLowNotification(userId, parsedTenantId);

        return ApiResponse.ok("发送成功", resolvedRequestId);
    }

    /**
     * 发送充值成功通知
     */
    @PostMapping("/recharge/success")
    public ApiResponse<String> sendRechargeSuccessNotification(
            @Valid @RequestBody RechargeSuccessRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Long userId = RequestContextUtil.getUserId();
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-sub-recharge-success");

        // 发送订阅消息
        subscribeMessageService.sendRechargeSuccessNotification(
                userId,
                parsedTenantId,
                request.tokenGrant,
                request.orderNo
        );

        return ApiResponse.ok("发送成功", resolvedRequestId);
    }

    /**
     * 获取小程序码（用于分享）
     */
    @GetMapping("/minicode")
    public ApiResponse<MiniCodeResponse> generateMiniappCode(
            @RequestParam String pluginCode,
            @RequestParam(defaultValue = "index") String page,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-sub-minicode");

        String code = subscribeMessageService.generateMiniappCode(
                parsedTenantId,
                pluginCode,
                page
        );

        MiniCodeResponse data = new MiniCodeResponse(code);
        return ApiResponse.ok(data, resolvedRequestId);
    }

    /**
     * 记录分享行为
     */
    @PostMapping("/share/record")
    public ApiResponse<String> recordShare(
            @Valid @RequestBody ShareRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Long userId = RequestContextUtil.getUserId();
        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-sub-share-record");

        // 记录分享行为
        subscribeMessageService.recordShare(
                userId,
                parsedTenantId,
                request.pluginCode,
                request.resultType,
                request.formId
        );

        return ApiResponse.ok("记录成功", resolvedRequestId);
    }

    /**
     * 充值成功请求
     */
    @Data
    @NoArgsConstructor
    public static class RechargeSuccessRequest {
        private Long tokenGrant;
        private String orderNo;
    }

    /**
     * 小程序码响应
     */
    @Data
    @NoArgsConstructor
    public static class MiniCodeResponse {
        private String code;

        public MiniCodeResponse(String code) {
            this.code = code;
        }
    }

    /**
     * 分享请求
     */
    @Data
    @NoArgsConstructor
    public static class ShareRequest {
        private String pluginCode;
        private String resultType; // image, text, script
        private String formId;

        public String getPluginCode() {
            return pluginCode;
        }

        public String getResultType() {
            return resultType;
        }

        public String getFormId() {
            return formId;
        }
    }
}
