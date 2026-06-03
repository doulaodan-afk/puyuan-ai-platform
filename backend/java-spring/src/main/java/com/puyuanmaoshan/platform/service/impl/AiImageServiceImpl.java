package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiImageService;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock.enabled", havingValue = "false")
public class AiImageServiceImpl implements AiImageService {

    private final SystemConfigService systemConfigService;
    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.base-url:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    @Value("${app.ai.models.image:qwen-vl-max-2025-01-2}")
    private String imageModel;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiImageServiceImpl(
            SystemConfigService systemConfigService,
            AccountWalletService accountWalletService,
            SubscribeMessageService subscribeMessageService,
            RestTemplate restTemplate) {
        this.systemConfigService = systemConfigService;
        this.accountWalletService = accountWalletService;
        this.subscribeMessageService = subscribeMessageService;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateImage(String prompt, String size, long tenantId) {
        log.info("Generating image for tenant {}, size: {}, prompt: {}", tenantId, size, prompt);

        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();

        if (wallet == null || wallet.getTokenBalance() == null) {
            throw new RuntimeException("账户钱包不存在");
        }

        long balance = wallet.getTokenBalance();
        int tokenCost = calculateTokenCost(size);

        if (balance < tokenCost) {
            Long userId = RequestContextUtil.getUserId();
            if (userId != null) {
                subscribeMessageService.sendBalanceLowNotification(userId, tenantId);
            }
            throw new RuntimeException("余额不足，请先充值");
        }

        if (balance - tokenCost < balanceLowThreshold && balance - tokenCost >= 0) {
            Long userId = RequestContextUtil.getUserId();
            if (userId != null) {
                subscribeMessageService.sendBalanceLowNotification(userId, tenantId);
            }
        }

        String apiKey = aiApiKey;
        String endpoint = aiBaseUrl;
        String model = imageModel;

        try {
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("ai_image");
            for (Map<String, String> provider : providers) {
                String pApiKey = provider.get("api_key");
                String pEndpoint = provider.get("endpoint");
                String pModel = provider.get("model_name");
                if (pApiKey != null && !pApiKey.isEmpty()) {
                    apiKey = pApiKey;
                    if (pEndpoint != null) endpoint = pEndpoint;
                    if (pModel != null) model = pModel;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get AI image config from DB, using defaults: {}", e.getMessage());
        }

        try {
            String url = endpoint + "/images/generations";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "prompt", prompt,
                    "n", 1,
                    "size", size != null ? size : "1024x1024"
            ));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode json = objectMapper.readTree(response);
            JsonNode data = json.path("data");
            if (data.isArray() && data.size() > 0) {
                String imageUrl = data.get(0).path("url").asText();
                if (!imageUrl.isEmpty()) {
                    log.info("Image generated successfully: {}", imageUrl);
                    return imageUrl;
                }
                String b64Json = data.get(0).path("b64_json").asText();
                if (!b64Json.isEmpty()) {
                    log.info("Image generated as base64");
                    return "data:image/png;base64," + b64Json;
                }
            }

            log.error("Unexpected image API response: {}", response);
            throw new RuntimeException("AI图片生成失败：响应格式异常");
        } catch (Exception e) {
            log.error("AI image generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI图片生成失败：" + e.getMessage());
        }
    }

    @Override
    public int calculateTokenCost(String size) {
        int baseCost = 10;
        return switch (size) {
            case "1024x1024" -> baseCost * 2;
            case "1792x1024", "1024x1792" -> baseCost * 3;
            case "512x512" -> baseCost;
            default -> baseCost;
        };
    }
}