package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.AiSceneDtos;

/**
 * 场景模型路由器接口
 * 根据场景编码获取对应的模型配置，支持主/备用模型自动切换
 */
public interface SceneModelRouter {

    /**
     * 根据场景编码解析模型配置（返回主模型）
     * @param sceneCode 场景编码（如 chat, image_gen, summarize）
     * @return 模型配置（包含 provider 信息、model、api_key、base_url）
     */
    AiSceneDtos.ModelResolution resolve(String sceneCode);

    /**
     * 获取备用模型配置（当主模型调用失败时使用）
     * @param sceneCode 场景编码
     * @param failedProviderId 失败的主模型提供商 ID
     * @return 备用模型配置，如果没有备用模型返回 null
     */
    AiSceneDtos.ModelResolution getFallback(String sceneCode, Long failedProviderId);

    /**
     * 清除所有场景模型缓存（配置变更时调用）
     */
    void evictCache();
}
