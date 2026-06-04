package com.puyuanmaoshan.platform.service;

/**
 * 限流服务接口
 * <p>
 * 支持租户分级限流：
 * - 默认租户：10 RPM
 * - 付费租户：100 RPM
 * - 突发系数：允许短时超过限制
 */
public interface RateLimiterService {

    /**
     * 检查是否允许请求（使用默认限流额度）
     * @param key 限流键（例如：租户ID）
     * @return true 允许请求，false 拒绝请求
     */
    boolean isAllowed(String key);

    /**
     * 检查是否允许请求（指定限流额度）
     * @param key    限流键
     * @param maxRpm 每分钟最大请求数
     * @return true 允许请求，false 拒绝请求
     */
    boolean isAllowed(String key, int maxRpm);

    /**
     * 获取剩余令牌数
     * @param key 限流键
     * @return 剩余令牌数
     */
    long getRemainingTokens(String key);

    /**
     * 获取租户级别的限流配置
     * @param tenantId 租户 ID
     * @return 该租户的 maxRpm，如果未配置则返回默认值
     */
    int getTenantMaxRpm(long tenantId);

    /**
     * 获取租户的突发系数
     * @param tenantId 租户 ID
     * @return 突发系数，默认 2.0
     */
    double getTenantBurstMultiplier(long tenantId);
}
