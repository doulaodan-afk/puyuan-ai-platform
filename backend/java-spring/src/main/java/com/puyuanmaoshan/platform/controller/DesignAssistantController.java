package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.DesignRequirement;
import com.puyuanmaoshan.platform.service.*;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/design")
public class DesignAssistantController {
    private final DesignRequirementService designRequirementService;
    private final DesignTaskService designTaskService;
    private final FabricLibraryService fabricLibraryService;
    private final MessageService messageService;

    public DesignAssistantController(DesignRequirementService designRequirementService,
                                       DesignTaskService designTaskService,
                                       FabricLibraryService fabricLibraryService,
                                       MessageService messageService) {
        this.designRequirementService = designRequirementService;
        this.designTaskService = designTaskService;
        this.fabricLibraryService = fabricLibraryService;
        this.messageService = messageService;
    }

    // ====== 需求管理 ======

    @PostMapping("/requirement/create")
    public ApiResponse<DesignRequirement> createRequirement(
            @RequestBody DesignAssistantDtos.CreateRequirementRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignRequirement requirement = designRequirementService.createRequirement(request, parsedTenantId, parsedUserId);
        return ApiResponse.ok(requirement, requestId);
    }

    @PostMapping("/requirement/chat")
    public ApiResponse<DesignAssistantDtos.ChatResponse> chat(
            @RequestBody DesignAssistantDtos.ChatRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.ChatResponse response = designRequirementService.chat(request, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/requirement/summarize")
    public ApiResponse<DesignAssistantDtos.SummarizeResponse> summarize(
            @RequestBody DesignAssistantDtos.SummarizeRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        DesignAssistantDtos.SummarizeResponse response = designRequirementService.summarize(request.requirementId(), parsedTenantId);
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/requirement/confirm")
    public ApiResponse<DesignAssistantDtos.CommonResponse> confirmRequirement(
            @RequestBody DesignAssistantDtos.ConfirmRequirementRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designRequirementService.confirmRequirement(request.requirementId(), parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/requirement/transfer")
    public ApiResponse<DesignAssistantDtos.CommonResponse> transferToAssistant(
            @RequestBody DesignAssistantDtos.TransferToAssistantRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designRequirementService.transferToAssistant(request, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @GetMapping("/requirement/list")
    public ApiResponse<List<DesignAssistantDtos.RequirementListItem>> getRequirementList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        List<DesignAssistantDtos.RequirementListItem> response = designRequirementService.getRequirementList(parsedTenantId, status, page, size);
        return ApiResponse.ok(response, requestId);
    }

    @GetMapping("/requirement/detail/{id}")
    public ApiResponse<DesignAssistantDtos.RequirementDetailResponse> getRequirementDetail(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        DesignAssistantDtos.RequirementDetailResponse response = designRequirementService.getRequirementDetail(id, parsedTenantId);
        return ApiResponse.ok(response, requestId);
    }

    // ====== 助理操作 ======

    @PutMapping("/assistant/task")
    public ApiResponse<DesignAssistantDtos.CommonResponse> editTask(
            @RequestBody DesignAssistantDtos.EditTaskRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.editTask(request, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/assistant/task")
    public ApiResponse<DesignAssistantDtos.CommonResponse> createTask(
            @RequestBody DesignAssistantDtos.CreateTaskRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.createTask(request, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @DeleteMapping("/assistant/task/{taskId}")
    public ApiResponse<DesignAssistantDtos.CommonResponse> deleteTask(
            @PathVariable Long taskId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.deleteTask(taskId, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/assistant/publish/{requirementId}")
    public ApiResponse<DesignAssistantDtos.CommonResponse> publishRequirement(
            @PathVariable Long requirementId,
            @RequestParam(defaultValue = "false") boolean forcePublish,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.publishRequirement(requirementId, parsedTenantId, parsedUserId, forcePublish);
        return ApiResponse.ok(response, requestId);
    }

    // ====== 任务处理 ======

    @GetMapping("/task/my-tasks")
    public ApiResponse<DesignAssistantDtos.MyTasksResponse> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.MyTasksResponse response = designTaskService.getMyTasks(parsedUserId, parsedTenantId, status, taskType, page, size);
        return ApiResponse.ok(response, requestId);
    }

    @GetMapping("/task/detail/{taskId}")
    public ApiResponse<DesignAssistantDtos.TaskInfo> getTaskDetail(
            @PathVariable Long taskId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.TaskInfo response = designTaskService.getTaskDetail(taskId, parsedUserId, parsedTenantId);
        return ApiResponse.ok(response, requestId);
    }

    @PutMapping("/task/{taskId}/status")
    public ApiResponse<DesignAssistantDtos.CommonResponse> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestBody DesignAssistantDtos.UpdateTaskStatusRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.updateTaskStatus(
            new DesignAssistantDtos.UpdateTaskStatusRequest(taskId, request.status(), request.rejectReason()),
            parsedUserId, parsedTenantId
        );
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/task/{taskId}/ship")
    public ApiResponse<DesignAssistantDtos.CommonResponse> shipTask(
            @PathVariable Long taskId,
            @RequestBody DesignAssistantDtos.ShipTaskRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.shipTask(
            new DesignAssistantDtos.ShipTaskRequest(taskId, request.logisticsCompany(), request.logisticsTrackingNo(), request.offlineLogisticsNote()),
            parsedUserId, parsedTenantId
        );
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/task/{taskId}/upload-result")
    public ApiResponse<DesignAssistantDtos.CommonResponse> uploadResult(
            @PathVariable Long taskId,
            @RequestBody DesignAssistantDtos.UploadResultRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = designTaskService.uploadResult(
            new DesignAssistantDtos.UploadResultRequest(taskId, request.resultUrl()),
            parsedUserId, parsedTenantId
        );
        return ApiResponse.ok(response, requestId);
    }

    // ====== 面料库 ======

    /**
     * 获取面料库列表
     * @param creatorId 面料特供商用户ID（可选——特供商只看自己的；设计师不传则看全工作室的）
     */
    @GetMapping("/fabric-library/list")
    public ApiResponse<DesignAssistantDtos.FabricLibraryListResponse> getFabricLibraryList(
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "true") boolean onlyVisible,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        DesignAssistantDtos.FabricLibraryListResponse response = fabricLibraryService.getFabricLibraryList(
            parsedTenantId, creatorId, category, onlyVisible, page, size
        );
        return ApiResponse.ok(response, requestId);
    }

    @GetMapping("/fabric-library/{fabricId}")
    public ApiResponse<DesignAssistantDtos.FabricInfo> getFabricDetail(
            @PathVariable Long fabricId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.FabricInfo response = fabricLibraryService.getFabricDetail(fabricId, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @PostMapping("/fabric-library")
    public ApiResponse<DesignAssistantDtos.CommonResponse> createFabric(
            @RequestBody DesignAssistantDtos.CreateFabricRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = fabricLibraryService.createFabric(request, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @PutMapping("/fabric-library/{fabricId}")
    public ApiResponse<DesignAssistantDtos.CommonResponse> updateFabric(
            @PathVariable Long fabricId,
            @RequestBody DesignAssistantDtos.UpdateFabricRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        // 合并 fabricId 到请求中
        DesignAssistantDtos.UpdateFabricRequest newRequest = new DesignAssistantDtos.UpdateFabricRequest(
            fabricId, request.name(), request.category(), request.images(),
            request.videoUrl(), request.specs(), request.pricePerMeter(),
            request.stockStatus(), request.isVisible()
        );

        DesignAssistantDtos.CommonResponse response = fabricLibraryService.updateFabric(newRequest, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @DeleteMapping("/fabric-library/{fabricId}")
    public ApiResponse<DesignAssistantDtos.CommonResponse> deleteFabric(
            @PathVariable Long fabricId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = fabricLibraryService.deleteFabric(fabricId, parsedTenantId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    // ====== 消息 ======

    @GetMapping("/message/list")
    public ApiResponse<DesignAssistantDtos.MessageListResponse> getMessageList(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.MessageListResponse response = messageService.getMessageList(parsedUserId, type, unreadOnly, page, size);
        return ApiResponse.ok(response, requestId);
    }

    @PutMapping("/message/{messageId}/read")
    public ApiResponse<DesignAssistantDtos.CommonResponse> markMessageRead(
            @PathVariable Long messageId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedUserId = Long.parseLong(userId);

        DesignAssistantDtos.CommonResponse response = messageService.markMessageRead(messageId, parsedUserId);
        return ApiResponse.ok(response, requestId);
    }

    @GetMapping("/message/unread-count")
    public ApiResponse<Integer> getUnreadCount(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        long parsedUserId = Long.parseLong(userId);

        int count = messageService.getUnreadCount(parsedUserId);
        return ApiResponse.ok(count, requestId);
    }
}