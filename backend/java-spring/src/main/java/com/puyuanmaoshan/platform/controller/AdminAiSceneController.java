package com.puyuanmaoshan.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.AiSceneDtos;
import com.puyuanmaoshan.platform.dto.AiSceneDtos.*;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.AiProvider;
import com.puyuanmaoshan.platform.entity.AiScene;
import com.puyuanmaoshan.platform.entity.AiSceneModel;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.mapper.AiProviderMapper;
import com.puyuanmaoshan.platform.mapper.AiSceneMapper;
import com.puyuanmaoshan.platform.mapper.AiSceneModelMapper;
import com.puyuanmaoshan.platform.service.SceneModelRouter;
import com.puyuanmaoshan.platform.util.CryptoUtil;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 场景模型管理控制器
 * 管理 AI 提供商、场景模型绑定、AI 推荐
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai-scene")
@RequiredArgsConstructor
public class AdminAiSceneController {

    private final AiProviderMapper aiProviderMapper;
    private final AiSceneMapper aiSceneMapper;
    private final AiSceneModelMapper aiSceneModelMapper;
    private final SceneModelRouter sceneModelRouter;
    private final CryptoUtil cryptoUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== AI 提供商 CRUD ====================

    /**
     * 获取所有 AI 提供商
     */
    @GetMapping("/providers")
    public ApiResponse<List<AiProviderResponse>> listProviders(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<AiProvider> providers = aiProviderMapper.selectList(
                new LambdaQueryWrapper<AiProvider>().orderByAsc(AiProvider::getPriority));
        List<AiProviderResponse> result = providers.stream()
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(result, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-providers"));
    }

    /**
     * 获取单个 AI 提供商
     */
    @GetMapping("/providers/{id}")
    public ApiResponse<AiProviderResponse> getProvider(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Provider not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider"));
        }
        return ApiResponse.ok(toProviderResponse(provider),
                RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider"));
    }

    /**
     * 创建 AI 提供商
     */
    @PostMapping("/providers")
    public ApiResponse<AiProviderResponse> createProvider(
            @RequestBody AiProviderRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        // 加密 API Key
        String encryptedKey = request.getApiKey() != null ? cryptoUtil.encrypt(request.getApiKey()) : "";

        AiProvider provider = AiProvider.builder()
                .name(request.getName())
                .displayName(request.getDisplayName())
                .baseUrl(request.getBaseUrl())
                .apiKey(encryptedKey)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .description(request.getDescription())
                .build();

        aiProviderMapper.insert(provider);
        sceneModelRouter.evictCache();

        return ApiResponse.ok(toProviderResponse(provider),
                RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-create"));
    }

    /**
     * 更新 AI 提供商
     */
    @PutMapping("/providers/{id}")
    public ApiResponse<AiProviderResponse> updateProvider(
            @PathVariable Long id,
            @RequestBody AiProviderRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Provider not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-update"));
        }

        if (request.getName() != null) provider.setName(request.getName());
        if (request.getDisplayName() != null) provider.setDisplayName(request.getDisplayName());
        if (request.getBaseUrl() != null) provider.setBaseUrl(request.getBaseUrl());
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            provider.setApiKey(cryptoUtil.encrypt(request.getApiKey()));
        }
        if (request.getEnabled() != null) provider.setEnabled(request.getEnabled());
        if (request.getPriority() != null) provider.setPriority(request.getPriority());
        if (request.getDescription() != null) provider.setDescription(request.getDescription());

        aiProviderMapper.updateById(provider);
        sceneModelRouter.evictCache();

        return ApiResponse.ok(toProviderResponse(provider),
                RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-update"));
    }

    /**
     * 删除 AI 提供商
     */
    @DeleteMapping("/providers/{id}")
    public ApiResponse<Map<String, Object>> deleteProvider(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        // 删除关联的场景模型绑定
        aiSceneModelMapper.delete(new LambdaQueryWrapper<AiSceneModel>()
                .eq(AiSceneModel::getProviderId, id));
        aiProviderMapper.deleteById(id);
        sceneModelRouter.evictCache();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", "deleted");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-delete"));
    }

    /**
     * 测试 AI 提供商连接
     */
    @PostMapping("/providers/{id}/test")
    public ApiResponse<TestModelResponse> testProvider(
            @PathVariable Long id,
            @RequestBody(required = false) TestModelRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            TestModelResponse resp = TestModelResponse.builder()
                    .success(false).message("Provider not found").build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-test"));
        }

        long start = System.currentTimeMillis();
        try {
            String apiKey = cryptoUtil.decrypt(provider.getApiKey());
            String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
            String url = baseUrl + "/models";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            long latency = System.currentTimeMillis() - start;

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode dataNode = json.path("data");
            int modelCount = dataNode.isArray() ? dataNode.size() : 0;

            TestModelResponse resp = TestModelResponse.builder()
                    .success(true)
                    .message("连接成功，可用模型数: " + modelCount)
                    .latencyMs(latency)
                    .build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-test"));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            TestModelResponse resp = TestModelResponse.builder()
                    .success(false)
                    .message("连接失败: " + e.getMessage())
                    .latencyMs(latency)
                    .build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-test"));
        }
    }

    /**
     * 获取指定提供商的可用模型列表
     * GET /api/v1/admin/ai-scene/providers/{id}/models
     */
    @GetMapping("/providers/{id}/models")
    public ApiResponse<List<ProviderModelEntry>> listProviderModels(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        try {
            AiProvider provider = aiProviderMapper.selectById(id);
            if (provider == null) {
                return ApiResponse.fail(ErrorCode.NOT_FOUND, "Provider not found",
                        RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-models"));
            }

            log.info("Fetching models for provider: id={}, name={}, baseUrl={}", id, provider.getName(), provider.getBaseUrl());

            List<ModelEntry> models = fetchProviderModels(provider);
            List<ProviderModelEntry> result = models.stream()
                    .map(m -> new ProviderModelEntry(m.getModelId(), m.getProviderName(), m.getProviderDisplayName()))
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} models for provider {}", result.size(), provider.getName());
            return ApiResponse.ok(result, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-models"));
        } catch (Exception e) {
            log.error("Failed to list provider models for provider id {}: {}", id, e.getMessage(), e);
            return ApiResponse.fail(ErrorCode.INTERNAL_ERROR, "获取模型列表失败: " + e.getMessage(),
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-provider-models"));
        }
    }

    // ==================== 场景管理 ====================

    /**
     * 获取所有场景定义
     */
    @GetMapping("/scenes")
    public ApiResponse<List<AiSceneDtos.SceneOverview>> listScenes(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<AiScene> scenes = aiSceneMapper.selectList(
                new LambdaQueryWrapper<AiScene>().eq(AiScene::getEnabled, true)
                        .orderByAsc(AiScene::getId));

        List<AiSceneDtos.SceneOverview> result = scenes.stream()
                .map(scene -> {
                    List<AiSceneModel> bindings = aiSceneModelMapper.selectList(
                            new LambdaQueryWrapper<AiSceneModel>()
                                    .eq(AiSceneModel::getSceneId, scene.getId())
                                    .orderByAsc(AiSceneModel::getPriority));

                    List<SceneModelBindingResponse> modelList = bindings.stream()
                            .map(b -> {
                                AiProvider provider = aiProviderMapper.selectById(b.getProviderId());
                                return SceneModelBindingResponse.builder()
                                        .id(b.getId())
                                        .sceneId(b.getSceneId())
                                        .sceneCode(scene.getSceneCode())
                                        .sceneName(scene.getSceneName())
                                        .providerId(b.getProviderId())
                                        .providerName(provider != null ? provider.getName() : "unknown")
                                        .providerDisplayName(provider != null ? provider.getDisplayName() : "unknown")
                                        .modelId(b.getModelId())
                                        .isPrimary(b.getIsPrimary())
                                        .isFallback(b.getIsFallback())
                                        .priority(b.getPriority())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return AiSceneDtos.SceneOverview.builder()
                            .sceneId(scene.getId())
                            .sceneCode(scene.getSceneCode())
                            .sceneName(scene.getSceneName())
                            .apiType(scene.getApiType())
                            .sceneDescription(scene.getSceneDescription())
                            .enabled(scene.getEnabled())
                            .models(modelList)
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.ok(result, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scenes"));
    }

    /**
     * 获取单个场景详情
     */
    @GetMapping("/scenes/{sceneCode}")
    public ApiResponse<AiSceneDtos.SceneOverview> getScene(
            @PathVariable String sceneCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiScene scene = aiSceneMapper.selectOne(
                new LambdaQueryWrapper<AiScene>().eq(AiScene::getSceneCode, sceneCode));
        if (scene == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Scene not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene"));
        }

        List<AiSceneModel> bindings = aiSceneModelMapper.selectList(
                new LambdaQueryWrapper<AiSceneModel>()
                        .eq(AiSceneModel::getSceneId, scene.getId())
                        .orderByAsc(AiSceneModel::getPriority));

        List<SceneModelBindingResponse> modelList = bindings.stream()
                .map(b -> {
                    AiProvider provider = aiProviderMapper.selectById(b.getProviderId());
                    return SceneModelBindingResponse.builder()
                            .id(b.getId())
                            .sceneId(b.getSceneId())
                            .sceneCode(scene.getSceneCode())
                            .sceneName(scene.getSceneName())
                            .providerId(b.getProviderId())
                            .providerName(provider != null ? provider.getName() : "unknown")
                            .providerDisplayName(provider != null ? provider.getDisplayName() : "unknown")
                            .modelId(b.getModelId())
                            .isPrimary(b.getIsPrimary())
                            .isFallback(b.getIsFallback())
                            .priority(b.getPriority())
                            .build();
                })
                .collect(Collectors.toList());

        AiSceneDtos.SceneOverview overview = AiSceneDtos.SceneOverview.builder()
                .sceneId(scene.getId())
                .sceneCode(scene.getSceneCode())
                .sceneName(scene.getSceneName())
                .apiType(scene.getApiType())
                .sceneDescription(scene.getSceneDescription())
                .enabled(scene.getEnabled())
                .models(modelList)
                .build();

        return ApiResponse.ok(overview, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene"));
    }

    // ==================== 场景-模型绑定 ====================

    /**
     * 为场景绑定模型
     */
    @PostMapping("/scenes/{sceneCode}/bind")
    public ApiResponse<SceneModelBindingResponse> bindModel(
            @PathVariable String sceneCode,
            @RequestBody SceneModelBindingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiScene scene = aiSceneMapper.selectOne(
                new LambdaQueryWrapper<AiScene>().eq(AiScene::getSceneCode, sceneCode));
        if (scene == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Scene not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-bind"));
        }

        // 如果设为主模型，先清除该场景下其他主模型标记
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            List<AiSceneModel> existingPrimary = aiSceneModelMapper.selectList(
                    new LambdaQueryWrapper<AiSceneModel>()
                            .eq(AiSceneModel::getSceneId, scene.getId())
                            .eq(AiSceneModel::getIsPrimary, true));
            for (AiSceneModel ep : existingPrimary) {
                ep.setIsPrimary(false);
                aiSceneModelMapper.updateById(ep);
            }
        }

        AiSceneModel binding = AiSceneModel.builder()
                .sceneId(scene.getId())
                .providerId(request.getProviderId())
                .modelId(request.getModelId())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .isFallback(request.getIsFallback() != null ? request.getIsFallback() : false)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .build();

        aiSceneModelMapper.insert(binding);
        sceneModelRouter.evictCache();

        AiProvider provider = aiProviderMapper.selectById(request.getProviderId());
        SceneModelBindingResponse resp = SceneModelBindingResponse.builder()
                .id(binding.getId())
                .sceneId(binding.getSceneId())
                .sceneCode(scene.getSceneCode())
                .sceneName(scene.getSceneName())
                .providerId(binding.getProviderId())
                .providerName(provider != null ? provider.getName() : "unknown")
                .providerDisplayName(provider != null ? provider.getDisplayName() : "unknown")
                .modelId(binding.getModelId())
                .isPrimary(binding.getIsPrimary())
                .isFallback(binding.getIsFallback())
                .priority(binding.getPriority())
                .build();

        return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-bind"));
    }

    /**
     * 更新场景-模型绑定
     */
    @PutMapping("/scenes/bindings/{bindingId}")
    public ApiResponse<SceneModelBindingResponse> updateBinding(
            @PathVariable Long bindingId,
            @RequestBody SceneModelBindingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiSceneModel binding = aiSceneModelMapper.selectById(bindingId);
        if (binding == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Binding not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-binding-update"));
        }

        // 如果设为主模型，先清除该场景下其他主模型标记
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(binding.getIsPrimary())) {
            List<AiSceneModel> existingPrimary = aiSceneModelMapper.selectList(
                    new LambdaQueryWrapper<AiSceneModel>()
                            .eq(AiSceneModel::getSceneId, binding.getSceneId())
                            .eq(AiSceneModel::getIsPrimary, true));
            for (AiSceneModel ep : existingPrimary) {
                ep.setIsPrimary(false);
                aiSceneModelMapper.updateById(ep);
            }
        }

        if (request.getModelId() != null) binding.setModelId(request.getModelId());
        if (request.getIsPrimary() != null) binding.setIsPrimary(request.getIsPrimary());
        if (request.getIsFallback() != null) binding.setIsFallback(request.getIsFallback());
        if (request.getPriority() != null) binding.setPriority(request.getPriority());

        aiSceneModelMapper.updateById(binding);
        sceneModelRouter.evictCache();

        AiScene scene = aiSceneMapper.selectById(binding.getSceneId());
        AiProvider provider = aiProviderMapper.selectById(binding.getProviderId());
        SceneModelBindingResponse resp = SceneModelBindingResponse.builder()
                .id(binding.getId())
                .sceneId(binding.getSceneId())
                .sceneCode(scene != null ? scene.getSceneCode() : "")
                .sceneName(scene != null ? scene.getSceneName() : "")
                .providerId(binding.getProviderId())
                .providerName(provider != null ? provider.getName() : "unknown")
                .providerDisplayName(provider != null ? provider.getDisplayName() : "unknown")
                .modelId(binding.getModelId())
                .isPrimary(binding.getIsPrimary())
                .isFallback(binding.getIsFallback())
                .priority(binding.getPriority())
                .build();

        return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-binding-update"));
    }

    /**
     * 解除场景-模型绑定
     */
    @DeleteMapping("/scenes/bindings/{bindingId}")
    public ApiResponse<Map<String, Object>> unbindModel(
            @PathVariable Long bindingId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        aiSceneModelMapper.deleteById(bindingId);
        sceneModelRouter.evictCache();

        Map<String, Object> data = new HashMap<>();
        data.put("bindingId", bindingId);
        data.put("status", "unbound");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-unbind"));
    }

    // ==================== 设置主/备用模型 ====================

    /**
     * 设置主模型（将指定的绑定设为主模型）
     */
    @PutMapping("/scenes/{sceneCode}/set-primary/{bindingId}")
    public ApiResponse<SceneModelBindingResponse> setPrimary(
            @PathVariable String sceneCode,
            @PathVariable Long bindingId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiScene scene = aiSceneMapper.selectOne(
                new LambdaQueryWrapper<AiScene>().eq(AiScene::getSceneCode, sceneCode));
        if (scene == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Scene not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-set-primary"));
        }

        // 清除该场景下所有主模型
        List<AiSceneModel> existingPrimary = aiSceneModelMapper.selectList(
                new LambdaQueryWrapper<AiSceneModel>()
                        .eq(AiSceneModel::getSceneId, scene.getId())
                        .eq(AiSceneModel::getIsPrimary, true));
        for (AiSceneModel ep : existingPrimary) {
            ep.setIsPrimary(false);
            aiSceneModelMapper.updateById(ep);
        }

        // 设置新的主模型
        AiSceneModel binding = aiSceneModelMapper.selectById(bindingId);
        if (binding == null || !binding.getSceneId().equals(scene.getId())) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Binding not found or not belong to this scene",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-set-primary"));
        }
        binding.setIsPrimary(true);
        aiSceneModelMapper.updateById(binding);
        sceneModelRouter.evictCache();

        AiProvider provider = aiProviderMapper.selectById(binding.getProviderId());
        SceneModelBindingResponse resp = SceneModelBindingResponse.builder()
                .id(binding.getId())
                .sceneId(binding.getSceneId())
                .sceneCode(scene.getSceneCode())
                .sceneName(scene.getSceneName())
                .providerId(binding.getProviderId())
                .providerName(provider != null ? provider.getName() : "unknown")
                .modelId(binding.getModelId())
                .isPrimary(true)
                .isFallback(binding.getIsFallback())
                .priority(binding.getPriority())
                .build();

        return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-set-primary"));
    }

    /**
     * 设置备用模型
     */
    @PutMapping("/scenes/{sceneCode}/set-fallback/{bindingId}")
    public ApiResponse<SceneModelBindingResponse> setFallback(
            @PathVariable String sceneCode,
            @PathVariable Long bindingId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiScene scene = aiSceneMapper.selectOne(
                new LambdaQueryWrapper<AiScene>().eq(AiScene::getSceneCode, sceneCode));
        if (scene == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Scene not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-set-fallback"));
        }

        AiSceneModel binding = aiSceneModelMapper.selectById(bindingId);
        if (binding == null || !binding.getSceneId().equals(scene.getId())) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Binding not found or not belong to this scene",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-set-fallback"));
        }
        binding.setIsFallback(true);
        aiSceneModelMapper.updateById(binding);
        sceneModelRouter.evictCache();

        AiProvider provider = aiProviderMapper.selectById(binding.getProviderId());
        SceneModelBindingResponse resp = SceneModelBindingResponse.builder()
                .id(binding.getId())
                .sceneId(binding.getSceneId())
                .sceneCode(scene.getSceneCode())
                .sceneName(scene.getSceneName())
                .providerId(binding.getProviderId())
                .providerName(provider != null ? provider.getName() : "unknown")
                .modelId(binding.getModelId())
                .isPrimary(binding.getIsPrimary())
                .isFallback(true)
                .priority(binding.getPriority())
                .build();

        return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-scene-set-fallback"));
    }

    // ==================== AI 推荐模型 ====================

    /**
     * AI 自动推荐模型
     * POST /api/admin/ai/scenes/{sceneCode}/recommend
     */
    @PostMapping("/scenes/{sceneCode}/recommend")
    public ApiResponse<RecommendResponse> recommendModels(
            @PathVariable String sceneCode,
            @RequestBody(required = false) RecommendRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiScene scene = aiSceneMapper.selectOne(
                new LambdaQueryWrapper<AiScene>().eq(AiScene::getSceneCode, sceneCode));
        if (scene == null) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, "Scene not found",
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-recommend"));
        }

        // 获取所有启用的提供商
        List<AiProvider> providers = aiProviderMapper.selectList(
                new LambdaQueryWrapper<AiProvider>().eq(AiProvider::getEnabled, true)
                        .orderByAsc(AiProvider::getPriority));

        if (providers.isEmpty()) {
            RecommendResponse resp = RecommendResponse.builder()
                    .recommendedModels(List.of())
                    .build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-recommend"));
        }

        // 收集所有可用模型
        List<ModelEntry> allModels = new ArrayList<>();
        for (AiProvider provider : providers) {
            try {
                List<ModelEntry> providerModels = fetchProviderModels(provider);
                allModels.addAll(providerModels);
            } catch (Exception e) {
                log.warn("Failed to fetch models from provider {}: {}", provider.getName(), e.getMessage());
            }
        }

        if (allModels.isEmpty()) {
            RecommendResponse resp = RecommendResponse.builder()
                    .recommendedModels(List.of())
                    .build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-recommend"));
        }

        // 规则预筛选：根据 api_type 过滤模型
        List<ModelEntry> filtered = filterByApiType(allModels, scene.getApiType());
        if (filtered.isEmpty()) {
            filtered = allModels; // 没有匹配的，使用全部
        }

        // 使用 AI 推荐（调用 chat 场景的默认模型进行分析）
        List<RecommendedModelEntry> recommended;
        try {
            recommended = callAiForRecommendation(scene, filtered, request);
        } catch (Exception e) {
            log.warn("AI recommendation failed, using rule-based fallback: {}", e.getMessage());
            recommended = ruleBasedRecommend(filtered, 3);
        }

        List<RecommendResponse.RecommendedModel> result = recommended.stream()
                .map(r -> RecommendResponse.RecommendedModel.builder()
                        .modelId(r.getModelId())
                        .providerId(r.getProviderId())
                        .providerName(r.getProviderName())
                        .reason(r.getReason())
                        .build())
                .collect(Collectors.toList());

        RecommendResponse resp = RecommendResponse.builder()
                .recommendedModels(result)
                .build();
        return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-recommend"));
    }

    // ==================== 测试模型调用 ====================

    /**
     * 测试模型调用
     */
    @PostMapping("/scenes/{sceneCode}/test")
    public ApiResponse<TestModelResponse> testModel(
            @PathVariable String sceneCode,
            @RequestBody TestModelRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        AiProvider provider = aiProviderMapper.selectById(request.getProviderId());
        if (provider == null) {
            return ApiResponse.ok(TestModelResponse.builder()
                    .success(false).message("Provider not found").build(),
                    RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-test-model"));
        }

        long start = System.currentTimeMillis();
        try {
            String apiKey = cryptoUtil.decrypt(provider.getApiKey());
            String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
            String url = baseUrl + "/chat/completions";

            String testPrompt = request.getPrompt() != null ? request.getPrompt() : "Hello, please respond with 'OK'.";
            String model = request.getModelId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", testPrompt)),
                    "max_tokens", 50
            ));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            long latency = System.currentTimeMillis() - start;

            JsonNode json = objectMapper.readTree(response.getBody());
            String content = json.path("choices").path(0).path("message").path("content").asText("");

            TestModelResponse resp = TestModelResponse.builder()
                    .success(true)
                    .message("调用成功")
                    .result(content)
                    .latencyMs(latency)
                    .build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-test-model"));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            TestModelResponse resp = TestModelResponse.builder()
                    .success(false)
                    .message("调用失败: " + e.getMessage())
                    .latencyMs(latency)
                    .build();
            return ApiResponse.ok(resp, RequestContextUtil.resolveRequestId(requestId, "req-admin-ai-test-model"));
        }
    }

    // ==================== 私有辅助方法 ====================

    private AiProviderResponse toProviderResponse(AiProvider provider) {
        String maskedKey = "";
        boolean hasKey = false;
        if (provider.getApiKey() != null && !provider.getApiKey().isEmpty()) {
            hasKey = true;
            String decrypted = cryptoUtil.decrypt(provider.getApiKey());
            maskedKey = CryptoUtil.maskApiKey(decrypted);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return AiProviderResponse.builder()
                .id(provider.getId())
                .name(provider.getName())
                .displayName(provider.getDisplayName())
                .baseUrl(provider.getBaseUrl())
                .apiKey(maskedKey)
                .enabled(provider.getEnabled())
                .priority(provider.getPriority())
                .description(provider.getDescription())
                .hasApiKey(hasKey)
                .createdAt(provider.getCreatedAt() != null ? provider.getCreatedAt().format(fmt) : null)
                .updatedAt(provider.getUpdatedAt() != null ? provider.getUpdatedAt().format(fmt) : null)
                .build();
    }

    /**
     * 从提供商获取模型列表
     */
    private List<ModelEntry> fetchProviderModels(AiProvider provider) {
        try {
            String apiKey = cryptoUtil.decrypt(provider.getApiKey());
            // 确保 baseUrl 去掉末尾斜杠，避免拼接出双斜杠
            String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
            String url = baseUrl + "/models";
            log.info("Calling provider models API: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getBody() == null) {
                log.warn("Empty response body from provider {}", provider.getName());
                return List.of();
            }

            log.debug("Provider {} response status: {}", provider.getName(), response.getStatusCode());
            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode dataNode = json.path("data");
            List<ModelEntry> models = new ArrayList<>();
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    String id = node.path("id").asText("");
                    if (!id.isEmpty()) {
                        models.add(new ModelEntry(id, provider.getId(), provider.getName(), provider.getDisplayName()));
                    }
                }
            }
            return models;
        } catch (Exception e) {
            log.error("Failed to fetch models from provider {} (baseUrl={}): {}", 
                    provider.getName(), provider.getBaseUrl(), e.getMessage(), e);
            throw new RuntimeException("获取模型列表失败: " + provider.getName() + " - " + e.getMessage(), e);
        }
    }

    /**
     * 根据 api_type 规则预筛选模型
     */
    private List<ModelEntry> filterByApiType(List<ModelEntry> models, String apiType) {
        return switch (apiType) {
            case "image_generation" -> models.stream()
                    .filter(m -> {
                        String id = m.getModelId().toLowerCase();
                        return id.contains("image") || id.contains("dall") || id.contains("flux")
                                || id.contains("sd") || id.contains("stable") || id.contains("midjourney")
                                || id.contains("illust");
                    })
                    .collect(Collectors.toList());
            case "speech_to_text" -> models.stream()
                    .filter(m -> {
                        String id = m.getModelId().toLowerCase();
                        return id.contains("whisper") || id.contains("speech") || id.contains("asr")
                                || id.contains("audio") || id.contains("stt") || id.contains("sensevoice");
                    })
                    .collect(Collectors.toList());
            case "video_understanding" -> models.stream()
                    .filter(m -> {
                        String id = m.getModelId().toLowerCase();
                        return id.contains("video") || id.contains("vl") || id.contains("vision")
                                || id.contains("multimodal");
                    })
                    .collect(Collectors.toList());
            case "image_understanding" -> models.stream()
                    .filter(m -> {
                        String id = m.getModelId().toLowerCase();
                        return id.contains("vl") || id.contains("vision") || id.contains("image")
                                || id.contains("multimodal") || id.contains("qwenvl") || id.contains("glm-4v");
                    })
                    .collect(Collectors.toList());
            default -> models; // chat_completion 等：全部保留
        };
    }

    /**
     * 调用 AI 推荐模型（使用 chat 场景的主模型）
     */
    private List<RecommendedModelEntry> callAiForRecommendation(
            AiScene scene, List<ModelEntry> candidates, RecommendRequest request) {
        // 获取 chat 场景的默认模型来调用推荐
        ModelResolution chatModel = sceneModelRouter.resolve("chat");
        if (chatModel == null) {
            throw new RuntimeException("No chat model available for recommendation");
        }

        String sceneDesc = (request != null && request.getDescription() != null)
                ? request.getDescription() : scene.getSceneDescription();

        // 构建候选模型列表文本
        StringBuilder modelsList = new StringBuilder();
        for (int i = 0; i < Math.min(candidates.size(), 50); i++) {
            ModelEntry m = candidates.get(i);
            modelsList.append("- ").append(m.getModelId())
                    .append(" (provider: ").append(m.getProviderName()).append(")\n");
        }

        // 使用自定义推荐 prompt 或默认 prompt
        String promptTemplate = scene.getRecommendationPrompt();
        if (promptTemplate == null || promptTemplate.isBlank()) {
            promptTemplate = "你是一个AI模型选型专家。根据场景类型\"{scene_name}\"（{scene_description}，api_type={api_type}），"
                    + "从以下可用模型列表中选择最适合的3个模型，按推荐优先级排序。"
                    + "请返回严格JSON格式，不要有任何额外文本：\n"
                    + "{\"recommended_models\":[{\"model_id\":\"模型ID\",\"reason\":\"推荐理由\"}]}";
        }

        String prompt = promptTemplate
                .replace("{scene_code}", scene.getSceneCode())
                .replace("{scene_name}", scene.getSceneName())
                .replace("{scene_description}", sceneDesc != null ? sceneDesc : "")
                .replace("{api_type}", scene.getApiType());

        String fullPrompt = prompt + "\n\n可用模型列表：\n" + modelsList;

        try {
            String baseUrl = chatModel.getBaseUrl().replaceAll("/+$", "");
            String url = baseUrl + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(chatModel.getApiKey());

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", chatModel.getModelId(),
                    "messages", List.of(Map.of("role", "user", "content", fullPrompt)),
                    "temperature", 0.3,
                    "max_tokens", 1000,
                    "response_format", Map.of("type", "json_object")
            ));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String content = json.path("choices").path(0).path("message").path("content").asText("");

            // 解析 JSON 响应
            JsonNode result = objectMapper.readTree(content);
            JsonNode recommendedArray = result.path("recommended_models");
            List<RecommendedModelEntry> recommendations = new ArrayList<>();

            if (recommendedArray.isArray()) {
                for (JsonNode node : recommendedArray) {
                    String modelId = node.path("model_id").asText("");
                    String reason = node.path("reason").asText("");

                    // 匹配 provider
                    ModelEntry matched = candidates.stream()
                            .filter(c -> c.getModelId().equals(modelId))
                            .findFirst().orElse(null);

                    if (!modelId.isEmpty()) {
                        recommendations.add(new RecommendedModelEntry(
                                modelId,
                                matched != null ? matched.getProviderId() : 0L,
                                matched != null ? matched.getProviderName() : "unknown",
                                reason
                        ));
                    }
                }
            }

            return recommendations.size() >= 3 ? recommendations.subList(0, 3)
                    : recommendations.isEmpty() ? ruleBasedRecommend(candidates, 3) : recommendations;
        } catch (Exception e) {
            log.warn("AI recommendation call failed: {}", e.getMessage());
            return ruleBasedRecommend(candidates, 3);
        }
    }

    /**
     * 基于规则的降级推荐
     */
    private List<RecommendedModelEntry> ruleBasedRecommend(List<ModelEntry> candidates, int count) {
        return candidates.stream()
                .limit(count)
                .map(m -> new RecommendedModelEntry(
                        m.getModelId(), m.getProviderId(), m.getProviderName(),
                        "基于规则筛选的推荐模型"))
                .collect(Collectors.toList());
    }

    // ==================== 内部数据类 ====================

    @lombok.Value
    private static class ModelEntry {
        String modelId;
        Long providerId;
        String providerName;
        String providerDisplayName;
    }

    @lombok.Value
    private static class RecommendedModelEntry {
        String modelId;
        Long providerId;
        String providerName;
        String reason;
    }

    @lombok.Value
    public static class ProviderModelEntry {
        @com.fasterxml.jackson.annotation.JsonProperty("model_id")
        String modelId;
        @com.fasterxml.jackson.annotation.JsonProperty("provider_name")
        String providerName;
        @com.fasterxml.jackson.annotation.JsonProperty("provider_display_name")
        String providerDisplayName;
    }
}
