package com.puyuanmaoshan.platform.service;

public interface AiTranslateService {
    String translate(String text, String targetLang, long tenantId);
    int calculateTokenCost(int textLength);
}