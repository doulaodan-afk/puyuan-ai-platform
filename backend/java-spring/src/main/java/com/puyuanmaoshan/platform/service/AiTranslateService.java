package com.puyuanmaoshan.platform.service;

public interface AiTranslateService {
    String translate(String text, String targetLang, long tenantId, String modelOverride);
    int calculateTokenCost(int textLength);
}