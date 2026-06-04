package com.puyuanmaoshan.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AiProviderConfigService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiModelController {

    private final PluginService pluginService;
    private final AiProviderConfigService aiProviderConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 模型列表缓存 */
    private static final ConcurrentHashMap<String, CachedModels> modelsCache = new ConcurrentHashMap<>();

    /** 缓存有效期：1小时 */
    private static final long CACHE_TTL_MS = 3600_000L;

    public AdminAiModelController(PluginService pluginService,
                                  AiProviderConfigService aiProviderConfigService,
                                  RestTemplate restTemplate) {
        this.pluginService = pluginService;
        this.aiProviderConfigService = aiProviderConfigService;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取可用 AI 模型列表（从 AI 提供商的 /v1/models 接口获取，缓存 1 小时）
     */
    @GetMapping("/models")
    public ApiResponse<List<ApiModels.AiModelItem>> listModels(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String reqId = RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-models");

        synchronized (modelsCache) {
            CachedModels cached = modelsCache.get("global");
            if (cached != null && !cached.isExpired()) {
                log.info("Returning cached AI models list ({} models, cached at {})",
                        cached.models.size(), cached.cachedAt);
                return ApiResponse.ok(cached.models, reqId);
            }
        }

        // 缓存过期或不存在，重新获取
        List<ApiModels.AiModelItem> models = fetchModelsFromProvider();

        // 仅缓存非空结果（空结果可能是 API 调用失败，不应缓存）
        if (!models.isEmpty()) {
            List<ApiModels.AiModelItem> unmodifiable = Collections.unmodifiableList(models);
            synchronized (modelsCache) {
                modelsCache.put("global", new CachedModels(unmodifiable, System.currentTimeMillis()));
            }
            log.info("Fetched and cached AI models list ({} models)", models.size());
        } else {
            log.warn("AI models list is empty, not caching result");
        }

        return ApiResponse.ok(models, reqId);
    }

    /**
     * 更新插件的 AI 模型配置
     */
    @PutMapping("/plugins/{pluginId}/model")
    public ApiResponse<Map<String, Object>> updatePluginModel(
            @PathVariable String pluginId,
            @RequestBody ApiModels.UpdatePluginModelRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String reqId = RequestContextUtil.resolveRequestId(requestId, "req-admin-plugin-model-update");

        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .eq(Plugin::getStatus, 1)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        // ai_model 可为空字符串（表示使用默认模型）或具体模型 ID
        String aiModel = request.aiModel();
        plugin.setAiModel(aiModel != null && !aiModel.isBlank() ? aiModel : null);
        pluginService.updateById(plugin);

        Map<String, Object> data = new HashMap<>();
        data.put("plugin_id", pluginId);
        data.put("ai_model", plugin.getAiModel());
        data.put("default_model", aiProviderConfigService.getDefaultModel());
        log.info("Updated plugin {} ai_model to: {}", pluginId, plugin.getAiModel());

        return ApiResponse.ok(data, reqId);
    }

    /**
     * 从 AI 提供商获取模型列表
     * 优先从 DB (system_config, config_group='ai_provider') 读取 base_url 和 api_key
     */
    private List<ApiModels.AiModelItem> fetchModelsFromProvider() {
        // 优先从 DB 读取全局 AI 配置
        String apiKey = aiProviderConfigService.getApiKey();
        String endpoint = aiProviderConfigService.getBaseUrl();

        // DB 没有 api_key 时，尝试从 ai_text 分组提供商配置获取
        if (apiKey == null || apiKey.isEmpty()) {
            log.info("No global api_key configured, trying ai_text provider configs");
            apiKey = "";
            endpoint = aiProviderConfigService.getBaseUrl();
        }

        try {
            String url = endpoint + "/models";
            log.info("Fetching models from: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.setBearerAuth(apiKey);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();
            if (response == null) {
                log.warn("Models API returned null body");
                return Collections.emptyList();
            }

            JsonNode json = objectMapper.readTree(response);
            JsonNode dataNode = json.path("data");
            if (dataNode.isArray()) {
                List<ApiModels.AiModelItem> models = new ArrayList<>();
                for (JsonNode modelNode : dataNode) {
                    String id = modelNode.path("id").asText("");
                    String ownedBy = modelNode.path("owned_by").asText("");
                    if (!id.isEmpty()) {
                        models.add(new ApiModels.AiModelItem(id, ownedBy));
                    }
                }
                return models;
            }

            log.warn("Unexpected models API response format");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch models from AI provider: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 清除模型列表缓存（供外部调用，配置变更时触发）
     */
    public static void evictModelCache() {
        modelsCache.clear();
        log.info("AI model cache evicted");
    }

    /** 缓存包装类 */
    private static class CachedModels {
        final List<ApiModels.AiModelItem> models;
        final long cachedAt;

        CachedModels(List<ApiModels.AiModelItem> models, long cachedAt) {
            this.models = models;
            this.cachedAt = cachedAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
        }
    }
}