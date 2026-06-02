package com.puyuanmaoshan.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DesignAssistantDtos {
    private DesignAssistantDtos() {}

    // ====== 需求管理 ======

    // 创建设计需求请求
    public record CreateRequirementRequest(
            @NotBlank String title,
            List<String> rawImages,
            List<String> rawVideos,
            String rawAudioUrl,
            @NotBlank String rawText,
            List<ChatMessage> conversationHistory,
            Long selectedSupplierId  // 选中的面料商ID
    ) {}

    // AI 对话请求
    public record ChatRequest(
            String sessionId,
            @NotBlank String message,
            Long requirementId
    ) {}

    // AI 对话响应
    public record ChatResponse(
            String sessionId,
            String assistantMessage,
            List<ChatMessage> conversationHistory
    ) {}

    // 聊天消息
    public record ChatMessage(
            String role, // user/assistant/system
            String content,
            LocalDateTime time
    ) {}

    // 生成总结请求
    public record SummarizeRequest(
            @NotNull Long requirementId
    ) {}

    // 生成总结响应
    public record SummarizeResponse(
            String aiSummary, // JSON string
            Map<String, Object> summaryData
    ) {}

    // 确认发布请求
    public record ConfirmRequirementRequest(
            @NotNull Long requirementId
    ) {}

    // 转给助理请求
    public record TransferToAssistantRequest(
            @NotNull Long requirementId,
            Long assistantId // 可选，不传则自动分配
    ) {}

    // 需求详情响应
    public record RequirementDetailResponse(
            Long id,
            Long tenantId,
            Long creatorId,
            String title,
            List<String> rawImages,
            List<String> rawVideos,
            String rawAudioUrl,
            String rawText,
            List<ChatMessage> conversationHistory,
            String aiSummary,
            Integer designerApproved,
            Long assistantId,
            String status,
            Integer totalTokenCost,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<TaskInfo> tasks
    ) {}

    // 需求列表项
    public record RequirementListItem(
            Long id,
            String title,
            String status,
            Integer totalTokenCost,
            LocalDateTime createdAt,
            Integer taskCount
    ) {}

    // ====== 助理操作 ======

    // 助理待办列表响应
    public record AssistantPendingListResponse(
            List<RequirementListItem> pendingRequirements
    ) {}

    // 编辑子任务请求
    public record EditTaskRequest(
            @NotNull Long taskId,
            Long assigneeId,
            LocalDateTime deadline,
            Map<String, Object> content
    ) {}

    // 新增子任务请求
    public record CreateTaskRequest(
            @NotNull Long requirementId,
            @NotBlank String taskType, // fabric/pattern
            @NotBlank String assigneeType, // supplier/pattern_service/internal
            @NotNull Long assigneeId,
            Map<String, Object> content,
            LocalDateTime deadline,
            Long fabricTaskId // 仅 pattern 任务需要
    ) {}

    // 发布需求请求
    public record PublishRequirementRequest(
            @NotNull Long requirementId,
            boolean forcePublish
    ) {}

    // ====== 任务处理 ======

    // 我的任务列表请求
    public record MyTasksRequest(
            String status,
            String taskType,
            int page,
            int size
    ) {}

    // 我的任务列表响应
    public record MyTasksResponse(
            List<TaskInfo> tasks,
            long total,
            int page,
            int size
    ) {}

    // 任务信息
    public record TaskInfo(
            Long id,
            Long requirementId,
            String taskType,
            String assigneeType,
            Long assigneeId,
            Map<String, Object> content,
            String status,
            LocalDateTime deadline,
            String resultUrl,
            Long fabricTaskId,
            String logisticsCompany,
            String logisticsTrackingNo,
            String logisticsStatus,
            String offlineLogisticsNote,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt,
            String rejectReason,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String requirementTitle,
            boolean canAccept, // 对于 pattern 任务，检查面料是否完成
            String cannotAcceptReason
    ) {}

    // 更新任务状态请求
    public record UpdateTaskStatusRequest(
            @NotNull Long taskId,
            @NotBlank String status, // accepted/rejected/done/cancelled
            String rejectReason
    ) {}

    // 发货请求
    public record ShipTaskRequest(
            @NotNull Long taskId,
            String logisticsCompany,
            String logisticsTrackingNo,
            String offlineLogisticsNote // 线下发货时的备注
    ) {}

    // 上传结果请求
    public record UploadResultRequest(
            @NotNull Long taskId,
            @NotBlank String resultUrl
    ) {}

    // ====== 面料库 ======

    // 面料库列表请求
    public record FabricLibraryListRequest(
            Long supplierTenantId,
            String category,
            boolean onlyVisible,
            int page,
            int size
    ) {}

    // 面料库列表响应
    public record FabricLibraryListResponse(
            List<FabricInfo> fabrics,
            long total,
            int page,
            int size
    ) {}

    // 面料信息
    public record FabricInfo(
            Long id,
            Long supplierTenantId,
            String name,
            String category,
            List<String> images,
            String videoUrl,
            Map<String, Object> specs,
            java.math.BigDecimal pricePerMeter,
            String stockStatus,
            Integer isVisible,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    // 添加面料请求
    public record CreateFabricRequest(
            @NotBlank String name,
            String category,
            List<String> images,
            String videoUrl,
            Map<String, Object> specs,
            java.math.BigDecimal pricePerMeter,
            String stockStatus
    ) {}

    // 编辑面料请求
    public record UpdateFabricRequest(
            @NotNull Long id,
            String name,
            String category,
            List<String> images,
            String videoUrl,
            Map<String, Object> specs,
            java.math.BigDecimal pricePerMeter,
            String stockStatus,
            Integer isVisible
    ) {}

    // ====== 站内信 ======

    // 站内信列表请求
    public record MessageListRequest(
            String type,
            boolean unreadOnly,
            int page,
            int size
    ) {}

    // 站内信列表响应
    public record MessageListResponse(
            List<MessageInfo> messages,
            long total,
            int page,
            int size
    ) {}

    // 站内信信息
    public record MessageInfo(
            Long id,
            Long receiverId,
            Long senderId,
            String title,
            String content,
            String type,
            boolean isRead,
            Long relatedId,
            LocalDateTime createdAt
    ) {}

    // 标记消息已读请求
    public record MarkMessageReadRequest(
            @NotNull Long id
    ) {}

    // ====== 统计 ======

    // 租户统计响应
    public record TenantStatsResponse(
            int totalRequirements,
            int draftCount,
            int processingCount,
            int releasedCount,
            int completedCount,
            long totalTokens,
            int totalTasks,
            int completedTasks,
            double taskCompletionRate
    ) {}

    // ====== 管理端 ======

    // 分配规则列表响应
    public record AssignRuleListResponse(
            List<AssignRuleInfo> rules
    ) {}

    // 分配规则信息
    public record AssignRuleInfo(
            Long id,
            String ruleName,
            String keyword,
            Long targetTenantId,
            String taskType,
            Integer priority,
            Integer enabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    // 创建分配规则请求
    public record CreateAssignRuleRequest(
            @NotBlank String ruleName,
            @NotBlank String keyword,
            @NotNull Long targetTenantId,
            @NotBlank String taskType,
            Integer priority
    ) {}

    // 编辑分配规则请求
    public record UpdateAssignRuleRequest(
            @NotNull Long id,
            String ruleName,
            String keyword,
            Long targetTenantId,
            Integer priority,
            Integer enabled
    ) {}

    // 租户助理列表响应
    public record TenantAssistantListResponse(
            List<AssistantInfo> assistants
    ) {}

    // 助理信息
    public record AssistantInfo(
            Long userId,
            String username,
            String tenantRole,
            LocalDateTime createdAt
    ) {}

    // 绑定助理请求
    public record BindAssistantRequest(
            @NotNull Long assistantUserId
    ) {}

    // 通用响应
    public record CommonResponse(
            boolean success,
            String message,
            Object data
    ) {}

    // 通用响应工具类
    public static class ResponseHelper {
        public static CommonResponse success() {
            return new CommonResponse(true, "操作成功", null);
        }

        public static CommonResponse success(String message) {
            return new CommonResponse(true, message, null);
        }

        public static CommonResponse success(Object data) {
            return new CommonResponse(true, "操作成功", data);
        }

        public static CommonResponse error(String message) {
            return new CommonResponse(false, message, null);
        }
    }
}