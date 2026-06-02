package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;

import java.util.List;
import java.util.Map;

public interface DesignAssistantAiService {

    // AI 对话
    String chat(String userMessage, String contextJson, Long tenantId);

    // 生成需求总结
    String summarize(String conversationHistoryJson, Long tenantId);

    // 自动拆分任务（基于 AI 总结）
    List<Map<String, Object>> splitTasks(String aiSummary, Long tenantId);

    // 获取系统提示词
    String getSystemPrompt(String pluginCode);
}