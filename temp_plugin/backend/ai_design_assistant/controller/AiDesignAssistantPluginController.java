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

    /**
     * 从请求头解析身份前缀信息
     * X-Identity-Prefix: 工作室名称-角色（如 "我的工作室-设计师"）
     * X-Identity-Role: 角色代码（如 "designer"）
     * X-Identity-Tenant-Id: 身份对应的租户ID
     */
    private IdentityContext resolveIdentity(
            @RequestHeader(value = "X-Identity-Prefix", required = false) String identityPrefix,
            @RequestHeader(value = "X-Identity-Role", required = false) String identityRole,
            @RequestHeader(value = "X-Identity-Tenant-Id", required = false) String identityTenantId) {
        IdentityContext ctx = new IdentityContext();
        ctx.identityPrefix = identityPrefix;
        ctx.identityRole = identityRole;
        ctx.identityTenantId = identityTenantId;
        return ctx;
    }

    /** 身份上下文内部类 */
    private static class IdentityContext {
        String identityPrefix;
        String identityRole;
        String identityTenantId;

        Long getEffectiveTenantId(Long fallback) {
            if (identityTenantId != null && !identityTenantId.isEmpty()) {
                try {
                    return Long.parseLong(identityTenantId);
                } catch (NumberFormatException e) {
                    // fall through to fallback
                }
            }
            return fallback;
        }
    }

    // ========== Requirement APIs ==========

    @GetMapping("/requirements")
    public List<Requirement> listRequirements(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestHeader(value = "X-Identity-Prefix", required = false) String identityPrefix,
            @RequestHeader(value = "X-Identity-Role", required = false) String identityRole,
            @RequestHeader(value = "X-Identity-Tenant-Id", required = false) String identityTenantId) {
        IdentityContext ctx = resolveIdentity(identityPrefix, identityRole, identityTenantId);
        // 优先使用身份选择的 tenantId
        Long effectiveTenantId = ctx.getEffectiveTenantId(tenantId);
        return service.listRequirements(effectiveTenantId, status, page, size);
    }

    @GetMapping("/requirements/{id}")
    public Requirement getRequirement(@PathVariable Long id) {
        return service.getRequirement(id);
    }

    @PostMapping("/requirements")
    public Requirement createRequirement(@RequestBody RequirementCreateRequest request,
                                        @RequestParam(required = false) Long tenantId,
                                        @RequestParam(required = false) Long creatorId,
                                        @RequestHeader(value = "X-Identity-Prefix", required = false) String identityPrefix,
                                        @RequestHeader(value = "X-Identity-Role", required = false) String identityRole,
                                        @RequestHeader(value = "X-Identity-Tenant-Id", required = false) String identityTenantId) {
        IdentityContext ctx = resolveIdentity(identityPrefix, identityRole, identityTenantId);
        Long effectiveTenantId = ctx.getEffectiveTenantId(tenantId);
        return service.createRequirement(request, effectiveTenantId, creatorId);
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
    public Fabric createFabric(@RequestBody FabricCreateRequest request,
                               @RequestHeader(value = "X-Identity-Tenant-Id", required = false) String identityTenantId) {
        // 面料创建时使用身份租户ID作为 supplierTenantId
        if (identityTenantId != null && !identityTenantId.isEmpty() && request.getSupplierTenantId() == null) {
            try {
                request.setSupplierTenantId(Long.parseLong(identityTenantId));
            } catch (NumberFormatException ignored) {}
        }
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
                                @RequestParam String senderName,
                                @RequestHeader(value = "X-Identity-Prefix", required = false) String identityPrefix) {
        // 发送消息时，如果提供了身份前缀，附加到发送者名称上
        String effectiveSenderName = identityPrefix != null && !identityPrefix.isEmpty()
                ? "[" + identityPrefix + "] " + senderName
                : senderName;
        return service.createMessage(request, senderId, effectiveSenderName);
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
    public StatisticsResponse getStatistics(
            @RequestParam(required = false) Long tenantId,
            @RequestHeader(value = "X-Identity-Tenant-Id", required = false) String identityTenantId) {
        // 优先使用身份租户ID
        Long effectiveTenantId = tenantId;
        if (identityTenantId != null && !identityTenantId.isEmpty()) {
            try {
                effectiveTenantId = Long.parseLong(identityTenantId);
            } catch (NumberFormatException ignored) {}
        }
        return service.getStatistics(effectiveTenantId);
    }
}
