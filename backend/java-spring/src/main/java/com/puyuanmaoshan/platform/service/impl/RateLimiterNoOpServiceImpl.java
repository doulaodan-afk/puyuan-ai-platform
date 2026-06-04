package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.RateLimiterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 限流空实现（Redis 不可用时降级）
 */
@Service
@ConditionalOnMissingBean(name = "rateLimiterServiceImpl")
public class RateLimiterNoOpServiceImpl implements RateLimiterService {

    @Override
    public boolean isAllowed(String key) {
        return true;
    }

    @Override
    public boolean isAllowed(String key, int maxRpm) {
        return true;
    }

    @Override
    public long getRemainingTokens(String key) {
        return Long.MAX_VALUE;
    }

    @Override
    public int getTenantMaxRpm(long tenantId) {
        return Integer.MAX_VALUE;
    }

    @Override
    public double getTenantBurstMultiplier(long tenantId) {
        return 2.0;
    }
}