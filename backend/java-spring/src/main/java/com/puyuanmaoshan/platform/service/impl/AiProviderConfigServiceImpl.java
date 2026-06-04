package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.SystemConfigDtos.SaveConfigRequest;
import com.puyuanmaoshan.platform.entity.SystemConfig;
import com.puyuanmaoshan.platform.service.AiProviderConfigService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * AI 提供商全局配置服务实现
 * 优先从 DB (system_config, config_group='ai_provider') 读取，
 * 兜底使用 application.yml 中的配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderConfigServiceImpl implements AiProviderConfigService {

    private final SystemConfigService systemConfigService;
    private final CryptoUtil cryptoUtil;

    /** yml 兜底值 */
    @Value("${app.ai.base-url:https://api.siliconflow.cn/v1}")
    private String ymlBaseUrl;

    @Value("${app.ai.api-key:}")
    private String ymlApiKey;

    @Value("${plugin.default.ai-model:deepseek-ai/DeepSeek-V3}")
    private String ymlDefaultModel;

    private static final String CONFIG_GROUP = "ai_provider";

    @Override
    public String getBaseUrl() {
        String dbValue = systemConfigService.getConfigValue(CONFIG_GROUP, "base_url");
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue;
        }
        return ymlBaseUrl;
    }

    @Override
    public String getApiKey() {
        String dbValue = systemConfigService.getConfigValue(CONFIG_GROUP, "api_key");
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue;
        }
        return ymlApiKey;
    }

    @Override
    public String getDefaultModel() {
        String dbValue = systemConfigService.getConfigValue(CONFIG_GROUP, "default_model");
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue;
        }
        return ymlDefaultModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAiProviderConfig(String baseUrl, String apiKey, String defaultModel) {
        if (baseUrl != null) {
            saveOrUpdateConfig("base_url", baseUrl, 0, "AI 提供商 API 地址");
        }
        if (apiKey != null) {
            saveOrUpdateConfig("api_key", apiKey, 1, "AI 提供商 API Key（加密存储）");
        }
        if (defaultModel != null) {
            saveOrUpdateConfig("default_model", defaultModel, 2, "全局默认 AI 模型");
        }
        // 保存后清除缓存
        evictCaches();
        log.info("AI provider config updated and caches evicted");
    }

    @Override
    public void evictCaches() {
        // 清除 AdminAiModelController 中的模型列表缓存
        // 通过反射或直接调用静态方法清除
        try {
            Class<?> controllerClass = Class.forName(
                    "com.puyuanmaoshan.platform.controller.AdminAiModelController");
            java.lang.reflect.Method method = controllerClass.getDeclaredMethod("evictModelCache");
            method.setAccessible(true);
            method.invoke(null);
            log.info("Model cache evicted successfully");
        } catch (Exception e) {
            log.warn("Failed to evict model cache via reflection: {}", e.getMessage());
            // 降级：不清除缓存，下次请求时缓存过期自然刷新
        }
    }

    private void saveOrUpdateConfig(String configKey, String configValue, int sortOrder, String description) {
        // 查找是否已存在
        List<SystemConfig> existing = systemConfigService.lambdaQuery()
                .eq(SystemConfig::getConfigGroup, CONFIG_GROUP)
                .eq(SystemConfig::getConfigKey, configKey)
                .list();

        SaveConfigRequest request = SaveConfigRequest.builder()
                .configGroup(CONFIG_GROUP)
                .configKey(configKey)
                .configValue(configValue)
                .enabled(true)
                .sortOrder(sortOrder)
                .description(description)
                .build();

        if (!existing.isEmpty()) {
            request.setId(existing.get(0).getId());
        }

        systemConfigService.saveOrUpdateConfig(request);
    }
}
