package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiInvokeTemplate;
import com.puyuanmaoshan.platform.service.AiScriptService;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
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
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "false")
public class AiScriptServiceImpl implements AiScriptService {

    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;
    private final AiInvokeTemplate aiInvokeTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiScriptServiceImpl(
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
    public String generateScript(String productDesc, String productUrl, String scriptType, long tenantId, String modelOverride) {
        log.info("Generating script for tenant {}, type: {}, desc: {}, modelOverride: {}", tenantId, scriptType, productDesc, modelOverride);

        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();

        if (wallet == null || wallet.getTokenBalance() == null) {
            throw new RuntimeException("账户钱包不存在");
        }

        long balance = wallet.getTokenBalance();
        int tokenCost = calculateTokenCost();

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

        String systemPrompt = buildSystemPrompt(scriptType);
        String userPrompt = buildUserPrompt(productDesc, productUrl, scriptType);
        String effectiveModel = modelOverride;

        // 使用 AiInvokeTemplate 统一处理多 Key 轮询 + 故障转移
        return aiInvokeTemplate.invokeWithRetry(
                AiInvokeTemplate.CallContext.builder()
                        .sceneCode("summarize")
                        .tenantId(tenantId)
                        .modelOverride(modelOverride)
                        .maxRetries(3)
                        .build(),
                resolution -> {
                    String model = effectiveModel != null && !effectiveModel.isEmpty()
                            ? effectiveModel : resolution.getModelId();
                    log.info("Calling script generation: model={} @ {} (provider: {}, keyIndex: {})",
                            model, resolution.getBaseUrl(), resolution.getProviderName(), resolution.getKeyIndex());
                    return callChatCompletion(resolution.getBaseUrl(), resolution.getApiKey(),
                            model, systemPrompt, userPrompt);
                }
        );
    }

    private String callChatCompletion(String endpoint, String apiKey, String model, String systemPrompt, String userPrompt) {
        try {
            String url = endpoint + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 2000
            ));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode json = objectMapper.readTree(response);
            JsonNode choices = json.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                if (!content.isEmpty()) {
                    log.info("Script generated successfully, length: {}", content.length());
                    return content;
                }
            }

            log.error("Unexpected AI API response: {}", response);
            throw new RuntimeException("AI脚本生成失败：响应格式异常");
        } catch (Exception e) {
            throw new RuntimeException("AI脚本生成失败：" + e.getMessage());
        }
    }

    private String buildSystemPrompt(String scriptType) {
        return "你是一位专业的短视频脚本编剧。请根据用户提供的商品信息，创作一个吸引人的短视频脚本。" +
                "脚本应包含：开场吸引、产品展示、卖点提炼、结尾引导。" +
                "请用中文输出，格式清晰，包含场景描述和台词。";
    }

    private String buildUserPrompt(String productDesc, String productUrl, String scriptType) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下商品创作一个").append(scriptType != null ? scriptType : "短视频").append("脚本：\n");
        if (productDesc != null && !productDesc.isEmpty()) {
            sb.append("商品描述：").append(productDesc).append("\n");
        }
        if (productUrl != null && !productUrl.isEmpty()) {
            sb.append("商品链接：").append(productUrl).append("\n");
        }
        return sb.toString();
    }

    @Override
    public int calculateTokenCost() {
        return 20;
    }
}