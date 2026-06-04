package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.SystemConfigDtos.*;
import com.puyuanmaoshan.platform.entity.SystemConfig;
import com.puyuanmaoshan.platform.service.AiProviderConfigService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.util.CryptoUtil;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置管理控制器
 */
@RestController
@RequestMapping("/api/v1/admin/system-config")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;
    private final CryptoUtil cryptoUtil;
    private final AiProviderConfigService aiProviderConfigService;

    /**
     * 获取所有配置分组
     */
    @GetMapping("/groups")
    public ApiResponse<List<Map<String, String>>> getGroups(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<Map<String, String>> groups = new ArrayList<>();

        Map<String, String> aiImage = new HashMap<>();
        aiImage.put("value", "ai_image");
        aiImage.put("label", "AI 图片生成");
        groups.add(aiImage);

        Map<String, String> aiText = new HashMap<>();
        aiText.put("value", "ai_text");
        aiText.put("label", "AI 文本生成");
        groups.add(aiText);

        Map<String, String> aiTranslate = new HashMap<>();
        aiTranslate.put("value", "ai_translate");
        aiTranslate.put("label", "AI 翻译");
        groups.add(aiTranslate);

        Map<String, String> oss = new HashMap<>();
        oss.put("value", "oss");
        oss.put("label", "对象存储 (OSS)");
        groups.add(oss);

        return ApiResponse.ok(groups, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-groups"));
    }

    /**
     * 获取指定分组的所有配置
     */
    @GetMapping("/list")
    public ApiResponse<List<ConfigResponse>> list(
            @RequestParam String group,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<SystemConfig> configs = systemConfigService.getGroupConfigs(group);
        List<ConfigResponse> responses = configs.stream()
                .map(systemConfigService::toConfigResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(responses, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-list"));
    }

    /**
     * 获取 AI 配置列表（按提供商分组）
     */
    @GetMapping("/ai")
    public ApiResponse<List<AiProviderConfig>> getAiConfigs(
            @RequestParam String group,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<Map<String, String>> providers = systemConfigService.getProviderConfigs(group);
        List<AiProviderConfig> configs = providers.stream()
                .map(p -> {
                    String apiKey = p.get("api_key");
                    return AiProviderConfig.builder()
                            .providerName(p.get("provider_name"))
                            .modelName(p.get("model_name"))
                            .apiKey(apiKey != null ? cryptoUtil.maskApiKey(apiKey) : null)
                            .endpoint(p.get("endpoint"))
                            .priority(p.get("priority") != null ? Integer.parseInt(p.get("priority")) : 1)
                            .enabled(p.get("enabled") != null ? Boolean.parseBoolean(p.get("enabled")) : true)
                            .configId(p.get("config_id") != null ? Long.parseLong(p.get("config_id")) : null)
                            .build();
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(configs, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-ai"));
    }

    /**
     * 获取 OSS 配置列表（按提供商分组）
     */
    @GetMapping("/oss")
    public ApiResponse<List<OssConfig>> getOssConfigs(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<Map<String, String>> providers = systemConfigService.getProviderConfigs("oss");
        List<OssConfig> configs = providers.stream()
                .map(p -> {
                    String accessKeyId = p.get("access_key_id");
                    String accessKeySecret = p.get("access_key_secret");
                    return OssConfig.builder()
                            .providerName(p.get("provider_name"))
                            .accessKeyId(accessKeyId != null ? CryptoUtil.maskKey(accessKeyId, 4, 4) : null)
                            .accessKeySecret(accessKeySecret != null ? CryptoUtil.maskKey(accessKeySecret, 4, 4) : null)
                            .endpoint(p.get("endpoint"))
                            .bucketName(p.get("bucket_name"))
                            .region(p.get("region"))
                            .priority(p.get("priority") != null ? Integer.parseInt(p.get("priority")) : 1)
                            .enabled(p.get("enabled") != null ? Boolean.parseBoolean(p.get("enabled")) : true)
                            .configId(p.get("config_id") != null ? Long.parseLong(p.get("config_id")) : null)
                            .build();
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(configs, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-oss"));
    }

    /**
     * 保存或更新配置
     */
    @PostMapping("/save")
    public ApiResponse<ConfigResponse> save(
            @RequestBody SaveConfigRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        SystemConfig config = systemConfigService.saveOrUpdateConfig(request);
        ConfigResponse response = systemConfigService.toConfigResponse(config);
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-save"));
    }

    /**
     * 批量保存 AI 提供商配置
     */
    @PostMapping("/ai/save")
    public ApiResponse<Map<String, Object>> saveAiConfig(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String group = (String) request.get("group");
        String providerName = (String) request.get("provider_name");
        String modelName = (String) request.get("model_name");
        String apiKey = (String) request.get("api_key");
        String endpoint = (String) request.get("endpoint");
        Integer priority = request.get("priority") != null ? ((Number) request.get("priority")).intValue() : 1;
        Boolean enabled = request.get("enabled") != null ? (Boolean) request.get("enabled") : true;

        // 保存配置
        SaveConfigRequest providerRequest = SaveConfigRequest.builder()
                .configGroup(group)
                .configKey("provider_name")
                .configValue(providerName)
                .enabled(enabled)
                .sortOrder(0)
                .description("AI provider name")
                .build();
        systemConfigService.saveOrUpdateConfig(providerRequest);

        SaveConfigRequest modelRequest = SaveConfigRequest.builder()
                .configGroup(group)
                .configKey("model_name")
                .configValue(modelName)
                .enabled(enabled)
                .sortOrder(1)
                .description("AI model name")
                .build();
        systemConfigService.saveOrUpdateConfig(modelRequest);

        SaveConfigRequest apiKeyRequest = SaveConfigRequest.builder()
                .configGroup(group)
                .configKey("api_key")
                .configValue(apiKey)
                .enabled(enabled)
                .sortOrder(2)
                .description("API key")
                .build();
        systemConfigService.saveOrUpdateConfig(apiKeyRequest);

        SaveConfigRequest endpointRequest = SaveConfigRequest.builder()
                .configGroup(group)
                .configKey("endpoint")
                .configValue(endpoint)
                .enabled(enabled)
                .sortOrder(3)
                .description("API endpoint")
                .build();
        systemConfigService.saveOrUpdateConfig(endpointRequest);

        SaveConfigRequest priorityRequest = SaveConfigRequest.builder()
                .configGroup(group)
                .configKey("priority")
                .configValue(String.valueOf(priority))
                .enabled(enabled)
                .sortOrder(4)
                .description("Config priority")
                .build();
        systemConfigService.saveOrUpdateConfig(priorityRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("status", "saved");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-ai-save"));
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        systemConfigService.deleteConfig(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", "deleted");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-delete"));
    }

    /**
     * 测试配置
     */
    @PostMapping("/test")
    public ApiResponse<TestConfigResponse> test(
            @RequestBody TestConfigRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        TestConfigResponse response = systemConfigService.testConfig(request.getId());
        return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-test"));
    }

    /**
     * 获取全局 AI 提供商配置（脱敏）
     */
    @GetMapping("/ai-provider")
    public ApiResponse<Map<String, String>> getAiProviderConfig(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String baseUrl = aiProviderConfigService.getBaseUrl();
        String apiKey = aiProviderConfigService.getApiKey();
        String defaultModel = aiProviderConfigService.getDefaultModel();

        Map<String, String> data = new HashMap<>();
        data.put("base_url", baseUrl);
        data.put("api_key", apiKey != null && !apiKey.isEmpty() ? cryptoUtil.maskApiKey(apiKey) : "");
        data.put("default_model", defaultModel);
        data.put("has_api_key", String.valueOf(apiKey != null && !apiKey.isEmpty()));

        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-ai-provider"));
    }

    /**
     * 保存全局 AI 提供商配置
     * PUT /api/admin/system-config/config/ai
     */
    @PutMapping("/config/ai")
    public ApiResponse<Map<String, String>> saveAiProviderConfig(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String baseUrl = request.get("base_url");
        String apiKey = request.get("api_key");
        String defaultModel = request.get("default_model");

        aiProviderConfigService.updateAiProviderConfig(baseUrl, apiKey, defaultModel);

        Map<String, String> data = new HashMap<>();
        data.put("status", "saved");
        data.put("caches_evicted", "true");
        data.put("base_url", baseUrl != null ? baseUrl : aiProviderConfigService.getBaseUrl());
        data.put("default_model", defaultModel != null ? defaultModel : aiProviderConfigService.getDefaultModel());
        data.put("has_api_key", String.valueOf(apiKey != null && !apiKey.isEmpty()));

        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-config-ai-save"));
    }
}
