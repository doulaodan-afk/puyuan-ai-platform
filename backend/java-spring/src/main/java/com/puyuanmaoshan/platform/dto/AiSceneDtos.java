package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 场景相关 DTO
 */
public class AiSceneDtos {

    /**
     * 模型解析结果 — SceneModelRouter 返回
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelResolution {
        /** 提供商 ID */
        @JsonProperty("provider_id")
        private Long providerId;
        /** 提供商标识 */
        @JsonProperty("provider_name")
        private String providerName;
        /** 提供商显示名称 */
        @JsonProperty("provider_display_name")
        private String providerDisplayName;
        /** 模型 ID */
        @JsonProperty("model_id")
        private String modelId;
        /** API 基础地址 */
        @JsonProperty("base_url")
        private String baseUrl;
        /** API Key（明文） */
        @JsonProperty("api_key")
        private String apiKey;
        /** 场景编码 */
        @JsonProperty("scene_code")
        private String sceneCode;
        /** 是否为主模型 */
        @JsonProperty("is_primary")
        private Boolean isPrimary;
        /** 选中的 Key 在 Key 池中的索引（多 Key 轮询场景） */
        @JsonProperty("key_index")
        private Integer keyIndex;
        /** 该提供商可用的 Key 总数 */
        @JsonProperty("total_keys")
        private Integer totalKeys;
    }

    /**
     * AI 提供商保存/更新请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProviderRequest {
        private Long id;
        private String name;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("base_url")
        private String baseUrl;
        @JsonProperty("api_key")
        private String apiKey;
        private Boolean enabled;
        private Integer priority;
        private String description;
    }

    /**
     * AI 提供商响应（脱敏）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProviderResponse {
        private Long id;
        private String name;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("base_url")
        private String baseUrl;
        @JsonProperty("api_key")
        private String apiKey; // 脱敏
        private Boolean enabled;
        private Integer priority;
        private String description;
        @JsonProperty("has_api_key")
        private Boolean hasApiKey;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
    }

    /**
     * 场景-模型绑定请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneModelBindingRequest {
        @JsonProperty("scene_id")
        private Long sceneId;
        @JsonProperty("provider_id")
        private Long providerId;
        @JsonProperty("model_id")
        private String modelId;
        @JsonProperty("is_primary")
        private Boolean isPrimary;
        @JsonProperty("is_fallback")
        private Boolean isFallback;
        private Integer priority;
    }

    /**
     * 场景-模型绑定响应（含关联信息）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneModelBindingResponse {
        private Long id;
        @JsonProperty("scene_id")
        private Long sceneId;
        @JsonProperty("scene_code")
        private String sceneCode;
        @JsonProperty("scene_name")
        private String sceneName;
        @JsonProperty("provider_id")
        private Long providerId;
        @JsonProperty("provider_name")
        private String providerName;
        @JsonProperty("provider_display_name")
        private String providerDisplayName;
        @JsonProperty("model_id")
        private String modelId;
        @JsonProperty("is_primary")
        private Boolean isPrimary;
        @JsonProperty("is_fallback")
        private Boolean isFallback;
        private Integer priority;
    }

    /**
     * 场景概览（含主/备用模型信息）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneOverview {
        @JsonProperty("scene_id")
        private Long sceneId;
        @JsonProperty("scene_code")
        private String sceneCode;
        @JsonProperty("scene_name")
        private String sceneName;
        @JsonProperty("api_type")
        private String apiType;
        @JsonProperty("scene_description")
        private String sceneDescription;
        private Boolean enabled;
        private List<SceneModelBindingResponse> models; // 该场景的所有绑定模型
    }

    /**
     * AI 推荐模型请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendRequest {
        private String description; // 可选，自定义场景描述
    }

    /**
     * AI 推荐模型响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendResponse {
        @JsonProperty("recommended_models")
        private List<RecommendedModel> recommendedModels;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RecommendedModel {
            @JsonProperty("model_id")
            private String modelId;
            @JsonProperty("provider_id")
            private Long providerId;
            @JsonProperty("provider_name")
            private String providerName;
            private String reason;
        }
    }

    /**
     * 测试模型请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestModelRequest {
        @JsonProperty("provider_id")
        private Long providerId;
        @JsonProperty("model_id")
        private String modelId;
        private String prompt; // 可选，测试 prompt
    }

    /**
     * 测试模型响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestModelResponse {
        private Boolean success;
        private String message;
        private String result; // 模型返回结果
        @JsonProperty("latency_ms")
        private Long latencyMs;
    }
}
