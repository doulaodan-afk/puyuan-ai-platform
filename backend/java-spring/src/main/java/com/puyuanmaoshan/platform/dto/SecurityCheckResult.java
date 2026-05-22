package com.puyuanmaoshan.platform.dto;

/**
 * 内容安全检查结果
 */
public record SecurityCheckResult(
        boolean passed,
        String reason,
        String riskLevel
) {}
