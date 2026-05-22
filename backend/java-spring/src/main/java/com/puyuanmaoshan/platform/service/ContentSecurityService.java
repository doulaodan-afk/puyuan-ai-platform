package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.SecurityCheckResult;

/**
 * 内容安全服务
 */
public interface ContentSecurityService {

    /**
     * 检查文本内容安全性
     * @param text 待检查文本
     * @param tenantId 租户ID
     * @return 检查结果
     */
    SecurityCheckResult checkText(String text, Long tenantId);

    /**
     * 检查图片内容安全性
     * @param imageUrl 图片URL
     * @param tenantId 租户ID
     * @return 检查结果
     */
    SecurityCheckResult checkImage(String imageUrl, Long tenantId);

    /**
     * 记录内容违规
     * @param tenantId 租户ID
     * @param contentType 内容类型：text, image
     * @param content 内容
     * @param riskLevel 风险等级
     * @param reason 违规原因
     */
    void recordViolation(Long tenantId, String contentType, String content, String riskLevel, String reason);
}
