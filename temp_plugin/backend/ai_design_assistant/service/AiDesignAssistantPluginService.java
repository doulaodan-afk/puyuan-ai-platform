package com.puyuanmaoshan.platform.plugin.ai_design_assistant.service;

import com.puyuanmaoshan.platform.plugin.ai_design_assistant.dto.AiDesignAssistantDtos.*;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Fabric;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Message;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Requirement;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Task;

import java.util.List;

public interface AiDesignAssistantPluginService {

    // Requirement APIs
    List<Requirement> listRequirements(Long tenantId, String status, Integer page, Integer size);
    Requirement getRequirement(Long id);
    Requirement createRequirement(RequirementCreateRequest request, Long tenantId, Long creatorId);
    Requirement updateRequirement(Long id, RequirementUpdateRequest request);
    void deleteRequirement(Long id);
    String getAiSummary(Long requirementId);

    // Task APIs
    List<Task> listTasks(Long requirementId, String taskType, String status, Integer page, Integer size);
    Task getTask(Long id);
    Task createTask(TaskCreateRequest request);
    Task updateTask(Long id, TaskUpdateRequest request);
    void deleteTask(Long id);
    Task acceptTask(Long id);
    Task rejectTask(Long id, String reason);
    Task shipTask(Long id);
    Task deliverTask(Long id);

    // Fabric APIs
    List<Fabric> listFabrics(Long supplierTenantId, String category, String stockStatus, Integer page, Integer size);
    Fabric getFabric(Long id);
    Fabric createFabric(FabricCreateRequest request);
    Fabric updateFabric(Long id, FabricUpdateRequest request);
    void deleteFabric(Long id);

    // Message APIs
    List<Message> listMessages(Long userId, String type, Integer page, Integer size);
    Message getMessage(Long id);
    Message createMessage(MessageCreateRequest request, Long senderId, String senderName);
    void markAsRead(Long id);
    void markAllAsRead(Long userId);
    void deleteMessage(Long id);

    // Statistics API
    StatisticsResponse getStatistics(Long tenantId);
}