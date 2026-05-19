package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.service.PricingConfigService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/pricing")
public class AdminPricingController {
    private final PricingConfigService pricingConfigService;

    public AdminPricingController(PricingConfigService pricingConfigService) {
        this.pricingConfigService = pricingConfigService;
    }

    @GetMapping
    public ApiResponse<ApiModels.PricingConfigResponse> get(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ApiResponse.ok(pricingConfigService.getConfig(), RequestContextUtil.resolveRequestId(requestId, "req-admin-pricing-get"));
    }

    @PutMapping
    public ApiResponse<ApiModels.PricingConfigResponse> update(
            @Valid @RequestBody ApiModels.PricingConfigResponse request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ApiResponse.ok(pricingConfigService.updateConfig(request), RequestContextUtil.resolveRequestId(requestId, "req-admin-pricing-update"));
    }
}
