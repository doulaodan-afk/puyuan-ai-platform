package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiScriptService;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 脚本生成服务（含余额检查和订阅消息）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "true", matchIfMissing = true)
public class AiScriptServiceImplV2 implements AiScriptService {

    private final SystemConfigService systemConfigService;
    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;

    @Value("${app.ai.mock-script-response:这是一个示例脚本}")
    private String mockScriptResponse;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiScriptServiceImplV2(
            SystemConfigService systemConfigService,
            AccountWalletService accountWalletService,
            SubscribeMessageService subscribeMessageService) {
        this.systemConfigService = systemConfigService;
        this.accountWalletService = accountWalletService;
        this.subscribeMessageService = subscribeMessageService;
    }

    @Override
    public String generateScript(String productDesc, String productUrl, String scriptType, long tenantId, String modelOverride) {
        log.info("Generating script for tenant {}, type: {}, desc: {}", tenantId, scriptType, productDesc);

        // 检查余额
        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();

        if (wallet == null || wallet.getTokenBalance() == null) {
            log.warn("租户钱包不存在: {}", tenantId);
            throw new RuntimeException("账户钱包不存在");
        }

        long balance = wallet.getTokenBalance();
        int tokenCost = calculateTokenCost();

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

        // 尝试从数据库获取配置
        try {
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("ai_text");

            for (Map<String, String> provider : providers) {
                try {
                    String apiKey = provider.get("api_key");
                    String endpoint = provider.get("endpoint");
                    String modelName = provider.get("model_name");

                    log.info("Using AI text provider: {}, model: {}, endpoint: {}",
                            provider.get("provider_name"), modelName, endpoint);

                    // TODO: 在真实模式下，这里调用 OpenAI API
                    // String response = callOpenAiChatApi(apiKey, endpoint, modelName, buildPrompt(productDesc, productUrl, scriptType));

                    // Mock 模式：返回预定义脚本模板
                    return mockScriptResponse;
                } catch (Exception e) {
                    log.warn("AI text provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败，使用配置文件的默认配置
            log.warn("All AI text providers failed, falling back to default config");
            return mockScriptResponse;

        } catch (Exception e) {
            log.error("Failed to get AI text config: {}", e.getMessage(), e);
            // 降级到 Mock 模式
            return mockScriptResponse;
        }
    }

    @Override
    public int calculateTokenCost() {
        return 20; // Fixed cost for script generation
    }
}
