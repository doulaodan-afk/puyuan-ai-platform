package com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity;

import java.time.LocalDateTime;
import java.util.List;

public class Requirement {
    private Long id;
    private Long tenantId;
    private Long creatorId;
    private String title;
    private List<String> rawImages;
    private List<String> rawVideos;
    private String rawAudioUrl;
    private String rawText;
    private List<ChatMessage> conversationHistory;
    private String aiSummary;
    private Integer designerApproved; // 0-未确认, 1-确认发布, 2-转助理
    private Long assistantId;
    private String status; // draft, assistant_processing, released, completed, cancelled
    private Integer totalTokenCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Task> tasks;

    public static class ChatMessage {
        private String role;
        private String content;
        private String time;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
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
    public List<ChatMessage> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ChatMessage> conversationHistory) { this.conversationHistory = conversationHistory; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public Integer getDesignerApproved() { return designerApproved; }
    public void setDesignerApproved(Integer designerApproved) { this.designerApproved = designerApproved; }
    public Long getAssistantId() { return assistantId; }
    public void setAssistantId(Long assistantId) { this.assistantId = assistantId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalTokenCost() { return totalTokenCost; }
    public void setTotalTokenCost(Integer totalTokenCost) { this.totalTokenCost = totalTokenCost; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
}