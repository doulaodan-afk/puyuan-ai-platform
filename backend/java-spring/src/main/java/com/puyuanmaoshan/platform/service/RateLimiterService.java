package com.puyuanmaoshan.platform.service;

/**
 * 限流服务接口
 */
public interface RateLimiterService {

    /**
     * 检查是否允许请求
     * @param key 限流键（例如：租户ID）
     * @return true 允许请求，false 拒绝请求
     */
    boolean isAllowed(String key);

    /**
     * 获取剩余令牌数
     * @param key 限流键
     * @return 剩余令牌数
     */
    long getRemainingTokens(String key);
}
