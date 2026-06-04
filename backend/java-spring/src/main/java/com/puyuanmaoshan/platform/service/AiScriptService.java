package com.puyuanmaoshan.platform.service;

public interface AiScriptService {
    String generateScript(String productDesc, String productUrl, String scriptType, long tenantId, String modelOverride);
    int calculateTokenCost();
}