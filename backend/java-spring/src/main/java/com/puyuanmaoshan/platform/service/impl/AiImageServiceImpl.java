package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiImageService;
import com.puyuanmaoshan.platform.service.AiInvokeTemplate;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "false")
public class AiImageServiceImpl implements AiImageService {

    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;
    private final AiInvokeTemplate aiInvokeTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiImageServiceImpl(
            AccountWalletService accountWalletService,
            SubscribeMessageService subscribeMessageService,
            AiInvokeTemplate aiInvokeTemplate,
            RestTemplate restTemplate) {
        this.accountWalletService = accountWalletService;
        this.subscribeMessageService = subscribeMessageService;
        this.aiInvokeTemplate = aiInvokeTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateImage(String prompt, String size, long tenantId, String modelOverride) {
        log.info("Generating image for tenant {}, size: {}, prompt: {}, modelOverride: {}", tenantId, size, prompt, modelOverride);

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

        String effectiveModel = modelOverride;

        // 使用 AiInvokeTemplate 统一处理多 Key 轮询 + 故障转移
        return aiInvokeTemplate.invokeWithRetry(
                AiInvokeTemplate.CallContext.builder()
                        .sceneCode("image_gen")
                        .tenantId(tenantId)
                        .modelOverride(modelOverride)
                        .maxRetries(2)
                        .build(),
                resolution -> {
                    String model = effectiveModel != null && !effectiveModel.isEmpty()
                            ? effectiveModel : resolution.getModelId();
                    log.info("Calling image generation: model={} @ {} (provider: {}, keyIndex: {})",
                            model, resolution.getBaseUrl(), resolution.getProviderName(), resolution.getKeyIndex());
                    return callImageApi(resolution.getBaseUrl(), resolution.getApiKey(), model, prompt, size);
                }
        );
    }

    private String callImageApi(String endpoint, String apiKey, String model, String prompt, String size) {
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