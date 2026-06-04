package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.AiSceneDtos;
import com.puyuanmaoshan.platform.service.AiInvokeTemplate;
import com.puyuanmaoshan.platform.service.SceneModelRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 调用模板实现
 * <p>
 * 调用流程：
 * 1. 通过 SceneModelRouter.resolve(sceneCode) 获取主模型 + 选中的 API Key
 * 2. 执行调用，成功则通知 Key 成功并返回
 * 3. 调用失败（限流/超时/网络错误）：
 *    a. 通知当前 Key 失败（进入冷却）
 *    b. 尝试同提供商的其他 Key（SceneModelRouter 自动轮询）
 *    c. 同提供商所有 Key 都失败后，getFallback 切换到备用提供商
 *    d. 最多重试 maxRetries 次
 */
@Slf4j
@Service
public class AiInvokeTemplateImpl implements AiInvokeTemplate {

    private final SceneModelRouter sceneModelRouter;
    private final SceneModelRouterImpl sceneModelRouterImpl;

    public AiInvokeTemplateImpl(SceneModelRouter sceneModelRouter) {
        this.sceneModelRouter = sceneModelRouter;
        // 获取实现类实例以调用 notify 方法
        this.sceneModelRouterImpl = (sceneModelRouter instanceof SceneModelRouterImpl impl) ? impl : null;
    }

    @Override
    public <T> T invokeWithRetry(CallContext ctx, AiCallExecutor<T> executor) {
        String sceneCode = ctx.getSceneCode();
        int maxRetries = ctx.getMaxRetries() > 0 ? ctx.getMaxRetries() : 3;

        // 第一步：获取主模型
        AiSceneDtos.ModelResolution resolution = sceneModelRouter.resolve(sceneCode);
        if (resolution == null) {
            throw new RuntimeException("未配置场景模型: " + sceneCode + "，请先在管理端配置");
        }

        log.info("AI invoke: scene={}, provider={}, model={}, keyIndex={}/{}, maxRetries={}",
                sceneCode, resolution.getProviderName(), resolution.getModelId(),
                resolution.getKeyIndex(), resolution.getTotalKeys(), maxRetries);

        Long currentProviderId = resolution.getProviderId();
        int keyIndex = resolution.getKeyIndex() != null ? resolution.getKeyIndex() : 0;

        // 主模型重试循环（含同提供商多 Key 轮询）
        for (int retry = 0; retry <= maxRetries; retry++) {
            try {
                T result = executor.execute(resolution);
                // 成功：通知 Key 成功
                notifySuccess(currentProviderId, keyIndex);
                return result;
            } catch (Exception e) {
                log.warn("AI call failed (attempt {}/{}): provider={}, keyIndex={}, error={}",
                        retry + 1, maxRetries + 1, resolution.getProviderName(), keyIndex, e.getMessage());

                // 通知 Key 失败（进入冷却）
                notifyFailure(currentProviderId, keyIndex);

                if (retry >= maxRetries) {
                    // 主模型所有重试耗尽，尝试跨提供商故障转移
                    break;
                }

                // 同提供商内重试：重新 resolve 以获取下一个可用 Key
                AiSceneDtos.ModelResolution retryResolution = sceneModelRouter.resolve(sceneCode);
                if (retryResolution != null
                        && retryResolution.getProviderId().equals(currentProviderId)
                        && !retryResolution.getApiKey().equals(resolution.getApiKey())) {
                    // 同提供商的不同 Key，继续重试
                    resolution = retryResolution;
                    keyIndex = retryResolution.getKeyIndex() != null ? retryResolution.getKeyIndex() : 0;
                    log.info("Retrying with same provider, different key: keyIndex={}", keyIndex);
                } else {
                    // 同提供商已无可用 Key
                    log.warn("No more keys available for provider {}", currentProviderId);
                    break;
                }
            }
        }

        // 第二步：跨提供商故障转移
        log.info("Attempting cross-provider fallback for scene: {}, failedProvider: {}", sceneCode, currentProviderId);
        AiSceneDtos.ModelResolution fallback = sceneModelRouter.getFallback(sceneCode, currentProviderId);
        if (fallback == null) {
            throw new RuntimeException("AI 调用失败：主备模型均不可用 (scene=" + sceneCode + ")");
        }

        log.info("Falling back to provider: {}, model: {}", fallback.getProviderName(), fallback.getModelId());

        // 备用模型重试（最多 1 次额外重试）
        for (int retry = 0; retry <= 1; retry++) {
            try {
                T result = executor.execute(fallback);
                notifySuccess(fallback.getProviderId(), fallback.getKeyIndex() != null ? fallback.getKeyIndex() : 0);
                return result;
            } catch (Exception e) {
                log.warn("Fallback call failed (attempt {}): provider={}, error={}",
                        retry + 1, fallback.getProviderName(), e.getMessage());
                notifyFailure(fallback.getProviderId(), fallback.getKeyIndex() != null ? fallback.getKeyIndex() : 0);

                if (retry >= 1) break;

                // 尝试备用提供商的其他 Key
                AiSceneDtos.ModelResolution fbRetry = sceneModelRouter.getFallback(sceneCode, currentProviderId);
                if (fbRetry != null && !fbRetry.getApiKey().equals(fallback.getApiKey())) {
                    fallback = fbRetry;
                }
            }
        }

        throw new RuntimeException("AI 调用失败：所有模型和 Key 均已尝试 (scene=" + sceneCode + ")");
    }

    private void notifySuccess(Long providerId, int keyIndex) {
        if (sceneModelRouterImpl != null) {
            sceneModelRouterImpl.notifyKeySuccess(providerId, keyIndex);
        }
    }

    private void notifyFailure(Long providerId, int keyIndex) {
        if (sceneModelRouterImpl != null) {
            sceneModelRouterImpl.notifyKeyFailure(providerId, keyIndex);
        }
    }
}
