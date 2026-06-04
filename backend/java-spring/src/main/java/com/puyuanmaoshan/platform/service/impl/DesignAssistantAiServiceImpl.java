package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.PluginConfig;
import com.puyuanmaoshan.platform.mapper.PluginConfigMapper;
import com.puyuanmaoshan.platform.service.DesignAssistantAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "true", matchIfMissing = true)
public class DesignAssistantAiServiceImpl implements DesignAssistantAiService {
    private static final Logger logger = LoggerFactory.getLogger(DesignAssistantAiServiceImpl.class);

    private final PluginConfigMapper pluginConfigMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.design-assistant.system-prompt:你是一个专业的服装设计专家，帮助设计师完善需求。}")
    private String defaultSystemPrompt;

    public DesignAssistantAiServiceImpl(PluginConfigMapper pluginConfigMapper, ObjectMapper objectMapper) {
        this.pluginConfigMapper = pluginConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String userMessage, String contextJson, Long tenantId) {
        logger.info("AI chat for tenant {}, message: {}", tenantId, userMessage);

        // Mock implementation
        String mockResponse = switch (getIntent(userMessage)) {
            case "面料" -> "您对面料有什么特殊要求吗？比如材质、克重、颜色等。";
            case "版型" -> "您希望是什么版型？比如修身、宽松、A字型等。";
            case "数量" -> "您计划生产多少件？这将影响面料采购和生产安排。";
            case "时间" -> "您期望什么时候能收到成品？这将影响我们安排生产的优先级。";
            default -> "我理解您的需求。请继续告诉我更多细节，比如面料偏好、版型要求、数量和时间预期等。";
        };

        return mockResponse;
    }

    @Override
    public String summarize(String conversationHistoryJson, Long tenantId) {
        logger.info("AI summarize for tenant {}", tenantId);

        // Mock implementation - return a structured summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("fabric", Map.of(
            "type", "重磅真丝",
            "weight", "20mm以上",
            "color", "莫兰迪色系",
            "special_requirements", "防缩水处理"
        ));
        summary.put("pattern", Map.of(
            "collar", "法式小尖领",
            "sleeve", "七分袖",
            "waist", "腰部收褶设计",
            "silhouette", "H型",
            "other_details", "无袖设计，侧开叉"
        ));
        summary.put("quantity", 100);
        summary.put("deadline", "2024-06-30");

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            logger.error("Failed to serialize summary", e);
            return "{}";
        }
    }

    @Override
    public List<Map<String, Object>> splitTasks(String aiSummary, Long tenantId) {
        try {
            Map<String, Object> summary = objectMapper.readValue(aiSummary, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> tasks = new ArrayList<>();

            // 面料任务
            @SuppressWarnings("unchecked")
            Map<String, Object> fabricData = (Map<String, Object>) summary.get("fabric");
            if (fabricData != null) {
                Map<String, Object> fabricTask = new HashMap<>();
                fabricTask.put("taskType", "fabric");
                fabricTask.put("content", fabricData);
                fabricTask.put("keyword", (String) fabricData.getOrDefault("type", "面料"));
                tasks.add(fabricTask);
            }

            // 打版任务
            @SuppressWarnings("unchecked")
            Map<String, Object> patternData = (Map<String, Object>) summary.get("pattern");
            if (patternData != null) {
                Map<String, Object> patternTask = new HashMap<>();
                patternTask.put("taskType", "pattern");
                patternTask.put("content", patternData);
                patternTask.put("keyword", "打版");
                tasks.add(patternTask);
            }

            return tasks;

        } catch (Exception e) {
            logger.error("Failed to split tasks", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getSystemPrompt(String pluginCode) {
        try {
            PluginConfig config = pluginConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PluginConfig>()
                    .eq(PluginConfig::getPluginCode, pluginCode)
                    .eq(PluginConfig::getConfigKey, "system_prompt")
            );

            if (config != null && config.getConfigValue() != null) {
                return config.getConfigValue();
            }
        } catch (Exception e) {
            logger.error("Failed to get system prompt", e);
        }

        return defaultSystemPrompt;
    }

    private String getIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("面料") || lower.contains("材质") || lower.contains("真丝") || lower.contains("羊毛")) {
            return "面料";
        }
        if (lower.contains("版型") || lower.contains("修身") || lower.contains("宽松") || lower.contains("领子")) {
            return "版型";
        }
        if (lower.contains("数量") || lower.contains("件") || lower.contains("生产")) {
            return "数量";
        }
        if (lower.contains("时间") || lower.contains("什么时候") || lower.contains("截止") || lower.contains("交货")) {
            return "时间";
        }
        return "其他";
    }
}