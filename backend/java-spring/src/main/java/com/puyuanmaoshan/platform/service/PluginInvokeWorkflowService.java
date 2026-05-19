package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.ApiModels;

import java.util.Map;

public interface PluginInvokeWorkflowService {
    ApiModels.PluginInvokeResponse invoke(long tenantId,
                                          String pluginId,
                                          Map<String, Object> payload,
                                          String idempotencyKey,
                                          String requestId);
}
