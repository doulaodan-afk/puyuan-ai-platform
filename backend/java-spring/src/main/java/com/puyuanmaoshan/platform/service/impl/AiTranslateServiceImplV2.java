package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiTranslateService;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 翻译服务（含余额检查和订阅消息）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock.enabled", havingValue = "true")
public class AiTranslateServiceImplV2 implements AiTranslateService {

    private final SystemConfigService systemConfigService;
    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    private static final Map<String, String> LANG_SUFFIX = new HashMap<>();
    static {
        LANG_SUFFIX.put("en", " (English)");
        LANG_SUFFIX.put("th", " (Thai)");
        LANG_SUFFIX.put("vi", " (Vietnamese)");
        LANG_SUFFIX.put("ms", " (Malay)");
        LANG_SUFFIX.put("id", " (Indonesian)");
    }

    public AiTranslateServiceImplV2(
            SystemConfigService systemConfigService,
            AccountWalletService accountWalletService,
            SubscribeMessageService subscribeMessageService) {
        this.systemConfigService = systemConfigService;
        this.accountWalletService = accountWalletService;
        this.subscribeMessageService = subscribeMessageService;
    }

    @Override
    public String translate(String text, String targetLang, long tenantId) {
        log.info("Translating text for tenant {}, targetLang: {}", tenantId, targetLang);

        // 检查余额
        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();

        if (wallet == null || wallet.getTokenBalance() == null) {
            log.warn("租户钱包不存在: {}", tenantId);
            throw new RuntimeException("账户钱包不存在");
        }

        long balance = wallet.getTokenBalance();
        int tokenCost = calculateTokenCost(text.length());

        // 余额不足检查
        if (balance < tokenCost) {
            log.warn("余额不足: balance={}, required={}", balance, tokenCost);

            // 发送余额不足提醒
            Long userId = RequestContextUtil.getUserId();
            if (userId != null) {
                subscribeMessageService.sendBalanceLowNotification(userId, tenantId);
            }

            throw new RuntimeException("余额不足，请先充值");
        }

        // 接近低余额阈值时发送提醒
        if (balance - tokenCost < balanceLowThreshold && balance - tokenCost >= 0) {
            log.info("余额接近阈值: balance={}, remainingAfter={}, threshold={}",
                    balance, balance - tokenCost, balanceLowThreshold);

            Long userId = RequestContextUtil.getUserId();
            if (userId != null) {
                subscribeMessageService.sendBalanceLowNotification(userId, tenantId);
            }
        }

        try {
            // 尝试从数据库获取配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("ai_translate");

            for (Map<String, String> provider : providers) {
                try {
                    String apiKey = provider.get("api_key");
                    String endpoint = provider.get("endpoint");
                    String modelName = provider.get("model_name");

                    log.info("Using AI translate provider: {}, model: {}, endpoint: {}",
                            provider.get("provider_name"), modelName, endpoint);

                    // TODO: 在真实模式下，这里调用 OpenAI API
                    // String response = callOpenAiChatApi(apiKey, endpoint, modelName, buildTranslatePrompt(text, targetLang));

                    // Mock 模式：追加语言后缀
                    String suffix = LANG_SUFFIX.getOrDefault(targetLang.toLowerCase(), " (" + targetLang + ")");
                    return text + suffix;
                } catch (Exception e) {
                    log.warn("AI translate provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败，使用默认 Mock 模式
            log.warn("All AI translate providers failed, falling back to default config");
            String suffix = LANG_SUFFIX.getOrDefault(targetLang.toLowerCase(), " (" + targetLang + ")");
            return text + suffix;

        } catch (Exception e) {
            log.error("Failed to get AI translate config: {}", e.getMessage(), e);
            // 降级到 Mock 模式
            String suffix = LANG_SUFFIX.getOrDefault(targetLang.toLowerCase(), " (" + targetLang + ")");
            return text + suffix;
        }
    }

    @Override
    public int calculateTokenCost(int textLength) {
        return Math.max(5, (textLength + 9) / 10);
    }
}
