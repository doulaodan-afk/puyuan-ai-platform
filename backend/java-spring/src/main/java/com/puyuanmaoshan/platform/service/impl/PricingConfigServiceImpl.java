package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.SystemConfigDtos;
import com.puyuanmaoshan.platform.service.PricingConfigService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 定价配置服务实现
 * 持久化到 system_config 表 (config_group = "pricing")，支持管理员通过管理端修改
 * 
 * 管理项：
 * - 新用户注册赠送 Token（register_bonus_token）
 * - 充值兑换率：1元 = N Token（token_ratio）
 * - 扣费记账单价：1 Token = N 元（cash_per_token）
 */
@Service
public class PricingConfigServiceImpl implements PricingConfigService {
    private static final Logger logger = LoggerFactory.getLogger(PricingConfigServiceImpl.class);

    private static final String CONFIG_GROUP = "pricing";
    private static final String KEY_REGISTER_BONUS = "register_bonus_token";
    private static final String KEY_TOKEN_RATIO = "token_ratio";
    private static final String KEY_CASH_PER_TOKEN = "cash_per_token";

    private static final long DEFAULT_REGISTER_BONUS = 10L;
    private static final int DEFAULT_TOKEN_RATIO = 10;
    private static final BigDecimal DEFAULT_CASH_PER_TOKEN = new BigDecimal("0.1");

    private final SystemConfigService systemConfigService;

    public PricingConfigServiceImpl(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public ApiModels.PricingConfigResponse getConfig() {
        try {
            long registerBonus = getLong(KEY_REGISTER_BONUS, DEFAULT_REGISTER_BONUS);
            int tokenRatio = getInt(KEY_TOKEN_RATIO, DEFAULT_TOKEN_RATIO);
            BigDecimal cashPerToken = getBigDecimal(KEY_CASH_PER_TOKEN, DEFAULT_CASH_PER_TOKEN);
            return new ApiModels.PricingConfigResponse(registerBonus, tokenRatio, cashPerToken);
        } catch (Exception e) {
            logger.warn("读取定价配置失败，返回默认值: {}", e.getMessage());
            return new ApiModels.PricingConfigResponse(DEFAULT_REGISTER_BONUS, DEFAULT_TOKEN_RATIO, DEFAULT_CASH_PER_TOKEN);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiModels.PricingConfigResponse updateConfig(ApiModels.PricingConfigResponse request) {
        saveConfig(KEY_REGISTER_BONUS, String.valueOf(request.registerBonusToken()), "新用户注册赠送 Token", 0);
        saveConfig(KEY_TOKEN_RATIO, String.valueOf(request.tokenRatio()), "充值兑换率：1元 = N Token", 1);
        saveConfig(KEY_CASH_PER_TOKEN, request.cashPerToken().toPlainString(), "扣费记账单价：1 Token = N 元", 2);

        logger.info("定价配置已更新: registerBonus={}, tokenRatio={}, cashPerToken={}",
                request.registerBonusToken(), request.tokenRatio(), request.cashPerToken());

        return request;
    }

    /**
     * 获取新用户注册赠送的 Token 数量
     */
    @Override
    public long getRegisterBonusToken() {
        return getLong(KEY_REGISTER_BONUS, DEFAULT_REGISTER_BONUS);
    }

    /**
     * 获取充值兑换率（1元 = 多少 Token）
     */
    @Override
    public BigDecimal getTokenRatio() {
        return getBigDecimal(KEY_TOKEN_RATIO, BigDecimal.valueOf(DEFAULT_TOKEN_RATIO));
    }

    /**
     * 获取扣费记账单价（1 Token = 多少元）
     */
    @Override
    public BigDecimal getCashPerToken() {
        return getBigDecimal(KEY_CASH_PER_TOKEN, DEFAULT_CASH_PER_TOKEN);
    }

    private void saveConfig(String key, String value, String description, int sortOrder) {
        systemConfigService.saveOrUpdateConfig(
                SystemConfigDtos.SaveConfigRequest.builder()
                        .configGroup(CONFIG_GROUP)
                        .configKey(key)
                        .configValue(value)
                        .enabled(true)
                        .sortOrder(sortOrder)
                        .description(description)
                        .build()
        );
    }

    private String getConfigValue(String key) {
        return systemConfigService.getConfigValue(CONFIG_GROUP, key);
    }

    private long getLong(String key, long defaultValue) {
        String val = getConfigValue(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            logger.warn("定价配置 {} 值格式错误: {}, 使用默认值", key, val);
            return defaultValue;
        }
    }

    private int getInt(String key, int defaultValue) {
        String val = getConfigValue(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.warn("定价配置 {} 值格式错误: {}, 使用默认值", key, val);
            return defaultValue;
        }
    }

    private BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String val = getConfigValue(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            logger.warn("定价配置 {} 值格式错误: {}, 使用默认值", key, val);
            return defaultValue;
        }
    }
}
