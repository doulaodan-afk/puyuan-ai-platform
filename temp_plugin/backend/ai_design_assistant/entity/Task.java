package com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity;

import java.time.LocalDateTime;
import java.util.Map;

public class Task {
    private Long id;
    private Long requirementId;
    private String taskType; // fabric, pattern
    private String assigneeType; // supplier, pattern_service, internal
    private Long assigneeId;
    private String assigneeName;
    private Map<String, Object> content;
    private String status; // draft, pending, accepted, shipped, delivered, rejected, done, cancelled
    private LocalDateTime deadline;
    private String resultUrl;
    private Long fabricTaskId;
    private String logisticsCompany;
    private String logisticsTrackingNo;
    private String logisticsStatus; // pending, shipped, delivered
    private String offlineLogisticsNote;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String rejectReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String requirementTitle;
    private Boolean canAccept;
    private String cannotAcceptReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getAssigneeType() { return assigneeType; }
    public void setAssigneeType(String assigneeType) { this.assigneeType = assigneeType; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getResultUrl() { return resultUrl; }
    public void setResultUrl(String resultUrl) { this.resultUrl = resultUrl; }
    public Long getFabricTaskId() { return fabricTaskId; }
    public void setFabricTaskId(Long fabricTaskId) { this.fabricTaskId = fabricTaskId; }
    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
    public String getLogisticsTrackingNo() { return logisticsTrackingNo; }
    public void setLogisticsTrackingNo(String logisticsTrackingNo) { this.logisticsTrackingNo = logisticsTrackingNo; }
    public String getLogisticsStatus() { return logisticsStatus; }
    public void setLogisticsStatus(String logisticsStatus) { this.logisticsStatus = logisticsStatus; }
    public String getOfflineLogisticsNote() { return offlineLogisticsNote; }
    public void setOfflineLogisticsNote(String offlineLogisticsNote) { this.offlineLogisticsNote = offlineLogisticsNote; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getRequirementTitle() { return requirementTitle; }
    public void setRequirementTitle(String requirementTitle) { this.requirementTitle = requirementTitle; }
    public Boolean getCanAccept() { return canAccept; }
    public void setCanAccept(Boolean canAccept) { this.canAccept = canAccept; }
    public String getCannotAcceptReason() { return cannotAcceptReason; }
    public void setCannotAcceptReason(String cannotAcceptReason) { this.cannotAcceptReason = cannotAcceptReason; }
}