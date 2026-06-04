package com.puyuanmaoshan.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置相关 DTO
 */
public class SystemConfigDtos {

    /**
     * 配置项保存请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveConfigRequest {
        private Long id; // 更新时传入 ID，新增时不传
        private String configGroup;
        private String configKey;
        private String configValue; // 明文，后端会自动加密
        private Boolean enabled;
        private Integer sortOrder;
        private String description;
    }

    /**
     * 配置项响应（包含脱敏后的值）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigResponse {
        private Long id;
        private String configGroup;
        private String configKey;
        private String configValue; // 脱敏后的值
        private Boolean enabled;
        private Integer sortOrder;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 配置分组响应（用于展示同一组的所有配置）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigGroupResponse {
        private String configGroup;
        private String groupName;
        private java.util.List<ConfigResponse> configs;
    }

    /**
     * AI 提供商配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProviderConfig {
        private String providerName;
        private String modelName;
        private String apiKey; // 脱敏
        private String endpoint;
        private Integer priority;
        private Boolean enabled;
        private Long configId; // 用于标识配置记录
    }

    /**
     * OSS 配置（七牛云）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssConfig {
        private String providerName;
        private String accessKey; // 脱敏
        private String secretKey; // 脱敏
        private String bucket;
        private String cdnDomain;
        private Integer priority;
        private Boolean enabled;
        private Long configId;
    }

    /**
     * 测试配置请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestConfigRequest {
        private Long id;
    }

    /**
     * 测试配置响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestConfigResponse {
        private Boolean success;
        private String message;
        private Long latency; // 毫秒
    }
}
