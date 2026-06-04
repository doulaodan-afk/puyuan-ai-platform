package com.puyuanmaoshan.platform.service;

import java.util.List;

/**
 * 多 API Key 轮询管理器
 * 负责在单个提供商内对多个 API Key 做负载均衡、故障转移和冷却管理
 */
public interface ApiKeyPoolManager {

    /**
     * 根据轮询策略选择一个可用的 API Key
     *
     * @param providerId  提供商 ID
     * @param apiKeys     解密后的 API Key 列表
     * @param strategy    轮询策略（round_robin / least_loaded / random）
     * @param maxRpm      每个 Key 的最大 RPM
     * @return 选中的 API Key
     * @throws IllegalStateException 当所有 Key 都不可用时抛出
     */
    String selectKey(Long providerId, List<String> apiKeys, String strategy, int maxRpm);

    /**
     * 记录 Key 调用成功
     */
    void recordSuccess(Long providerId, int keyIndex);

    /**
     * 记录 Key 调用失败（限流/超时等），触发冷却
     */
    void recordFailure(Long providerId, int keyIndex);

    /**
     * 检查指定 Key 是否处于冷却期
     */
    boolean isKeyInCooldown(Long providerId, int keyIndex);

    /**
     * 获取 Key 的 RPM 使用情况
     */
    int getKeyRpmUsage(Long providerId, int keyIndex);

    /**
     * 清除指定提供商的所有 Key 统计
     */
    void resetProviderStats(Long providerId);
}
