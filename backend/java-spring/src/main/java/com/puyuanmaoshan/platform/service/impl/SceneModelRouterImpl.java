package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.AiSceneDtos;
import com.puyuanmaoshan.platform.entity.AiProvider;
import com.puyuanmaoshan.platform.entity.AiScene;
import com.puyuanmaoshan.platform.entity.AiSceneModel;
import com.puyuanmaoshan.platform.mapper.AiProviderMapper;
import com.puyuanmaoshan.platform.mapper.AiSceneMapper;
import com.puyuanmaoshan.platform.mapper.AiSceneModelMapper;
import com.puyuanmaoshan.platform.service.ApiKeyPoolManager;
import com.puyuanmaoshan.platform.service.SceneModelRouter;
import com.puyuanmaoshan.platform.util.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景模型路由器实现
 * <p>
 * 根据场景编码查找主模型，支持：
 * - 多 API Key 轮询（round_robin / least_loaded / random）
 * - 故障自动切换到同提供商的其他 Key
 * - 跨提供商故障切换（主 → 备用）
 */
@Slf4j
@Service
public class SceneModelRouterImpl implements SceneModelRouter {

    private final AiSceneMapper aiSceneMapper;
    private final AiSceneModelMapper aiSceneModelMapper;
    private final AiProviderMapper aiProviderMapper;
    private final CryptoUtil cryptoUtil;
    private final ApiKeyPoolManager apiKeyPoolManager;
    private final ObjectMapper objectMapper;

    public SceneModelRouterImpl(AiSceneMapper aiSceneMapper,
                                 AiSceneModelMapper aiSceneModelMapper,
                                 AiProviderMapper aiProviderMapper,
                                 CryptoUtil cryptoUtil,
                                 ApiKeyPoolManager apiKeyPoolManager) {
        this.aiSceneMapper = aiSceneMapper;
        this.aiSceneModelMapper = aiSceneModelMapper;
        this.aiProviderMapper = aiProviderMapper;
        this.cryptoUtil = cryptoUtil;
        this.apiKeyPoolManager = apiKeyPoolManager;
        this.objectMapper = new ObjectMapper();
    }

    /** 场景模型缓存（sceneCode -> 解析结果列表） */
    private static final ConcurrentHashMap<String, List<ResolvedEntry>> sceneCache = new ConcurrentHashMap<>();

    /** 缓存 TTL：5 分钟 */
    private static final long CACHE_TTL_MS = 300_000L;

