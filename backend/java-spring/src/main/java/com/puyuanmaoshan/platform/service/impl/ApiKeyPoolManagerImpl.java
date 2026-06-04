package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.ApiKeyPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多 API Key 轮询管理器实现（内存版）
 * <p>
 * 支持三种轮询策略：
 * - round_robin：轮询
 * - least_loaded：最少负载（按最近一分钟请求数）
 * - random：随机
 * <p>
 * 故障冷却：Key 调用失败后进入冷却期（默认 60 秒），冷却期内不会被选中。
 * RPM 限流：每个 Key 每分钟最多 maxRpm 次请求（0 表示不限制）。
 */
@Slf4j
@Service
public class ApiKeyPoolManagerImpl implements ApiKeyPoolManager {

    /** Key 冷却时间（秒），可通过 system_config 动态调整 */
    private volatile long cooldownSeconds = 60;

    /** 每个提供商的轮询计数器 (providerId -> counter) */
    private final Map<Long, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    /** Key 统计信息：providerId:keyIndex -> stats */
    private final Map<String, KeyStats> keyStatsMap = new ConcurrentHashMap<>();

    // RPM 滑动窗口：60 秒
    private static final int RPM_WINDOW_SECONDS = 60;

    @Override
    public String selectKey(Long providerId, List<String> apiKeys, String strategy, int maxRpm) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalStateException("No API keys available for provider " + providerId);
        }

        // 单 Key 直接返回
        if (apiKeys.size() == 1) {
            String key = apiKeys.get(0);
            if (!isKeyInCooldown(providerId, 0) && !isKeyRpmExceeded(providerId, 0, maxRpm)) {
                return key;
            }
            throw new IllegalStateException("Sole API key is in cooldown or rate limited for provider " + providerId);
        }

        String effectiveStrategy = strategy != null ? strategy.toLowerCase() : "round_robin";

        return switch (effectiveStrategy) {
            case "least_loaded" -> selectLeastLoaded(providerId, apiKeys, maxRpm);
            case "random" -> selectRandom(providerId, apiKeys, maxRpm);
            default -> selectRoundRobin(providerId, apiKeys, maxRpm);
        };
    }

    @Override
    public void recordSuccess(Long providerId, int keyIndex) {
        KeyStats stats = getOrCreateStats(providerId, keyIndex);
        synchronized (stats) {
            stats.totalRequests++;
            stats.lastMinuteRequests++;
            stats.lastSuccessTime = System.currentTimeMillis();
        }
        // 每分钟衰减一次 RPM 计数
        scheduleRpmDecay(providerId, keyIndex);
    }

    @Override
    public void recordFailure(Long providerId, int keyIndex) {
        KeyStats stats = getOrCreateStats(providerId, keyIndex);
        synchronized (stats) {
            stats.totalFailures++;
            stats.lastFailTime = System.currentTimeMillis();
            stats.cooldownUntil = Instant.now().plusSeconds(cooldownSeconds).toEpochMilli();
        }
        log.warn("Key {} for provider {} entered cooldown until {} ({}s)",
                keyIndex, providerId, stats.cooldownUntil, cooldownSeconds);
    }

    @Override
    public boolean isKeyInCooldown(Long providerId, int keyIndex) {
        KeyStats stats = keyStatsMap.get(buildKey(providerId, keyIndex));
        if (stats == null) return false;
        return stats.cooldownUntil > System.currentTimeMillis();
    }

    @Override
    public int getKeyRpmUsage(Long providerId, int keyIndex) {
        KeyStats stats = keyStatsMap.get(buildKey(providerId, keyIndex));
        return stats != null ? stats.lastMinuteRequests : 0;
    }

    @Override
    public void resetProviderStats(Long providerId) {
        roundRobinCounters.remove(providerId);
        keyStatsMap.keySet().removeIf(k -> k.startsWith(providerId + ":"));
        log.info("Reset stats for provider {}", providerId);
    }

    // ========== 私有方法 ==========

    private String selectRoundRobin(Long providerId, List<String> apiKeys, int maxRpm) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(providerId, k -> new AtomicInteger(0));

        // 最多尝试所有 Key 一遍
        for (int attempt = 0; attempt < apiKeys.size(); attempt++) {
            int idx = Math.abs(counter.getAndIncrement() % apiKeys.size());
            if (!isKeyInCooldown(providerId, idx) && !isKeyRpmExceeded(providerId, idx, maxRpm)) {
                return apiKeys.get(idx);
            }
        }

        // 所有 Key 都不可用，尝试找一个冷却已过的
        for (int i = 0; i < apiKeys.size(); i++) {
            if (!isKeyInCooldown(providerId, i)) {
                return apiKeys.get(i);
            }
        }

        throw new IllegalStateException("All API keys are in cooldown or rate limited for provider " + providerId);
    }

    private String selectLeastLoaded(Long providerId, List<String> apiKeys, int maxRpm) {
        int bestIdx = -1;
        int minLoad = Integer.MAX_VALUE;

        for (int i = 0; i < apiKeys.size(); i++) {
            if (isKeyInCooldown(providerId, i) || isKeyRpmExceeded(providerId, i, maxRpm)) {
                continue;
            }
            int load = getKeyRpmUsage(providerId, i);
            if (load < minLoad) {
                minLoad = load;
                bestIdx = i;
            }
        }

        if (bestIdx >= 0) {
            return apiKeys.get(bestIdx);
        }
        throw new IllegalStateException("All API keys are unavailable for provider " + providerId);
    }

    private String selectRandom(Long providerId, List<String> apiKeys, int maxRpm) {
        // 收集可用 Key
        List<Integer> availableIndices = java.util.stream.IntStream.range(0, apiKeys.size())
                .filter(i -> !isKeyInCooldown(providerId, i) && !isKeyRpmExceeded(providerId, i, maxRpm))
                .boxed()
                .toList();

        if (availableIndices.isEmpty()) {
            throw new IllegalStateException("All API keys are unavailable for provider " + providerId);
        }

        int randomIdx = availableIndices.get((int) (Math.random() * availableIndices.size()));
        return apiKeys.get(randomIdx);
    }

    private boolean isKeyRpmExceeded(Long providerId, int keyIndex, int maxRpm) {
        if (maxRpm <= 0) return false; // 不限制
        int usage = getKeyRpmUsage(providerId, keyIndex);
        return usage >= maxRpm;
    }

    private KeyStats getOrCreateStats(Long providerId, int keyIndex) {
        return keyStatsMap.computeIfAbsent(buildKey(providerId, keyIndex), k -> new KeyStats());
    }

    private void scheduleRpmDecay(Long providerId, int keyIndex) {
        // 简化实现：在每次请求时检查是否超过窗口期，是则重置
        KeyStats stats = keyStatsMap.get(buildKey(providerId, keyIndex));
        if (stats == null) return;

        long now = System.currentTimeMillis();
        synchronized (stats) {
            if (now - stats.lastRpmResetTime > RPM_WINDOW_SECONDS * 1000L) {
                stats.lastMinuteRequests = 1; // 当前请求
                stats.lastRpmResetTime = now;
            }
        }
    }

    private String buildKey(Long providerId, int keyIndex) {
        return providerId + ":" + keyIndex;
    }

    /** 计算 key_hash（SHA-256 前 16 位） */
    public static String hashKey(String apiKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(apiKey.hashCode());
        }
    }

    // ========== 内部类 ==========

    private static class KeyStats {
        volatile int lastMinuteRequests = 0;
        volatile long lastRpmResetTime = System.currentTimeMillis();
        volatile long totalRequests = 0;
        volatile long totalFailures = 0;
        volatile long lastSuccessTime = 0;
        volatile long lastFailTime = 0;
        volatile long cooldownUntil = 0;
    }
}
