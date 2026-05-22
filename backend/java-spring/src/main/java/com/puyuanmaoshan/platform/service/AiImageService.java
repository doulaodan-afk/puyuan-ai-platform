package com.puyuanmaoshan.platform.service;

public interface AiImageService {
    String generateImage(String prompt, String size, long tenantId);
    int calculateTokenCost(String size);
}