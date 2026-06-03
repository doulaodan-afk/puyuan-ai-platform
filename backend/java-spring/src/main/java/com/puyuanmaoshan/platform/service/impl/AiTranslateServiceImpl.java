package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiTranslateService;
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
public class AiTranslateServiceImpl implements AiTranslateService {

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

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiTranslateServiceImpl(
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
    public String translate(String text, String targetLang, long tenantId) {
        log.info("Translating text for tenant {}, targetLang: {}", tenantId, targetLang);

        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();

        if (wallet == null || wallet.getTokenBalance() == null) {
            throw new RuntimeException("账户钱包不存在");
        }

        long balance = wallet.getTokenBalance();
        int tokenCost = calculateTokenCost(text.length());

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
        String model = defaultModel;

        try {
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("ai_translate");
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
            log.warn("Failed to get AI translate config from DB, using defaults: {}", e.getMessage());
        }

        String langName = getLangName(targetLang);
        String systemPrompt = "你是一位专业翻译。请将用户提供的文本翻译为" + langName + "。只输出翻译结果，不要添加任何解释或注释。";
        String userPrompt = text;

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
                    "temperature", 0.3,
                    "max_tokens", 2000
            ));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode json = objectMapper.readTree(response);
            JsonNode choices = json.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                if (!content.isEmpty()) {
                    log.info("Translation completed successfully");
                    return content;
                }
            }

            log.error("Unexpected AI API response: {}", response);
            throw new RuntimeException("AI翻译失败：响应格式异常");
        } catch (Exception e) {
            log.error("AI translation failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI翻译失败：" + e.getMessage());
        }
    }

    private String getLangName(String langCode) {
        return switch (langCode.toLowerCase()) {
            case "en" -> "英语";
            case "th" -> "泰语";
            case "vi" -> "越南语";
            case "ms" -> "马来语";
            case "id" -> "印尼语";
            case "ja" -> "日语";
            case "ko" -> "韩语";
            case "fr" -> "法语";
            case "de" -> "德语";
            case "es" -> "西班牙语";
            case "ar" -> "阿拉伯语";
            default -> langCode;
        };
    }

    @Override
    public int calculateTokenCost(int textLength) {
        return Math.max(5, (textLength + 9) / 10);
    }
}