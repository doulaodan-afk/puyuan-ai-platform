package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.service.PricingConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PricingConfigServiceImpl implements PricingConfigService {
    private final AtomicReference<ApiModels.PricingConfigResponse> configRef = new AtomicReference<>(
            new ApiModels.PricingConfigResponse(new BigDecimal("1.20"), new BigDecimal("0.50"), 100000L, 5.0)
    );

    @Override
    public ApiModels.PricingConfigResponse getConfig() {
        return configRef.get();
    }

    @Override
    public ApiModels.PricingConfigResponse updateConfig(ApiModels.PricingConfigResponse request) {
        configRef.set(request);
        return request;
    }
}
