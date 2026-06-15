package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.ApiModels;

import java.math.BigDecimal;

public interface PricingConfigService {
    ApiModels.PricingConfigResponse getConfig();

    ApiModels.PricingConfigResponse updateConfig(ApiModels.PricingConfigResponse request);

    /**
     * 获取新用户注册赠送的 Token 数量
     * @return 注册赠送 Token 数，默认 10
     */
    long getRegisterBonusToken();

    /**
     * 获取充值兑换率（1元 = 多少 Token）
     * @return Token 兑换率，默认 10
     */
    BigDecimal getTokenRatio();

    /**
     * 获取扣费记账单价（1 Token = 多少元）
     * @return 每 Token 现金价值，默认 0.1
     */
    BigDecimal getCashPerToken();
}
