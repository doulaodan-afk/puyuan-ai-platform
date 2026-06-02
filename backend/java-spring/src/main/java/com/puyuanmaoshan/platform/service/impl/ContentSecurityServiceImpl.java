package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.SecurityCheckResult;
import com.puyuanmaoshan.platform.service.ContentSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 内容安全服务实现（基于关键词过滤）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "content-security.enabled", havingValue = "true", matchIfMissing = false)
public class ContentSecurityServiceImpl implements ContentSecurityService {

    // 敏感词库（示例，实际应从数据库或配置文件加载）
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "暴力", "血腥", "恐怖", "色情", "赌博", "毒品"
    );

    @Override
    public SecurityCheckResult checkText(String text, Long tenantId) {
        if (text == null || text.trim().isEmpty()) {
            return new SecurityCheckResult(true, "", "safe");
        }

        String lowerText = text.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerText.contains(sensitiveWord)) {
                log.warn("文本内容包含敏感词: tenantId={}, word={}", tenantId, sensitiveWord);
                return new SecurityCheckResult(false, "内容包含敏感词", "high");
            }
        }

        return new SecurityCheckResult(true, "", "safe");
    }

    @Override
    public SecurityCheckResult checkImage(String imageUrl, Long tenantId) {
        // Mock 实现：检查图片URL是否包含敏感词
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return new SecurityCheckResult(true, "", "safe");
        }

        String lowerUrl = imageUrl.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerUrl.contains(sensitiveWord)) {
                log.warn("图片URL包含敏感词: tenantId={}, url={}, word={}", tenantId, imageUrl, sensitiveWord);
                return new SecurityCheckResult(false, "图片URL包含敏感词", "high");
            }
        }

        // TODO: 实际项目中应调用阿里云内容安全API检查图片内容
        return new SecurityCheckResult(true, "", "safe");
    }

    @Override
    public void recordViolation(Long tenantId, String contentType, String content, String riskLevel, String reason) {
        // TODO: 记录到数据库
        // 创建 content_violation 表
        // 记录：tenantId, contentType, content, riskLevel, reason, createdAt

        log.info("记录内容违规: tenantId={}, contentType={}, riskLevel={}, reason={}",
                tenantId, contentType, riskLevel, reason);
    }
}
