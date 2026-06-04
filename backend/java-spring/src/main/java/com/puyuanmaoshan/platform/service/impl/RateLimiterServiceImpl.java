package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.puyuanmaoshan.platform.entity.TenantRateLimitConfig;
import com.puyuanmaoshan.platform.mapper.TenantRateLimitConfigMapper;
import com.puyuanmaoshan.platform.service.RateLimiterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流服务实现（Redis 版）
 * <p>
 * 支持租户分级限流：
 * - 优先从 DB (tenant_rate_limit_config) 读取租户配额
 * - 兜底使用 application.yml 配置
 * - 默认租户 10 RPM，付费租户 100 RPM
 * - 突发系数允许短时超过限制
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterServiceImpl implements RateLimiterService {

    private final RedisTemplate<String, Long> redisTemplate;
    private final TenantRateLimitConfigMapper tenantRateLimitConfigMapper;

    @Value("${rate-limit.token-bucket.capacity:60}")
    private long capacity;

    @Value("${rate-limit.token-bucket.refill-rate:1}")
    private long refillRate;

    /** 默认租户 RPM */
    @Value("${rate-limit.tenant.default-rpm:10}")
    private int defaultTenantRpm;

    /** 付费租户 RPM */
    @Value("${rate-limit.tenant.premium-rpm:100}")
    private int premiumTenantRpm;

    /** 默认突发系数 */
    @Value("${rate-limit.tenant.burst-multiplier:2.0}")
    private double defaultBurstMultiplier;

    private static final String TOKEN_BUCKET_PREFIX = "rate_limit:bucket:";
    private static final String TENANT_CONFIG_PREFIX = "rate_limit:tenant_config:";

    /** 本地缓存租户限流配置（减少 DB 查询） */
    private final Map<Long, TenantRateLimitConfig> tenantConfigCache = new ConcurrentHashMap<>();
    private long tenantConfigCacheExpiry = 0;
    private static final long TENANT_CONFIG_CACHE_TTL_MS = 60_000; // 1 分钟

    public RateLimiterServiceImpl(RedisTemplate<String, Long> redisTemplate,
                                   TenantRateLimitConfigMapper tenantRateLimitConfigMapper) {
        this.redisTemplate = redisTemplate;
        this.tenantRateLimitConfigMapper = tenantRateLimitConfigMapper;
    }

    @Override
    public boolean isAllowed(String key) {
        return isAllowed(key, defaultTenantRpm);
    }

    @Override
    public boolean isAllowed(String key, int maxRpm) {
        // 使用 maxRpm 作为桶容量
        long effectiveCapacity = maxRpm > 0 ? maxRpm : capacity;
        String bucketKey = TOKEN_BUCKET_PREFIX + key;

        Long tokens = redisTemplate.opsForValue().get(bucketKey);

        if (tokens == null) {
            // 首次请求，初始化桶
            redisTemplate.opsForValue().set(bucketKey, effectiveCapacity - 1, 1, TimeUnit.SECONDS);
            log.debug("Rate limit initialized for key: {}, capacity: {}", key, effectiveCapacity);
            return true;
        }

        long currentTokens = tokens;

        if (currentTokens > 0) {
            // 有令牌，允许请求
            redisTemplate.opsForValue().set(bucketKey, currentTokens - 1, 1, TimeUnit.SECONDS);
            return true;
        }

        log.warn("Rate limit exceeded for key: {}, capacity: {}", key, effectiveCapacity);
        return false;
    }

    @Override
    public long getRemainingTokens(String key) {
        String bucketKey = TOKEN_BUCKET_PREFIX + key;
        Long tokens = redisTemplate.opsForValue().get(bucketKey);
        return tokens != null ? tokens : capacity;
    }

    @Override
    public int getTenantMaxRpm(long tenantId) {
        TenantRateLimitConfig config = getTenantConfig(tenantId);
        if (config != null && config.getMaxRpm() != null) {
            return config.getMaxRpm();
        }
        return defaultTenantRpm;
    }

    @Override
    public double getTenantBurstMultiplier(long tenantId) {
        TenantRateLimitConfig config = getTenantConfig(tenantId);
        if (config != null && config.getBurstMultiplier() != null) {
            return config.getBurstMultiplier().doubleValue();
        }
        return defaultBurstMultiplier;
    }

    /**
     * 获取租户限流配置（带本地缓存）
     */
    private TenantRateLimitConfig getTenantConfig(long tenantId) {
        // 刷新缓存
        if (System.currentTimeMillis() > tenantConfigCacheExpiry) {
            refreshTenantConfigCache();
        }

        // 先查租户专属配置
        TenantRateLimitConfig config = tenantConfigCache.get(tenantId);
        if (config != null) {
            return config;
        }

        // 回退到默认配置（tenant_id = 0）
        return tenantConfigCache.get(0L);
    }

    private void refreshTenantConfigCache() {
        try {
            var configs = tenantRateLimitConfigMapper.selectList(
                    new LambdaQueryWrapper<TenantRateLimitConfig>()
                            .eq(TenantRateLimitConfig::getEnabled, true)
            );
            tenantConfigCache.clear();
            for (TenantRateLimitConfig config : configs) {
                tenantConfigCache.put(config.getTenantId(), config);
            }
            tenantConfigCacheExpiry = System.currentTimeMillis() + TENANT_CONFIG_CACHE_TTL_MS;
            log.debug("Refreshed tenant rate limit config cache: {} entries", configs.size());
        } catch (Exception e) {
            log.warn("Failed to refresh tenant rate limit config cache: {}", e.getMessage());
        }
    }
}
