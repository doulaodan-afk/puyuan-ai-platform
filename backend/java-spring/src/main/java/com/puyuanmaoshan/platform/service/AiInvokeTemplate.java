package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.AiSceneDtos;

/**
 * AI 调用模板接口
 * <p>
 * 封装多 Key 轮询重试 + 跨提供商故障转移的统一调用逻辑，
 * 各 AI Service 通过此模板调用，避免重复代码。
 */
public interface AiInvokeTemplate {

    /**
     * AI 调用上下文
     */
    @lombok.Data
    @lombok.Builder
    class CallContext {
        /** 场景编码 */
        private String sceneCode;
        /** 租户 ID */
        private long tenantId;
        /** 模型覆盖（可选） */
        private String modelOverride;
        /** 最大重试次数 */
        private int maxRetries;
    }

    /**
     * AI 调用执行器
     */
    @FunctionalInterface
    interface AiCallExecutor<T> {
        /**
         * 执行实际的 AI 调用
         * @param resolution 模型配置（含选中的 API Key）
         * @return 调用结果
         * @throws Exception 调用失败
         */
        T execute(AiSceneDtos.ModelResolution resolution) throws Exception;
    }

    /**
     * 执行 AI 调用（含多 Key 重试 + 跨提供商故障转移）
     *
     * @param ctx      调用上下文
     * @param executor 具体执行逻辑
     * @param <T>      返回类型
     * @return 调用结果
     * @throws RuntimeException 所有重试和故障转移都失败后抛出
     */
    <T> T invokeWithRetry(CallContext ctx, AiCallExecutor<T> executor);
}
