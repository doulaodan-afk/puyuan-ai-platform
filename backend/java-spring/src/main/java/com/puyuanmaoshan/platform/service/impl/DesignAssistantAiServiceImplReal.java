package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.entity.PluginConfig;
import com.puyuanmaoshan.platform.mapper.PluginConfigMapper;
import com.puyuanmaoshan.platform.service.DesignAssistantAiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "false")
public class DesignAssistantAiServiceImplReal implements DesignAssistantAiService {

    private final PluginConfigMapper pluginConfigMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.ai.base-url:https://api.openai.com/v1}")
    private String aiBaseUrl;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    @Value("${app.ai.models.default:DeepSeek-V3}")
    private String defaultModel;

    @Value("${app.ai.design-assistant.system-prompt:你是一个专业的服装设计专家，帮助设计师完善需求。}")
    private String defaultSystemPrompt;

    public DesignAssistantAiServiceImplReal(PluginConfigMapper pluginConfigMapper, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.pluginConfigMapper = pluginConfigMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public String chat(String userMessage, String contextJson, Long tenantId) {
        String systemPrompt = getSystemPrompt("design_assistant");
        String userContent = userMessage;
        if (contextJson != null && !contextJson.isEmpty()) {
            userContent = "上下文信息：\n" + contextJson + "\n\n用户问题：" + userMessage;
        }

        return callAiApi(systemPrompt, userContent, tenantId);
    }

    @Override
    public String summarize(String conversationHistoryJson, Long tenantId) {
        String systemPrompt = "请根据以下对话历史，生成一份简洁的需求总结，包含关键设计要点和约束条件。";
        return callAiApi(systemPrompt, conversationHistoryJson, tenantId);
    }

    @Override
    public List<Map<String, Object>> splitTasks(String aiSummary, Long tenantId) {
        String systemPrompt = "请根据以下AI需求总结，自动拆分为具体的执行任务列表。每个任务包含：task_name（任务名）、description（描述）、priority（优先级：high/medium/low）、estimated_hours（预估工时）。请以JSON数组格式输出。";
        String response = callAiApi(systemPrompt, aiSummary, tenantId);

        try {
            String jsonContent = extractJsonFromResponse(response);
            return objectMapper.readValue(jsonContent, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse AI task split response, creating single task: {}", e.getMessage());
            List<Map<String, Object>> tasks = new ArrayList<>();
            Map<String, Object> task = new HashMap<>();
            task.put("task_name", "设计任务");
            task.put("description", response);
            task.put("priority", "medium");
            task.put("estimated_hours", 2);
            tasks.add(task);
            return tasks;
        }
    }

    @Override
    public String getSystemPrompt(String pluginCode) {
        try {
            PluginConfig config = pluginConfigMapper.selectOne(
                    new LambdaQueryWrapper<PluginConfig>()
                            .eq(PluginConfig::getPluginCode, pluginCode)
                            .eq(PluginConfig::getConfigKey, "system_prompt")
            );
            if (config != null && config.getConfigValue() != null) {
                return config.getConfigValue();
            }
        } catch (Exception e) {
            log.warn("Failed to get system prompt from DB config: {}", e.getMessage());
        }
        return defaultSystemPrompt;
    }

    private String callAiApi(String systemPrompt, String userContent, Long tenantId) {
        String apiKey = aiApiKey;
        String endpoint = aiBaseUrl;
        String model = defaultModel;

        try {
            String url = endpoint + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 2000
            ));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode json = objectMapper.readTree(response);
            JsonNode choices = json.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                if (!content.isEmpty()) {
                    return content;
                }
            }

            log.error("Unexpected AI API response: {}", response);
            throw new RuntimeException("AI服务响应格式异常");
        } catch (Exception e) {
            log.error("AI API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败：" + e.getMessage());
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response.contains("[") && response.contains("]")) {
            int start = response.indexOf("[");
            int end = response.lastIndexOf("]") + 1;
            return response.substring(start, end);
        }
        return "[" + response + "]";
    }
}