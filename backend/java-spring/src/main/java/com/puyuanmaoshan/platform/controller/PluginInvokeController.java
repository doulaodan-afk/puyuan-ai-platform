package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.PluginDtos;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.AiImageService;
import com.puyuanmaoshan.platform.service.AiScriptService;
import com.puyuanmaoshan.platform.service.AiTranslateService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.RateLimiterService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/plugin/invoke")
public class PluginInvokeController {
    private final PluginService pluginService;
    private final TenantPluginService tenantPluginService;
    private final AccountWalletService accountWalletService;
    private final AiImageService aiImageService;
    private final AiScriptService aiScriptService;
    private final AiTranslateService aiTranslateService;
    private final RateLimiterService rateLimiterService;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public PluginInvokeController(PluginService pluginService,
                                    TenantPluginService tenantPluginService,
                                    AccountWalletService accountWalletService,
                                    AiImageService aiImageService,
                                    AiScriptService aiScriptService,
                                    AiTranslateService aiTranslateService,
                                    RateLimiterService rateLimiterService) {
        this.pluginService = pluginService;
        this.tenantPluginService = tenantPluginService;
        this.accountWalletService = accountWalletService;
        this.aiImageService = aiImageService;
        this.aiScriptService = aiScriptService;
        this.aiTranslateService = aiTranslateService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/{pluginCode}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<PluginDtos.PluginInvokeResponse> invoke(
            @PathVariable String pluginCode,
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        // 限流检查
        if (rateLimitEnabled) {
            String rateLimitKey = String.valueOf(parsedTenantId);
            if (!rateLimiterService.isAllowed(rateLimitKey)) {
                throw new AppException(ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
            }
        }

        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginCode)
                .eq(Plugin::getStatus, 1)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "plugin not found");
        }

        TenantPlugin tenantPlugin = tenantPluginService.lambdaQuery()
                .eq(TenantPlugin::getTenantId, parsedTenantId)
                .eq(TenantPlugin::getPluginId, pluginCode)
                .one();
        if (tenantPlugin == null || !Objects.equals(tenantPlugin.getEnabled(), 1)) {
            throw new AppException(ErrorCode.FORBIDDEN, "plugin disabled for tenant");
        }

        Map<String, Object> result;
        int tokenUsed;

        switch (pluginCode) {
            case "ai_image_gen" -> {
                String prompt = (String) payload.get("prompt");
                String imageSize = (String) payload.getOrDefault("image_size", "1024x1024");

                tokenUsed = aiImageService.calculateTokenCost(imageSize);
                long balanceRemaining = accountWalletService.deductToken(parsedTenantId, tokenUsed, pluginCode);

                String imageUrl = aiImageService.generateImage(prompt, imageSize, parsedTenantId);

                result = new HashMap<>();
                result.put("image_url", imageUrl);
                result.put("image_size", imageSize);

                return ApiResponse.ok(new PluginDtos.PluginInvokeResponse(result, tokenUsed, balanceRemaining), requestId);
            }

            case "ai_script_gen" -> {
                String productDesc = (String) payload.get("product_desc");
                String productUrl = (String) payload.get("product_url");
                String scriptType = (String) payload.getOrDefault("script_type", "video");

                tokenUsed = aiScriptService.calculateTokenCost();
                long balanceRemaining = accountWalletService.deductToken(parsedTenantId, tokenUsed, pluginCode);

                String script = aiScriptService.generateScript(productDesc, productUrl, scriptType, parsedTenantId);

                result = new HashMap<>();
                result.put("script", script);
                result.put("script_type", scriptType);

                return ApiResponse.ok(new PluginDtos.PluginInvokeResponse(result, tokenUsed, balanceRemaining), requestId);
            }

            case "ai_translate" -> {
                String text = (String) payload.get("text");
                String targetLang = (String) payload.get("target_lang");

                tokenUsed = aiTranslateService.calculateTokenCost(text.length());
                long balanceRemaining = accountWalletService.deductToken(parsedTenantId, tokenUsed, pluginCode);

                String translatedText = aiTranslateService.translate(text, targetLang, parsedTenantId);

                result = new HashMap<>();
                result.put("translated_text", translatedText);
                result.put("target_lang", targetLang);
                result.put("source_lang", "zh");

                return ApiResponse.ok(new PluginDtos.PluginInvokeResponse(result, tokenUsed, balanceRemaining), requestId);
            }

            default -> throw new AppException(ErrorCode.NOT_FOUND, "unknown plugin code: " + pluginCode);
        }
    }
}