package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.*;
import com.puyuanmaoshan.platform.mapper.DesignRequirementMapper;
import com.puyuanmaoshan.platform.mapper.DesignTaskMapper;
import com.puyuanmaoshan.platform.mapper.MessageMapper;
import com.puyuanmaoshan.platform.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DesignTaskServiceImpl implements DesignTaskService {
    private static final Logger logger = LoggerFactory.getLogger(DesignTaskServiceImpl.class);

    private final DesignTaskMapper designTaskMapper;
    private final DesignRequirementMapper designRequirementMapper;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public DesignTaskServiceImpl(DesignTaskMapper designTaskMapper,
                                 DesignRequirementMapper designRequirementMapper,
                                 MessageService messageService,
                                 ObjectMapper objectMapper) {
        this.designTaskMapper = designTaskMapper;
        this.designRequirementMapper = designRequirementMapper;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse editTask(DesignAssistantDtos.EditTaskRequest request, Long tenantId, Long userId) {
        try {
            DesignTask task = designTaskMapper.selectById(request.taskId());
            if (task == null) {
                return DesignAssistantDtos.ResponseHelper.error("任务不存在");
            }

            DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
            if (req == null || !req.getTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("无权操作此任务");
            }

            // 只有 draft 状态的任务可以编辑
            if (!"draft".equals(task.getStatus())) {
                return DesignAssistantDtos.ResponseHelper.error("只能编辑草稿状态的任务");
            }

            if (request.assigneeId() != null) {
                task.setAssigneeId(request.assigneeId());
            }
            if (request.deadline() != null) {
                task.setDeadline(request.deadline());
            }
            if (request.content() != null) {
                task.setContent(objectMapper.writeValueAsString(request.content()));
            }
            task.setUpdatedAt(LocalDateTime.now());

            designTaskMapper.updateById(task);

            return DesignAssistantDtos.ResponseHelper.success("任务已更新");

        } catch (Exception e) {
            logger.error("Edit task failed", e);
            return DesignAssistantDtos.ResponseHelper.error("编辑失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse createTask(DesignAssistantDtos.CreateTaskRequest request, Long tenantId, Long userId) {
        try {
            DesignRequirement req = designRequirementMapper.selectById(request.requirementId());
            if (req == null || !req.getTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("需求不存在或无权操作");
            }

            // 检查 fabric_task_id 是否有效（对于 pattern 任务）
            Long fabricTaskId = 0L;
            if ("pattern".equals(request.taskType()) && request.fabricTaskId() != null && request.fabricTaskId() > 0) {
                DesignTask fabricTask = designTaskMapper.selectById(request.fabricTaskId());
                if (fabricTask == null || !"fabric".equals(fabricTask.getTaskType())) {
                    return DesignAssistantDtos.ResponseHelper.error("无效的面料任务ID");
                }
                fabricTaskId = request.fabricTaskId();
            }

            DesignTask task = DesignTask.builder()
                    .requirementId(request.requirementId())
                    .taskType(request.taskType())
                    .assigneeType(request.assigneeType())
                    .assigneeId(request.assigneeId())
                    .content(request.content() != null ? objectMapper.writeValueAsString(request.content()) : "{}")
                    .status("draft")
                    .deadline(request.deadline())
                    .fabricTaskId(fabricTaskId)
                    .createdAt(LocalDateTime.now())
                    .build();

            designTaskMapper.insert(task);

            return DesignAssistantDtos.ResponseHelper.success("任务已创建");

        } catch (Exception e) {
            logger.error("Create task failed", e);
            return DesignAssistantDtos.ResponseHelper.error("创建失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse deleteTask(Long taskId, Long tenantId, Long userId) {
        try {
            DesignTask task = designTaskMapper.selectById(taskId);
            if (task == null) {
                return DesignAssistantDtos.ResponseHelper.error("任务不存在");
            }

            DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
            if (req == null || !req.getTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("无权操作此任务");
            }

            // 只有 draft 状态的任务可以删除
            if (!"draft".equals(task.getStatus())) {
                return DesignAssistantDtos.ResponseHelper.error("只能删除草稿状态的任务");
            }

            designTaskMapper.deleteById(taskId);

            return DesignAssistantDtos.ResponseHelper.success("任务已删除");

        } catch (Exception e) {
            logger.error("Delete task failed", e);
            return DesignAssistantDtos.ResponseHelper.error("删除失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse publishRequirement(Long requirementId, Long tenantId, Long userId, boolean forcePublish) {
        try {
            DesignRequirement req = designRequirementMapper.selectById(requirementId);
            if (req == null || !req.getTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("需求不存在或无权操作");
            }

            // 获取所有 draft 状态的任务
            List<DesignTask> draftTasks = designTaskMapper.selectList(
                new LambdaQueryWrapper<DesignTask>()
                    .eq(DesignTask::getRequirementId, requirementId)
                    .eq(DesignTask::getStatus, "draft")
            );

            if (draftTasks.isEmpty() && !forcePublish) {
                return DesignAssistantDtos.ResponseHelper.error("没有待发布的任务");
            }

            // 将所有任务状态改为 pending
            for (DesignTask task : draftTasks) {
                task.setStatus("pending");
                task.setUpdatedAt(LocalDateTime.now());
                designTaskMapper.updateById(task);

                // 发送站内信
                messageService.sendMessage(task.getAssigneeId(), "新任务分配",
                    "您收到了一个新的" + ("fabric".equals(task.getTaskType()) ? "面料" : "打版") + "任务",
                    "task", task.getId());
            }

            // 更新需求状态
            req.setStatus("released");
            designRequirementMapper.updateById(req);

            return DesignAssistantDtos.ResponseHelper.success("需求已发布");

        } catch (Exception e) {
            logger.error("Publish requirement failed", e);
            return DesignAssistantDtos.ResponseHelper.error("发布失败: " + e.getMessage());
        }
    }

    @Override
    public DesignAssistantDtos.MyTasksResponse getMyTasks(Long userId, Long tenantId, String status, String taskType, int page, int size) {
        try {
            // TODO: 根据实际用户角色和租户类型筛选任务
            // 这里简化处理，假设 userId 就是 assigneeId

            LambdaQueryWrapper<DesignTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DesignTask::getAssigneeId, userId);

            if (status != null && !status.isEmpty()) {
                wrapper.eq(DesignTask::getStatus, status);
            }
            if (taskType != null && !taskType.isEmpty()) {
                wrapper.eq(DesignTask::getTaskType, taskType);
            }
            wrapper.orderByDesc(DesignTask::getCreatedAt);

            long total = designTaskMapper.selectCount(wrapper);
            int offset = (page - 1) * size;
            wrapper.last("LIMIT " + offset + ", " + size);

            List<DesignTask> tasks = designTaskMapper.selectList(wrapper);
            List<DesignAssistantDtos.TaskInfo> result = new ArrayList<>();

            for (DesignTask task : tasks) {
                DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
                Map<String, Object> content = objectMapper.readValue(
                    task.getContent(), new TypeReference<Map<String, Object>>() {}
                );

                boolean canAccept = canAcceptTask(task.getId(), userId, tenantId);
                String cannotAcceptReason = null;
                if (!canAccept && "pattern".equals(task.getTaskType())) {
                    cannotAcceptReason = "等待面料准备就绪";
                }

                result.add(new DesignAssistantDtos.TaskInfo(
                    task.getId(), task.getRequirementId(), task.getTaskType(),
                    task.getAssigneeType(), task.getAssigneeId(), content,
                    task.getStatus(), task.getDeadline(), task.getResultUrl(),
                    task.getFabricTaskId(), task.getLogisticsCompany(),
                    task.getLogisticsTrackingNo(), task.getLogisticsStatus(),
                    task.getOfflineLogisticsNote(), task.getShippedAt(),
                    task.getDeliveredAt(), task.getRejectReason(),
                    task.getCompletedAt(), task.getCreatedAt(),
                    task.getUpdatedAt(), req != null ? req.getTitle() : "",
                    canAccept, cannotAcceptReason
                ));
            }

            return new DesignAssistantDtos.MyTasksResponse(result, total, page, size);

        } catch (Exception e) {
            logger.error("Get my tasks failed", e);
            throw new RuntimeException("获取任务列表失败: " + e.getMessage());
        }
    }

    @Override
    public DesignAssistantDtos.TaskInfo getTaskDetail(Long taskId, Long userId, Long tenantId) {
        try {
            DesignTask task = designTaskMapper.selectById(taskId);
            if (task == null || !task.getAssigneeId().equals(userId)) {
                throw new RuntimeException("任务不存在或无权访问");
            }

            DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
            Map<String, Object> content = objectMapper.readValue(
                task.getContent(), new TypeReference<Map<String, Object>>() {}
            );

            boolean canAccept = canAcceptTask(taskId, userId, tenantId);
            String cannotAcceptReason = null;
            if (!canAccept && "pattern".equals(task.getTaskType())) {
                cannotAcceptReason = "等待面料准备就绪";
            }

            return new DesignAssistantDtos.TaskInfo(
                task.getId(), task.getRequirementId(), task.getTaskType(),
                task.getAssigneeType(), task.getAssigneeId(), content,
                task.getStatus(), task.getDeadline(), task.getResultUrl(),
                task.getFabricTaskId(), task.getLogisticsCompany(),
                task.getLogisticsTrackingNo(), task.getLogisticsStatus(),
                task.getOfflineLogisticsNote(), task.getShippedAt(),
                task.getDeliveredAt(), task.getRejectReason(),
                task.getCompletedAt(), task.getCreatedAt(),
                task.getUpdatedAt(), req != null ? req.getTitle() : "",
                canAccept, cannotAcceptReason
            );

        } catch (Exception e) {
            logger.error("Get task detail failed", e);
            throw new RuntimeException("获取任务详情失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse updateTaskStatus(DesignAssistantDtos.UpdateTaskStatusRequest request, Long userId, Long tenantId) {
        try {
            DesignTask task = designTaskMapper.selectById(request.taskId());
            if (task == null || !task.getAssigneeId().equals(userId)) {
                return DesignAssistantDtos.ResponseHelper.error("任务不存在或无权操作");
            }

            String newStatus = request.status();

            // 检查状态流转是否合法
            if (!isValidStatusTransition(task.getStatus(), newStatus)) {
                return DesignAssistantDtos.ResponseHelper.error("无效的状态流转");
            }

            // 对于 pattern 任务，检查面料是否完成
            if ("pattern".equals(task.getTaskType()) && "accepted".equals(newStatus)) {
                if (!canAcceptTask(request.taskId(), userId, tenantId)) {
                    return DesignAssistantDtos.ResponseHelper.error("面料尚未准备就绪，无法接受任务");
                }
            }

            task.setStatus(newStatus);

            if ("rejected".equals(newStatus)) {
                task.setRejectReason(request.rejectReason());
            }
            if ("done".equals(newStatus)) {
                task.setCompletedAt(LocalDateTime.now());
            }

            task.setUpdatedAt(LocalDateTime.now());
            designTaskMapper.updateById(task);

            // 通知需求创建者
            DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
            if (req != null) {
                String statusText = switch (newStatus) {
                    case "accepted" -> "已接受";
                    case "rejected" -> "已拒绝";
                    case "done" -> "已完成";
                    case "cancelled" -> "已取消";
                    default -> "状态已更新";
                };
                messageService.sendMessage(req.getCreatorId(), "任务状态更新",
                    "您的设计任务" + statusText, "task", task.getId());
            }

            return DesignAssistantDtos.ResponseHelper.success("状态已更新");

        } catch (Exception e) {
            logger.error("Update task status failed", e);
            return DesignAssistantDtos.ResponseHelper.error("更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse shipTask(DesignAssistantDtos.ShipTaskRequest request, Long userId, Long tenantId) {
        try {
            DesignTask task = designTaskMapper.selectById(request.taskId());
            if (task == null || !task.getAssigneeId().equals(userId)) {
                return DesignAssistantDtos.ResponseHelper.error("任务不存在或无权操作");
            }

            if (!"fabric".equals(task.getTaskType())) {
                return DesignAssistantDtos.ResponseHelper.error("只有面料任务可以发货");
            }

            if (!"accepted".equals(task.getStatus())) {
                return DesignAssistantDtos.ResponseHelper.error("任务未接受，无法发货");
            }

            if (request.logisticsCompany() != null && request.logisticsTrackingNo() != null) {
                task.setLogisticsCompany(request.logisticsCompany());
                task.setLogisticsTrackingNo(request.logisticsTrackingNo());
                task.setLogisticsStatus("shipped");
            }
            if (request.offlineLogisticsNote() != null) {
                task.setOfflineLogisticsNote(request.offlineLogisticsNote());
                task.setLogisticsStatus("shipped");
            }

            task.setShippedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            designTaskMapper.updateById(task);

            // 通知需求创建者
            DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
            if (req != null) {
                messageService.sendMessage(req.getCreatorId(), "任务已发货",
                    "您的面料任务已发货", "task", task.getId());
            }

            return DesignAssistantDtos.ResponseHelper.success("发货成功");

        } catch (Exception e) {
            logger.error("Ship task failed", e);
            return DesignAssistantDtos.ResponseHelper.error("发货失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse uploadResult(DesignAssistantDtos.UploadResultRequest request, Long userId, Long tenantId) {
        try {
            DesignTask task = designTaskMapper.selectById(request.taskId());
            if (task == null || !task.getAssigneeId().equals(userId)) {
                return DesignAssistantDtos.ResponseHelper.error("任务不存在或无权操作");
            }

            task.setResultUrl(request.resultUrl());

            if ("accepted".equals(task.getStatus())) {
                task.setStatus("done");
                task.setCompletedAt(LocalDateTime.now());
            }

            task.setUpdatedAt(LocalDateTime.now());
            designTaskMapper.updateById(task);

            // 通知需求创建者
            DesignRequirement req = designRequirementMapper.selectById(task.getRequirementId());
            if (req != null) {
                messageService.sendMessage(req.getCreatorId(), "任务结果已上传",
                    "您的设计任务结果已上传", "task", task.getId());
            }

            return DesignAssistantDtos.ResponseHelper.success("上传成功");

        } catch (Exception e) {
            logger.error("Upload result failed", e);
            return DesignAssistantDtos.ResponseHelper.error("上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean canAcceptTask(Long taskId, Long userId, Long tenantId) {
        try {
            DesignTask task = designTaskMapper.selectById(taskId);
            if (task == null) {
                return false;
            }

            // 只有 pattern 任务需要检查面料状态
            if (!"pattern".equals(task.getTaskType())) {
                return true;
            }

            // 检查关联的面料任务
            if (task.getFabricTaskId() != null && task.getFabricTaskId() > 0) {
                DesignTask fabricTask = designTaskMapper.selectById(task.getFabricTaskId());
                if (fabricTask != null) {
                    return "delivered".equals(fabricTask.getStatus()) || "done".equals(fabricTask.getStatus());
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("Can accept task check failed", e);
            return false;
        }
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "pending" -> "accepted".equals(newStatus) || "rejected".equals(newStatus) || "cancelled".equals(newStatus);
            case "accepted" -> "shipped".equals(newStatus) || "done".equals(newStatus) || "cancelled".equals(newStatus);
            case "shipped" -> "delivered".equals(newStatus) || "done".equals(newStatus);
            case "delivered" -> "done".equals(newStatus);
            default -> false;
        };
    }
}