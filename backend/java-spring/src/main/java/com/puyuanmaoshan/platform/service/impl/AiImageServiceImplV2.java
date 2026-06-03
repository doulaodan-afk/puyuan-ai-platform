package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiImageService;
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
 * AI 图片生成服务（含余额检查和订阅消息）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock.enabled", havingValue = "true")
public class AiImageServiceImplV2 implements AiImageService {

    private final SystemConfigService systemConfigService;
    private final AccountWalletService accountWalletService;
    private final SubscribeMessageService subscribeMessageService;

    @Value("${app.ai.mock-image-url:https://via.placeholder.com/300x200}")
    private String mockImageUrl;

    @Value("${account.balance.low-threshold:100}")
    private long balanceLowThreshold;

    public AiImageServiceImplV2(
            SystemConfigService systemConfigService,
            AccountWalletService accountWalletService,
            SubscribeMessageService subscribeMessageService) {
        this.systemConfigService = systemConfigService;
        this.accountWalletService = accountWalletService;
        this.subscribeMessageService = subscribeMessageService;
    }

    @Override
    public String generateImage(String prompt, String size, long tenantId) {
        log.info("Generating image for tenant {}, size: {}, prompt: {}", tenantId, size, prompt);

        // 检查余额
        AccountWallet wallet = accountWalletService.lambdaQuery()
                .eq(AccountWallet::getTenantId, tenantId)
                .one();

        if (wallet == null || wallet.getTokenBalance() == null) {
            log.warn("租户钱包不存在: {}", tenantId);
            throw new RuntimeException("账户钱包不存在");
        }

        long balance = wallet.getTokenBalance();

        // 计算消耗
        int tokenCost = calculateTokenCost(size);

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
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("ai_image");

            for (Map<String, String> provider : providers) {
                String mockUrl = provider.get("mock_url");
                if (mockUrl != null) {
                    log.info("Using mock image URL: {}", mockUrl);
                    return mockUrl + "&size=" + size;
                }
            }
        } catch (Exception e) {
            log.error("Failed to get AI image config: {}", e.getMessage());
        }

        // 降级到默认配置
        log.warn("Falling back to default config");
        return mockImageUrl + "&size=" + size;
    }

    @Override
    public int calculateTokenCost(String size) {
        // Base cost: 10 tokens per image
        int baseCost = 10;
        return switch (size) {
            case "1024x1024" -> baseCost * 2;
            case "1792x1024", "1024x1792" -> baseCost * 3;
            case "512x512" -> baseCost;
            default -> baseCost;
        };
    }
}