    @Override
    public AiSceneDtos.ModelResolution resolve(String sceneCode) {
        List<ResolvedEntry> entries = getSceneEntries(sceneCode);

        // 找主模型
        ResolvedEntry primary = entries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsPrimary()))
                .min(Comparator.comparingInt(ResolvedEntry::getPriority))
                .orElse(null);

        // 没有主模型时，取第一个启用的作为 fallback
        if (primary == null) {
            primary = entries.stream()
                    .min(Comparator.comparingInt(ResolvedEntry::getPriority))
                    .orElse(null);
        }

        if (primary == null) {
            log.warn("No model found for scene: {}", sceneCode);
            return null;
        }

        return buildResolutionWithKeySelection(primary, sceneCode);
    }

    @Override
    public AiSceneDtos.ModelResolution getFallback(String sceneCode, Long failedProviderId) {
        List<ResolvedEntry> entries = getSceneEntries(sceneCode);

        // 找备用模型（排除失败的 provider 和已用过的主模型）
        ResolvedEntry fallback = entries.stream()
                .filter(e -> !e.getProviderId().equals(failedProviderId))
                .filter(e -> Boolean.TRUE.equals(e.getIsFallback()) || !Boolean.TRUE.equals(e.getIsPrimary()))
                .min(Comparator.comparingInt(ResolvedEntry::getPriority))
                .orElse(null);

        if (fallback == null) {
            // 尝试任意其他 provider
            fallback = entries.stream()
                    .filter(e -> !e.getProviderId().equals(failedProviderId))
                    .min(Comparator.comparingInt(ResolvedEntry::getPriority))
                    .orElse(null);
        }

        if (fallback == null) {
            log.warn("No fallback model found for scene: {}, failedProvider: {}", sceneCode, failedProviderId);
            return null;
        }

        return buildResolutionWithKeySelection(fallback, sceneCode);
    }

    @Override
    public void evictCache() {
        sceneCache.clear();
        log.info("Scene model cache evicted");
    }

    /**
     * 通知 Key 调用成功
     */
    public void notifyKeySuccess(Long providerId, int keyIndex) {
        apiKeyPoolManager.recordSuccess(providerId, keyIndex);
    }

    /**
     * 通知 Key 调用失败
     */
    public void notifyKeyFailure(Long providerId, int keyIndex) {
        apiKeyPoolManager.recordFailure(providerId, keyIndex);
    }

    // ========== 私有方法 ==========

    private List<ResolvedEntry> getSceneEntries(String sceneCode) {
        // 检查缓存
        List<ResolvedEntry> cached = sceneCache.get(sceneCode);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 从 DB 加载
        AiScene scene = aiSceneMapper.selectOne(
                new LambdaQueryWrapper<AiScene>()
                        .eq(AiScene::getSceneCode, sceneCode)
                        .eq(AiScene::getEnabled, true)
        );
        if (scene == null) {
            log.warn("Scene not found or disabled: {}", sceneCode);
            return List.of();
        }

        List<AiSceneModel> bindings = aiSceneModelMapper.selectList(
                new LambdaQueryWrapper<AiSceneModel>()
                        .eq(AiSceneModel::getSceneId, scene.getId())
        );

        List<ResolvedEntry> entries = bindings.stream()
                .map(b -> {
                    AiProvider provider = aiProviderMapper.selectById(b.getProviderId());
                    if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
                        return null;
                    }
                    return new ResolvedEntry(
                            b.getId(),
                            b.getSceneId(),
                            b.getProviderId(),
                            b.getModelId(),
                            b.getIsPrimary(),
                            b.getIsFallback(),
                            b.getPriority() != null ? b.getPriority() : 99,
                            provider.getName(),
                            provider.getDisplayName(),
                            provider.getBaseUrl(),
                            provider.getApiKey(),
                            provider.getApiKeys(),
                            provider.getKeyStrategy(),
                            provider.getKeyMaxRpm() != null ? provider.getKeyMaxRpm() : 0
                    );
                })
                .filter(e -> e != null)
                .sorted(Comparator.comparingInt(ResolvedEntry::getPriority))
                .toList();

        // 缓存结果
        if (!entries.isEmpty()) {
            sceneCache.put(sceneCode, entries);
        }

        return entries;
    }

    /**
     * 构建 ModelResolution，同时通过多 Key 轮询选择 API Key
     */
    private AiSceneDtos.ModelResolution buildResolutionWithKeySelection(ResolvedEntry entry, String sceneCode) {
        String selectedApiKey;
        int selectedKeyIndex = 0;

        // 尝试多 Key 轮询
        List<String> decryptedKeys = decryptApiKeys(entry);
        if (decryptedKeys != null && decryptedKeys.size() > 1) {
            try {
                String strategy = entry.getKeyStrategy() != null ? entry.getKeyStrategy() : "round_robin";
                int maxRpm = entry.getKeyMaxRpm();
                selectedApiKey = apiKeyPoolManager.selectKey(entry.getProviderId(), decryptedKeys, strategy, maxRpm);
                // 找到选中 Key 的索引
                selectedKeyIndex = decryptedKeys.indexOf(selectedApiKey);
                log.debug("Multi-key selection: provider={}, strategy={}, selectedIndex={}",
                        entry.getProviderName(), strategy, selectedKeyIndex);
            } catch (IllegalStateException e) {
                log.error("All API keys exhausted for provider {}: {}", entry.getProviderName(), e.getMessage());
                // 降级到单 Key
                selectedApiKey = decryptedKeys.get(0);
            }
        } else if (decryptedKeys != null && decryptedKeys.size() == 1) {
            selectedApiKey = decryptedKeys.get(0);
        } else {
            // 兼容旧版单 api_key 字段
            selectedApiKey = entry.getApiKey();
            if (selectedApiKey != null && !selectedApiKey.isBlank()) {
                selectedApiKey = cryptoUtil.decrypt(selectedApiKey);
            }
        }

        return AiSceneDtos.ModelResolution.builder()
                .providerId(entry.getProviderId())
                .providerName(entry.getProviderName())
                .providerDisplayName(entry.getProviderDisplayName())
                .modelId(entry.getModelId())
                .baseUrl(entry.getBaseUrl())
                .apiKey(selectedApiKey)
                .sceneCode(sceneCode)
                .isPrimary(entry.getIsPrimary())
                .keyIndex(selectedKeyIndex)
                .totalKeys(decryptedKeys != null ? decryptedKeys.size() : 1)
                .build();
    }

    /**
     * 解密 api_keys JSON 数组
     */
    private List<String> decryptApiKeys(ResolvedEntry entry) {
        // 优先使用 api_keys（多 Key）
        String apiKeysJson = entry.getApiKeys();
        if (apiKeysJson != null && !apiKeysJson.isBlank()) {
            try {
                List<String> keys = objectMapper.readValue(apiKeysJson, new TypeReference<List<String>>() {});
                // 解密每个 Key
                return keys.stream()
                        .map(k -> {
                            try {
                                return cryptoUtil.decrypt(k);
                            } catch (Exception e) {
                                log.warn("Failed to decrypt key, using as-is: {}", e.getMessage());
                                return k;
                            }
                        })
                        .toList();
            } catch (Exception e) {
                log.warn("Failed to parse api_keys JSON for provider {}: {}", entry.getProviderName(), e.getMessage());
            }
        }

        // 回退到单 Key
        String singleKey = entry.getApiKey();
        if (singleKey != null && !singleKey.isBlank()) {
            return Collections.singletonList(cryptoUtil.decrypt(singleKey));
        }

        return null;
    }

    /**
     * 内部解析条目
     */
    @lombok.Value
    private static class ResolvedEntry {
        Long bindingId;
        Long sceneId;
        Long providerId;
        String modelId;
        Boolean isPrimary;
        Boolean isFallback;
        int priority;
        String providerName;
        String providerDisplayName;
        String baseUrl;
        String apiKey;       // 单 Key（向后兼容）
        String apiKeys;      // 多 Key JSON 数组
        String keyStrategy;  // 轮询策略
        int keyMaxRpm;       // 每个 Key 的最大 RPM
    }
}
