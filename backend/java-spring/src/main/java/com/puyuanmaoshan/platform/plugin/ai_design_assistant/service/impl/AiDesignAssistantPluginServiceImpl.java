package com.puyuanmaoshan.platform.plugin.ai_design_assistant.service.impl;

import com.puyuanmaoshan.platform.plugin.ai_design_assistant.dto.AiDesignAssistantDtos.*;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Fabric;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Message;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Requirement;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity.Task;
import com.puyuanmaoshan.platform.plugin.ai_design_assistant.service.AiDesignAssistantPluginService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AiDesignAssistantPluginServiceImpl implements AiDesignAssistantPluginService {

    private final Map<Long, Requirement> requirements = new ConcurrentHashMap<>();
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final Map<Long, Fabric> fabrics = new ConcurrentHashMap<>();
    private final Map<Long, Message> messages = new ConcurrentHashMap<>();
    private final AtomicLong requirementIdSeq = new AtomicLong(1);
    private final AtomicLong taskIdSeq = new AtomicLong(1);
    private final AtomicLong fabricIdSeq = new AtomicLong(1);
    private final AtomicLong messageIdSeq = new AtomicLong(1);

    public AiDesignAssistantPluginServiceImpl() {
        initMockData();
    }

    private void initMockData() {
        // Mock Requirements
        for (long i = 1; i <= 5; i++) {
            Requirement r = new Requirement();
            r.setId(i);
            r.setTenantId(1L);
            r.setCreatorId(1L);
            r.setTitle("设计需求 " + i);
            r.setRawImages(Arrays.asList("https://picsum.photos/400/300?random=" + i));
            r.setRawVideos(new ArrayList<>());
            r.setRawAudioUrl(null);
            r.setRawText("这是需求 " + i + "的描述文本");
            r.setConversationHistory(new ArrayList<>());
            r.setAiSummary("AI总结：需求" + i + "需要设计一款时尚毛衣，采用高品质羊毛材质。");
            r.setDesignerApproved(1);
            r.setAssistantId(2L);
            r.setStatus(i % 2 == 0 ? "completed" : "released");
            r.setTotalTokenCost(1000 + (int) (i * 100));
            r.setCreatedAt(LocalDateTime.now().minusDays(i));
            r.setUpdatedAt(LocalDateTime.now().minusDays(i));
            r.setTasks(new ArrayList<>());
            requirements.put(i, r);
        }
        requirementIdSeq.set(6);

        // Mock Tasks
        long taskId = 1;
        for (long reqId = 1; reqId <= 5; reqId++) {
            for (int j = 0; j < 2; j++) {
                Task t = new Task();
                t.setId(taskId);
                t.setRequirementId(reqId);
                t.setTaskType(j == 0 ? "fabric" : "pattern");
                t.setAssigneeType(j == 0 ? "supplier" : "pattern_service");
                t.setAssigneeId(j == 0 ? 10L : 11L);
                t.setAssigneeName(j == 0 ? "面料供应商A" : "花型服务商B");
                Map<String, Object> content = new HashMap<>();
                content.put("description", "任务内容描述 " + taskId);
                content.put("requirements", "具体要求说明");
                t.setContent(content);
                t.setStatus(taskId % 3 == 0 ? "done" : taskId % 3 == 1 ? "pending" : "accepted");
                t.setDeadline(LocalDateTime.now().plusDays(7));
                t.setResultUrl(taskId % 3 == 0 ? "https://example.com/result/" + taskId : null);
                t.setFabricTaskId(j == 0 ? null : 1L);
                t.setLogisticsStatus(taskId % 3 == 0 ? "delivered" : null);
                t.setRequirementTitle("设计需求 " + reqId);
                t.setCanAccept(true);
                t.setCreatedAt(LocalDateTime.now().minusDays(taskId));
                tasks.put(taskId, t);
                taskId++;
            }
        }
        taskIdSeq.set(taskId);

        // Mock Fabrics
        for (long i = 1; i <= 8; i++) {
            Fabric f = new Fabric();
            f.setId(i);
            f.setSupplierTenantId(10L + (i % 3));
            f.setName("面料 " + i);
            f.setCategory(i % 2 == 0 ? "羊毛" : "棉麻");
            f.setImages(new String[]{"https://picsum.photos/400/300?fabric=" + i});
            f.setVideoUrl(i % 2 == 0 ? "https://example.com/video/" + i : null);
            Map<String, Object> specs = new HashMap<>();
            specs.put("weight", "200g/m²");
            specs.put("width", "150cm");
            specs.put("color", "多色可选");
            f.setSpecs(specs);
            f.setPricePerMeter(50.0 + i * 10);
            f.setStockStatus(i % 3 != 0 ? "in_stock" : "out_of_stock");
            f.setIsVisible(1);
            f.setCreatedAt(LocalDateTime.now().minusDays(i));
            f.setUpdatedAt(LocalDateTime.now().minusDays(i));
            fabrics.put(i, f);
        }
        fabricIdSeq.set(9);

        // Mock Messages
        for (long i = 1; i <= 6; i++) {
            Message m = new Message();
            m.setId(i);
            m.setReceiverId(1L);
            m.setSenderId(2L);
            m.setSenderName("AI设计师");
            m.setTitle("任务通知 " + i);
            m.setContent("您的任务 " + i + "有新更新，请及时查看。");
            m.setType(i % 2 == 0 ? "task" : "system");
            m.setIsRead(i > 3);
            m.setRelatedId(i);
            m.setCreatedAt(LocalDateTime.now().minusHours(i));
            messages.put(i, m);
        }
        messageIdSeq.set(7);
    }

    // ========== Requirement APIs ==========

    @Override
    public List<Requirement> listRequirements(Long tenantId, String status, Integer page, Integer size) {
        return requirements.values().stream()
                .filter(r -> tenantId == null || r.getTenantId().equals(tenantId))
                .filter(r -> status == null || r.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public Requirement getRequirement(Long id) {
        return requirements.get(id);
    }

    @Override
    public Requirement createRequirement(RequirementCreateRequest request, Long tenantId, Long creatorId) {
        Requirement r = new Requirement();
        r.setId(requirementIdSeq.getAndIncrement());
        r.setTenantId(tenantId);
        r.setCreatorId(creatorId);
        r.setTitle(request.getTitle());
        r.setRawImages(request.getRawImages());
        r.setRawVideos(request.getRawVideos());
        r.setRawAudioUrl(request.getRawAudioUrl());
        r.setRawText(request.getRawText());
        r.setConversationHistory(new ArrayList<>());
        r.setAiSummary(null);
        r.setDesignerApproved(0);
        r.setAssistantId(null);
        r.setStatus("draft");
        r.setTotalTokenCost(0);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setTasks(new ArrayList<>());
        requirements.put(r.getId(), r);
        return r;
    }

    @Override
    public Requirement updateRequirement(Long id, RequirementUpdateRequest request) {
        Requirement r = requirements.get(id);
        if (r == null) return null;
        if (request.getTitle() != null) r.setTitle(request.getTitle());
        if (request.getDesignerApproved() != null) r.setDesignerApproved(request.getDesignerApproved());
        if (request.getStatus() != null) r.setStatus(request.getStatus());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    @Override
    public void deleteRequirement(Long id) {
        requirements.remove(id);
    }

    @Override
    public String getAiSummary(Long requirementId) {
        Requirement r = requirements.get(requirementId);
        return r != null ? r.getAiSummary() : null;
    }

    // ========== Task APIs ==========

    @Override
    public List<Task> listTasks(Long requirementId, String taskType, String status, Integer page, Integer size) {
        return tasks.values().stream()
                .filter(t -> requirementId == null || t.getRequirementId().equals(requirementId))
                .filter(t -> taskType == null || t.getTaskType().equals(taskType))
                .filter(t -> status == null || t.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public Task getTask(Long id) {
        return tasks.get(id);
    }

    @Override
    public Task createTask(TaskCreateRequest request) {
        Task t = new Task();
        t.setId(taskIdSeq.getAndIncrement());
        t.setRequirementId(request.getRequirementId());
        t.setTaskType(request.getTaskType());
        t.setAssigneeType(request.getAssigneeType());
        t.setAssigneeId(request.getAssigneeId());
        t.setAssigneeName(request.getAssigneeType() + "-" + request.getAssigneeId());
        t.setContent(request.getContent());
        t.setStatus("pending");
        t.setDeadline(request.getDeadline());
        Requirement r = requirements.get(request.getRequirementId());
        t.setRequirementTitle(r != null ? r.getTitle() : null);
        t.setCanAccept(true);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        tasks.put(t.getId(), t);
        return t;
    }

    @Override
    public Task updateTask(Long id, TaskUpdateRequest request) {
        Task t = tasks.get(id);
        if (t == null) return null;
        if (request.getContent() != null) t.setContent(request.getContent());
        if (request.getStatus() != null) t.setStatus(request.getStatus());
        if (request.getDeadline() != null) t.setDeadline(request.getDeadline());
        if (request.getResultUrl() != null) t.setResultUrl(request.getResultUrl());
        if (request.getLogisticsCompany() != null) t.setLogisticsCompany(request.getLogisticsCompany());
        if (request.getLogisticsTrackingNo() != null) t.setLogisticsTrackingNo(request.getLogisticsTrackingNo());
        if (request.getLogisticsStatus() != null) t.setLogisticsStatus(request.getLogisticsStatus());
        if (request.getOfflineLogisticsNote() != null) t.setOfflineLogisticsNote(request.getOfflineLogisticsNote());
        if (request.getRejectReason() != null) t.setRejectReason(request.getRejectReason());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Override
    public void deleteTask(Long id) {
        tasks.remove(id);
    }

    @Override
    public Task acceptTask(Long id) {
        Task t = tasks.get(id);
        if (t != null) {
            t.setStatus("accepted");
            t.setUpdatedAt(LocalDateTime.now());
        }
        return t;
    }

    @Override
    public Task rejectTask(Long id, String reason) {
        Task t = tasks.get(id);
        if (t != null) {
            t.setStatus("rejected");
            t.setRejectReason(reason);
            t.setUpdatedAt(LocalDateTime.now());
        }
        return t;
    }

    @Override
    public Task shipTask(Long id) {
        Task t = tasks.get(id);
        if (t != null) {
            t.setStatus("shipped");
            t.setShippedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
        }
        return t;
    }

    @Override
    public Task deliverTask(Long id) {
        Task t = tasks.get(id);
        if (t != null) {
            t.setStatus("delivered");
            t.setDeliveredAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
        }
        return t;
    }

    // ========== Fabric APIs ==========

    @Override
    public List<Fabric> listFabrics(Long supplierTenantId, String category, String stockStatus, Integer page, Integer size) {
        return fabrics.values().stream()
                .filter(f -> supplierTenantId == null || f.getSupplierTenantId().equals(supplierTenantId))
                .filter(f -> category == null || f.getCategory().equals(category))
                .filter(f -> stockStatus == null || f.getStockStatus().equals(stockStatus))
                .collect(Collectors.toList());
    }

    @Override
    public Fabric getFabric(Long id) {
        return fabrics.get(id);
    }

    @Override
    public Fabric createFabric(FabricCreateRequest request) {
        Fabric f = new Fabric();
        f.setId(fabricIdSeq.getAndIncrement());
        f.setSupplierTenantId(request.getSupplierTenantId());
        f.setName(request.getName());
        f.setCategory(request.getCategory());
        f.setImages(request.getImages());
        f.setVideoUrl(request.getVideoUrl());
        f.setSpecs(request.getSpecs());
        f.setPricePerMeter(request.getPricePerMeter());
        f.setStockStatus(request.getStockStatus() != null ? request.getStockStatus() : "in_stock");
        f.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : 1);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        fabrics.put(f.getId(), f);
        return f;
    }

    @Override
    public Fabric updateFabric(Long id, FabricUpdateRequest request) {
        Fabric f = fabrics.get(id);
        if (f == null) return null;
        if (request.getName() != null) f.setName(request.getName());
        if (request.getCategory() != null) f.setCategory(request.getCategory());
        if (request.getImages() != null) f.setImages(request.getImages());
        if (request.getVideoUrl() != null) f.setVideoUrl(request.getVideoUrl());
        if (request.getSpecs() != null) f.setSpecs(request.getSpecs());
        if (request.getPricePerMeter() != null) f.setPricePerMeter(request.getPricePerMeter());
        if (request.getStockStatus() != null) f.setStockStatus(request.getStockStatus());
        if (request.getIsVisible() != null) f.setIsVisible(request.getIsVisible());
        f.setUpdatedAt(LocalDateTime.now());
        return f;
    }

    @Override
    public void deleteFabric(Long id) {
        fabrics.remove(id);
    }

    // ========== Message APIs ==========

    @Override
    public List<Message> listMessages(Long userId, String type, Integer page, Integer size) {
        return messages.values().stream()
                .filter(m -> m.getReceiverId().equals(userId))
                .filter(m -> type == null || m.getType().equals(type))
                .collect(Collectors.toList());
    }

    @Override
    public Message getMessage(Long id) {
        return messages.get(id);
    }

    @Override
    public Message createMessage(MessageCreateRequest request, Long senderId, String senderName) {
        Message m = new Message();
        m.setId(messageIdSeq.getAndIncrement());
        m.setReceiverId(request.getReceiverId());
        m.setSenderId(senderId);
        m.setSenderName(senderName);
        m.setTitle(request.getTitle());
        m.setContent(request.getContent());
        m.setType(request.getType() != null ? request.getType() : "system");
        m.setIsRead(false);
        m.setRelatedId(request.getRelatedId());
        m.setCreatedAt(LocalDateTime.now());
        messages.put(m.getId(), m);
        return m;
    }

    @Override
    public void markAsRead(Long id) {
        Message m = messages.get(id);
        if (m != null) m.setIsRead(true);
    }

    @Override
    public void markAllAsRead(Long userId) {
        messages.values().stream()
                .filter(m -> m.getReceiverId().equals(userId))
                .forEach(m -> m.setIsRead(true));
    }

    @Override
    public void deleteMessage(Long id) {
        messages.remove(id);
    }

    // ========== Statistics API ==========

    @Override
    public StatisticsResponse getStatistics(Long tenantId) {
        StatisticsResponse resp = new StatisticsResponse();

        Map<String, Object> reqStats = new HashMap<>();
        reqStats.put("total", requirements.size());
        reqStats.put("draft", requirements.values().stream().filter(r -> "draft".equals(r.getStatus())).count());
        reqStats.put("released", requirements.values().stream().filter(r -> "released".equals(r.getStatus())).count());
        reqStats.put("completed", requirements.values().stream().filter(r -> "completed".equals(r.getStatus())).count());
        resp.setRequirements(reqStats);

        Map<String, Object> taskStats = new HashMap<>();
        taskStats.put("total", tasks.size());
        taskStats.put("pending", tasks.values().stream().filter(t -> "pending".equals(t.getStatus())).count());
        taskStats.put("accepted", tasks.values().stream().filter(t -> "accepted".equals(t.getStatus())).count());
        taskStats.put("done", tasks.values().stream().filter(t -> "done".equals(t.getStatus())).count());
        resp.setTasks(taskStats);

        Map<String, Object> fabricStats = new HashMap<>();
        fabricStats.put("total", fabrics.size());
        fabricStats.put("in_stock", fabrics.values().stream().filter(f -> "in_stock".equals(f.getStockStatus())).count());
        fabricStats.put("out_of_stock", fabrics.values().stream().filter(f -> "out_of_stock".equals(f.getStockStatus())).count());
        resp.setFabrics(fabricStats);

        Map<String, Object> msgStats = new HashMap<>();
        msgStats.put("total", messages.size());
        msgStats.put("unread", messages.values().stream().filter(m -> !Boolean.TRUE.equals(m.getIsRead())).count());
        resp.setMessages(msgStats);

        return resp;
    }
}