package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiScriptService;
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
public class AiScriptServiceImpl implements AiScriptService {

    private final SystemConfigService systemConfigService;
    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.base-url:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    @Value("${app.ai.models.default:DeepSeek-V3}")
    private String defaultModel;

    @Value("${app.ai.models.script:z-ai/glm-4.7}")
    private String scriptModel;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiScriptServiceImpl(
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
    public String generateScript(String productDesc, String productUrl, String scriptType, long tenantId) {
        log.info("Generating script for tenant {}, type: {}, desc: {}", tenantId, scriptType, productDesc);

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

        String apiKey = aiApiKey;
        String endpoint = aiBaseUrl;
        String model = scriptModel;

        try {
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("ai_text");
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
            log.warn("Failed to get AI text config from DB, using defaults: {}", e.getMessage());
        }

        String systemPrompt = buildSystemPrompt(scriptType);
        String userPrompt = buildUserPrompt(productDesc, productUrl, scriptType);

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
            log.error("AI script generation failed: {}", e.getMessage(), e);
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