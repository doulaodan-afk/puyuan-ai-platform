package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.ApiModels;

public interface PricingConfigService {
    ApiModels.PricingConfigResponse getConfig();

    ApiModels.PricingConfigResponse updateConfig(ApiModels.PricingConfigResponse request);
}
