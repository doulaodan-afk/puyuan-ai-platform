package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.RateLimiterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(name = "rateLimiterServiceImpl")
public class RateLimiterNoOpServiceImpl implements RateLimiterService {

    @Override
    public boolean isAllowed(String key) {
        return true;
    }

    @Override
    public long getRemainingTokens(String key) {
        return Long.MAX_VALUE;
    }
}