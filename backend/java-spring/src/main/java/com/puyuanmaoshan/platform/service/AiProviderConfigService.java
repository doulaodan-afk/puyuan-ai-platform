package com.puyuanmaoshan.platform.service;

/**
 * AI 提供商全局配置服务
 * 从 system_config (config_group='ai_provider') 读取 base_url, api_key, default_model
 * 实现动态生效，无需重启
 */
public interface AiProviderConfigService {

    /**
     * 获取 AI 提供商 API 地址
     * @return base_url，未配置时返回默认值
     */
    String getBaseUrl();

    /**
     * 获取 AI 提供商 API Key（解密后）
     * @return api_key，未配置时返回空字符串
     */
    String getApiKey();

    /**
     * 获取全局默认 AI 模型
     * @return 默认模型 ID，未配置时返回 "deepseek-ai/DeepSeek-V3"
     */
    String getDefaultModel();

    /**
     * 更新 AI 提供商全局配置（批量）
     * @param baseUrl API 地址
     * @param apiKey API Key（明文，内部加密存储）
     * @param defaultModel 默认模型
     */
    void updateAiProviderConfig(String baseUrl, String apiKey, String defaultModel);

    /**
     * 清除相关缓存（模型列表缓存等）
     */
    void evictCaches();
}
