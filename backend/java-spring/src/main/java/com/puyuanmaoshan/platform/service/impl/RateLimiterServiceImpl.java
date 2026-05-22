package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterServiceImpl implements RateLimiterService {

    private final RedisTemplate<String, Long> redisTemplate;

    @Value("${rate-limit.token-bucket.capacity:60}")
    private long capacity;

    @Value("${rate-limit.token-bucket.refill-rate:1}")
    private long refillRate;

    private static final String TOKEN_BUCKET_PREFIX = "rate_limit:bucket:";

    @Override
    public boolean isAllowed(String key) {
        String bucketKey = TOKEN_BUCKET_PREFIX + key;

        Long tokens = redisTemplate.opsForValue().get(bucketKey);

        if (tokens == null) {
            // 首次请求，初始化桶
            redisTemplate.opsForValue().set(bucketKey, capacity - 1, 1, TimeUnit.SECONDS);
            log.debug("Rate limit initialized for key: {}, capacity: {}", key, capacity);
            return true;
        }

        long currentTokens = tokens;

        if (currentTokens > 0) {
            // 有令牌，允许请求
            redisTemplate.opsForValue().set(bucketKey, currentTokens - 1, 1, TimeUnit.SECONDS);
            return true;
        }

        log.warn("Rate limit exceeded for key: {}", key);
        return false;
    }

    @Override
    public long getRemainingTokens(String key) {
        String bucketKey = TOKEN_BUCKET_PREFIX + key;
        Long tokens = redisTemplate.opsForValue().get(bucketKey);
        return tokens != null ? tokens : capacity;
    }
}
