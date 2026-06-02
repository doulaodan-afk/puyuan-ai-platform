package com.puyuanmaoshan.platform.plugin.ai_design_assistant.controller;

import com.puyuanmaoshan.platform.plugin.ai_design_assistant.dto.AiDesignAssistantDtos.*;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Fabric;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Message;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Requirement;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Task;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.service.AiDesignAssistantPluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plugins/ai-design-assistant")
public class AiDesignAssistantPluginController {

    @Autowired
    private AiDesignAssistantPluginService service;

    // ========== Requirement APIs ==========

    @GetMapping("/requirements")
    public List<Requirement> listRequirements(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return service.listRequirements(tenantId, status, page, size);
    }

    @GetMapping("/requirements/{id}")
    public Requirement getRequirement(@PathVariable Long id) {
        return service.getRequirement(id);
    }

    @PostMapping("/requirements")
    public Requirement createRequirement(@RequestBody RequirementCreateRequest request,
                                        @RequestParam Long tenantId,
                                        @RequestParam Long creatorId) {
        return service.createRequirement(request, tenantId, creatorId);
    }

    @PutMapping("/requirements/{id}")
    public Requirement updateRequirement(@PathVariable Long id,
                                        @RequestBody RequirementUpdateRequest request) {
        return service.updateRequirement(id, request);
    }

    @DeleteMapping("/requirements/{id}")
    public void deleteRequirement(@PathVariable Long id) {
        service.deleteRequirement(id);
    }

    @GetMapping("/requirements/{id}/ai-summary")
    public String getAiSummary(@PathVariable Long id) {
        return service.getAiSummary(id);
    }

    // ========== Task APIs ==========

    @GetMapping("/tasks")
    public List<Task> listTasks(
            @RequestParam(required = false) Long requirementId,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return service.listTasks(requirementId, taskType, status, page, size);
    }

    @GetMapping("/tasks/{id}")
    public Task getTask(@PathVariable Long id) {
        return service.getTask(id);
    }

    @PostMapping("/tasks")
    public Task createTask(@RequestBody TaskCreateRequest request) {
        return service.createTask(request);
    }

    @PutMapping("/tasks/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody TaskUpdateRequest request) {
        return service.updateTask(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    public void deleteTask(@PathVariable Long id) {
        service.deleteTask(id);
    }

    @PostMapping("/tasks/{id}/accept")
    public Task acceptTask(@PathVariable Long id) {
        return service.acceptTask(id);
    }

    @PostMapping("/tasks/{id}/reject")
    public Task rejectTask(@PathVariable Long id, @RequestParam String reason) {
        return service.rejectTask(id, reason);
    }

    @PostMapping("/tasks/{id}/ship")
    public Task shipTask(@PathVariable Long id) {
        return service.shipTask(id);
    }

    @PostMapping("/tasks/{id}/deliver")
    public Task deliverTask(@PathVariable Long id) {
        return service.deliverTask(id);
    }

    // ========== Fabric APIs ==========

    @GetMapping("/fabrics")
    public List<Fabric> listFabrics(
            @RequestParam(required = false) Long supplierTenantId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return service.listFabrics(supplierTenantId, category, stockStatus, page, size);
    }

    @GetMapping("/fabrics/{id}")
    public Fabric getFabric(@PathVariable Long id) {
        return service.getFabric(id);
    }

    @PostMapping("/fabrics")
    public Fabric createFabric(@RequestBody FabricCreateRequest request) {
        return service.createFabric(request);
    }

    @PutMapping("/fabrics/{id}")
    public Fabric updateFabric(@PathVariable Long id, @RequestBody FabricUpdateRequest request) {
        return service.updateFabric(id, request);
    }

    @DeleteMapping("/fabrics/{id}")
    public void deleteFabric(@PathVariable Long id) {
        service.deleteFabric(id);
    }

    // ========== Message APIs ==========

    @GetMapping("/messages")
    public List<Message> listMessages(
            @RequestParam Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return service.listMessages(userId, type, page, size);
    }

    @GetMapping("/messages/{id}")
    public Message getMessage(@PathVariable Long id) {
        return service.getMessage(id);
    }

    @PostMapping("/messages")
    public Message createMessage(@RequestBody MessageCreateRequest request,
                                @RequestParam Long senderId,
                                @RequestParam String senderName) {
        return service.createMessage(request, senderId, senderName);
    }

    @PostMapping("/messages/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
    }

    @PostMapping("/messages/read-all")
    public void markAllAsRead(@RequestParam Long userId) {
        service.markAllAsRead(userId);
    }

    @DeleteMapping("/messages/{id}")
    public void deleteMessage(@PathVariable Long id) {
        service.deleteMessage(id);
    }

    // ========== Statistics API ==========

    @GetMapping("/statistics")
    public StatisticsResponse getStatistics(@RequestParam Long tenantId) {
        return service.getStatistics(tenantId);
    }
}