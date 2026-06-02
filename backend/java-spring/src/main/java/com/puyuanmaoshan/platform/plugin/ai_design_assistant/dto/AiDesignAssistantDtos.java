package com.puyuanmaoshan.platform.plugin.ai_design_assistant.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AiDesignAssistantDtos {

    // ========== Requirement DTOs ==========

    public static class RequirementCreateRequest {
        private String title;
        private List<String> rawImages;
        private List<String> rawVideos;
        private String rawAudioUrl;
        private String rawText;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getRawImages() { return rawImages; }
        public void setRawImages(List<String> rawImages) { this.rawImages = rawImages; }
        public List<String> getRawVideos() { return rawVideos; }
        public void setRawVideos(List<String> rawVideos) { this.rawVideos = rawVideos; }
        public String getRawAudioUrl() { return rawAudioUrl; }
        public void setRawAudioUrl(String rawAudioUrl) { this.rawAudioUrl = rawAudioUrl; }
        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
    }

    public static class RequirementUpdateRequest {
        private String title;
        private Integer designerApproved;
        private String status;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getDesignerApproved() { return designerApproved; }
        public void setDesignerApproved(Integer designerApproved) { this.designerApproved = designerApproved; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // ========== Task DTOs ==========

    public static class TaskCreateRequest {
        private Long requirementId;
        private String taskType;
        private String assigneeType;
        private Long assigneeId;
        private Map<String, Object> content;
        private LocalDateTime deadline;

        public Long getRequirementId() { return requirementId; }
        public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getAssigneeType() { return assigneeType; }
        public void setAssigneeType(String assigneeType) { this.assigneeType = assigneeType; }
        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
        public Map<String, Object> getContent() { return content; }
        public void setContent(Map<String, Object> content) { this.content = content; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    }

    public static class TaskUpdateRequest {
        private Map<String, Object> content;
        private String status;
        private LocalDateTime deadline;
        private String resultUrl;
        private String logisticsCompany;
        private String logisticsTrackingNo;
        private String logisticsStatus;
        private String offlineLogisticsNote;
        private String rejectReason;

        public Map<String, Object> getContent() { return content; }
        public void setContent(Map<String, Object> content) { this.content = content; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
        public String getResultUrl() { return resultUrl; }
        public void setResultUrl(String resultUrl) { this.resultUrl = resultUrl; }
        public String getLogisticsCompany() { return logisticsCompany; }
        public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
        public String getLogisticsTrackingNo() { return logisticsTrackingNo; }
        public void setLogisticsTrackingNo(String logisticsTrackingNo) { this.logisticsTrackingNo = logisticsTrackingNo; }
        public String getLogisticsStatus() { return logisticsStatus; }
        public void setLogisticsStatus(String logisticsStatus) { this.logisticsStatus = logisticsStatus; }
        public String getOfflineLogisticsNote() { return offlineLogisticsNote; }
        public void setOfflineLogisticsNote(String offlineLogisticsNote) { this.offlineLogisticsNote = offlineLogisticsNote; }
        public String getRejectReason() { return rejectReason; }
        public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    }

    // ========== Fabric DTOs ==========

    public static class FabricCreateRequest {
        private Long supplierTenantId;
        private String name;
        private String category;
        private String[] images;
        private String videoUrl;
        private Map<String, Object> specs;
        private Double pricePerMeter;
        private String stockStatus;
        private Integer isVisible;

        public Long getSupplierTenantId() { return supplierTenantId; }
        public void setSupplierTenantId(Long supplierTenantId) { this.supplierTenantId = supplierTenantId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String[] getImages() { return images; }
        public void setImages(String[] images) { this.images = images; }
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        public Map<String, Object> getSpecs() { return specs; }
        public void setSpecs(Map<String, Object> specs) { this.specs = specs; }
        public Double getPricePerMeter() { return pricePerMeter; }
        public void setPricePerMeter(Double pricePerMeter) { this.pricePerMeter = pricePerMeter; }
        public String getStockStatus() { return stockStatus; }
        public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
        public Integer getIsVisible() { return isVisible; }
        public void setIsVisible(Integer isVisible) { this.isVisible = isVisible; }
    }

    public static class FabricUpdateRequest {
        private String name;
        private String category;
        private String[] images;
        private String videoUrl;
        private Map<String, Object> specs;
        private Double pricePerMeter;
        private String stockStatus;
        private Integer isVisible;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String[] getImages() { return images; }
        public void setImages(String[] images) { this.images = images; }
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        public Map<String, Object> getSpecs() { return specs; }
        public void setSpecs(Map<String, Object> specs) { this.specs = specs; }
        public Double getPricePerMeter() { return pricePerMeter; }
        public void setPricePerMeter(Double pricePerMeter) { this.pricePerMeter = pricePerMeter; }
        public String getStockStatus() { return stockStatus; }
        public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
        public Integer getIsVisible() { return isVisible; }
        public void setIsVisible(Integer isVisible) { this.isVisible = isVisible; }
    }

    // ========== Message DTOs ==========

    public static class MessageCreateRequest {
        private Long receiverId;
        private String title;
        private String content;
        private String type;
        private Long relatedId;

        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getRelatedId() { return relatedId; }
        public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
    }

    // ========== Statistics DTOs ==========

    public static class StatisticsResponse {
        private Map<String, Object> requirements;
        private Map<String, Object> tasks;
        private Map<String, Object> fabrics;
        private Map<String, Object> messages;

        public Map<String, Object> getRequirements() { return requirements; }
        public void setRequirements(Map<String, Object> requirements) { this.requirements = requirements; }
        public Map<String, Object> getTasks() { return tasks; }
        public void setTasks(Map<String, Object> tasks) { this.tasks = tasks; }
        public Map<String, Object> getFabrics() { return fabrics; }
        public void setFabrics(Map<String, Object> fabrics) { this.fabrics = fabrics; }
        public Map<String, Object> getMessages() { return messages; }
        public void setMessages(Map<String, Object> messages) { this.messages = messages; }
    }
}