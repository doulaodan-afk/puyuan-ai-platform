package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.*;
import com.puyuanmaoshan.platform.mapper.*;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.DesignAssistantAiService;
import com.puyuanmaoshan.platform.service.DesignRequirementService;
import com.puyuanmaoshan.platform.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DesignRequirementServiceImpl implements DesignRequirementService {
    private static final Logger logger = LoggerFactory.getLogger(DesignRequirementServiceImpl.class);

    private final DesignRequirementMapper designRequirementMapper;
    private final DesignTaskMapper designTaskMapper;
    private final RequirementFabricMapper requirementFabricMapper;
    private final FabricLibraryMapper fabricLibraryMapper;
    private final AiSessionMapper aiSessionMapper;
    private final TaskAssignRuleMapper taskAssignRuleMapper;
    private final DesignAssistantAiService aiService;
    private final AccountWalletService accountWalletService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public DesignRequirementServiceImpl(DesignRequirementMapper designRequirementMapper,
                                         DesignTaskMapper designTaskMapper,
                                         RequirementFabricMapper requirementFabricMapper,
                                         FabricLibraryMapper fabricLibraryMapper,
                                         AiSessionMapper aiSessionMapper,
                                         TaskAssignRuleMapper taskAssignRuleMapper,
                                         DesignAssistantAiService aiService,
                                         AccountWalletService accountWalletService,
                                         MessageService messageService,
                                         ObjectMapper objectMapper) {
        this.designRequirementMapper = designRequirementMapper;
        this.designTaskMapper = designTaskMapper;
        this.requirementFabricMapper = requirementFabricMapper;
        this.fabricLibraryMapper = fabricLibraryMapper;
        this.aiSessionMapper = aiSessionMapper;
        this.taskAssignRuleMapper = taskAssignRuleMapper;
        this.aiService = aiService;
        this.accountWalletService = accountWalletService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DesignRequirement createRequirement(DesignAssistantDtos.CreateRequirementRequest request, Long tenantId, Long userId) {
        try {
            String imagesJson = objectMapper.writeValueAsString(request.rawImages() != null ? request.rawImages() : new ArrayList<>());
            String videosJson = objectMapper.writeValueAsString(request.rawVideos() != null ? request.rawVideos() : new ArrayList<>());
            String historyJson = objectMapper.writeValueAsString(request.conversationHistory() != null ? request.conversationHistory() : new ArrayList<>());

            DesignRequirement requirement = DesignRequirement.builder()
                    .tenantId(tenantId)
                    .creatorId(userId)
                    .title(request.title())
                    .rawImages(imagesJson)
                    .rawVideos(videosJson)
                    .rawAudioUrl(request.rawAudioUrl())
                    .rawText(request.rawText())
                    .conversationHistory(historyJson)
                    .status("draft")
                    .totalTokenCost(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            designRequirementMapper.insert(requirement);
            logger.info("Created requirement {} for tenant {}", requirement.getId(), tenantId);

            // ====== 面料关联（新增：支持多面料多供应商） ======
            if (request.selectedFabrics() != null && !request.selectedFabrics().isEmpty()) {
                for (var selection : request.selectedFabrics()) {
                    FabricLibrary fabric = fabricLibraryMapper.selectById(selection.fabricId());
                    if (fabric != null) {
                        RequirementFabric rf = RequirementFabric.builder()
                                .requirementId(requirement.getId())
                                .fabricId(selection.fabricId())
                                .fabricSupplierId(selection.fabricSupplierId() != null
                                        ? selection.fabricSupplierId() : fabric.getCreatorId())
                                .quantity(selection.quantity())
                                .createdAt(LocalDateTime.now())
                                .build();
                        requirementFabricMapper.insert(rf);
                    }
                }
            }

            // 如果选择了面料商（旧逻辑兼容），直接创建面料任务
            if (request.selectedSupplierId() != null && request.selectedSupplierId() > 0) {
                assignFabricTaskToSupplier(requirement.getId(), request.selectedSupplierId());
            }

            return requirement;
        } catch (Exception e) {
            logger.error("Failed to create requirement", e);
            throw new RuntimeException("创建需求失败: " + e.getMessage());
        }
    }

    // 为选中的面料商分配任务
    private void assignFabricTaskToSupplier(Long requirementId, Long supplierTenantId) {
        try {
            Map<String, Object> taskContent = Map.of(
                "description", "面料商根据需求提供面料",
                "requirementId", requirementId
            );

            DesignTask fabricTask = DesignTask.builder()
                .requirementId(requirementId)
                .taskType("fabric")
                .assigneeType("supplier")
                .assigneeId(supplierTenantId)
                .content(objectMapper.writeValueAsString(taskContent))
                .status("pending")
                .deadline(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

            designTaskMapper.insert(fabricTask);
            logger.info("Assigned fabric task {} to supplier {} for requirement {}", fabricTask.getId(), supplierTenantId, requirementId);
        } catch (Exception e) {
            logger.error("Assign fabric task to supplier failed", e);
            // 不抛出异常，避免影响需求创建
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.ChatResponse chat(DesignAssistantDtos.ChatRequest request, Long tenantId, Long userId) {
        try {
            AiSession session;
            List<DesignAssistantDtos.ChatMessage> history;

            if (request.sessionId() != null && !request.sessionId().isEmpty()) {
                // 获取现有会话
                session = aiSessionMapper.selectOne(
                    new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getSessionId, request.sessionId())
                        .eq(AiSession::getTenantId, tenantId)
                        .eq(AiSession::getUserId, userId)
                );
            } else {
                // 创建新会话
                String newSessionId = UUID.randomUUID().toString();
                session = AiSession.builder()
                        .sessionId(newSessionId)
                        .tenantId(tenantId)
                        .userId(userId)
                        .requirementId(request.requirementId() != null ? request.requirementId() : 0L)
                        .context("[]")
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .createdAt(LocalDateTime.now())
                        .build();
                aiSessionMapper.insert(session);
            }

            if (session == null) {
                session = AiSession.builder()
                        .sessionId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .userId(userId)
                        .requirementId(0L)
                        .context("[]")
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .createdAt(LocalDateTime.now())
                        .build();
                aiSessionMapper.insert(session);
            }

            history = objectMapper.readValue(session.getContext(), new TypeReference<List<DesignAssistantDtos.ChatMessage>>() {});

            // 添加用户消息
            DesignAssistantDtos.ChatMessage userMsg = new DesignAssistantDtos.ChatMessage(
                "user", request.message(), LocalDateTime.now()
            );
            history.add(userMsg);

            // 调用 AI
            String contextJson = objectMapper.writeValueAsString(history);
            String assistantReply = aiService.chat(request.message(), contextJson, tenantId);

            // 添加助手消息
            DesignAssistantDtos.ChatMessage assistantMsg = new DesignAssistantDtos.ChatMessage(
                "assistant", assistantReply, LocalDateTime.now()
            );
            history.add(assistantMsg);

            // 更新会话上下文
            session.setContext(objectMapper.writeValueAsString(history));
            session.setUpdatedAt(LocalDateTime.now());
            aiSessionMapper.updateById(session);

            // 扣费
            int tokenCost = 5; // 每次对话 5 tokens
            accountWalletService.deductToken(tenantId, tokenCost, "ai_design_assistant");

            // 更新需求（如果有关联）
            if (session.getRequirementId() != null && session.getRequirementId() > 0) {
                DesignRequirement requirement = designRequirementMapper.selectById(session.getRequirementId());
                if (requirement != null) {
                    requirement.setConversationHistory(session.getContext());
                    requirement.setTotalTokenCost(requirement.getTotalTokenCost() + tokenCost);
                    designRequirementMapper.updateById(requirement);
                }
            }

            return new DesignAssistantDtos.ChatResponse(session.getSessionId(), assistantReply, history);

        } catch (Exception e) {
            logger.error("Chat failed", e);
            throw new RuntimeException("对话失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.SummarizeResponse summarize(Long requirementId, Long tenantId) {
        try {
            DesignRequirement requirement = designRequirementMapper.selectById(requirementId);
            if (requirement == null || !requirement.getTenantId().equals(tenantId)) {
                throw new RuntimeException("需求不存在或无权访问");
            }

            String aiSummary = aiService.summarize(requirement.getConversationHistory(), tenantId);

            requirement.setAiSummary(aiSummary);
            designRequirementMapper.updateById(requirement);

            // 扣费
            int tokenCost = 15; // 生成总结 15 tokens
            accountWalletService.deductToken(tenantId, tokenCost, "ai_design_assistant");
            requirement.setTotalTokenCost(requirement.getTotalTokenCost() + tokenCost);
            designRequirementMapper.updateById(requirement);

            Map<String, Object> summaryData = objectMapper.readValue(aiSummary, new TypeReference<Map<String, Object>>() {});

            return new DesignAssistantDtos.SummarizeResponse(aiSummary, summaryData);

        } catch (Exception e) {
            logger.error("Summarize failed", e);
            throw new RuntimeException("生成总结失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse confirmRequirement(Long requirementId, Long tenantId, Long userId) {
        try {
            DesignRequirement requirement = designRequirementMapper.selectById(requirementId);
            if (requirement == null || !requirement.getTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("需求不存在或无权访问");
            }

            if (requirement.getAiSummary() == null || requirement.getAiSummary().isEmpty()) {
                return DesignAssistantDtos.ResponseHelper.error("请先生成 AI 总结");
            }

            // 自动拆分任务
            List<Map<String, Object>> tasks = aiService.splitTasks(requirement.getAiSummary(), tenantId);

            // 创建任务
            LocalDateTime defaultDeadline = LocalDateTime.now().plusDays(7);
            for (Map<String, Object> taskData : tasks) {
                String taskType = (String) taskData.get("taskType");
                Long assigneeId = findAssignee(taskType, taskData, tenantId);

                if (assigneeId != null) {
                    DesignTask task = DesignTask.builder()
                            .requirementId(requirementId)
                            .taskType(taskType)
                            .assigneeType(taskType.equals("fabric") ? "supplier" : "pattern_service")
                            .assigneeId(assigneeId)
                            .content(objectMapper.writeValueAsString(taskData))
                            .status("pending")
                            .deadline(defaultDeadline)
                            .createdAt(LocalDateTime.now())
                            .build();
                    designTaskMapper.insert(task);
                }
            }

            // 更新需求状态
            requirement.setDesignerApproved(1);
            requirement.setStatus("released");
            designRequirementMapper.updateById(requirement);

            // 发送通知
            List<Long> assigneeIds = designTaskMapper.selectList(
                new LambdaQueryWrapper<DesignTask>()
                    .eq(DesignTask::getRequirementId, requirementId)
            ).stream().map(DesignTask::getAssigneeId).toList();

            for (Long assigneeId : assigneeIds) {
                messageService.sendMessage(assigneeId, "新任务分配", "您收到了一个新的设计任务", "task", requirementId);
            }

            return DesignAssistantDtos.ResponseHelper.success("需求已确认并发布");

        } catch (Exception e) {
            logger.error("Confirm requirement failed", e);
            return DesignAssistantDtos.ResponseHelper.error("确认失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse transferToAssistant(DesignAssistantDtos.TransferToAssistantRequest request, Long tenantId, Long userId) {
        try {
            DesignRequirement requirement = designRequirementMapper.selectById(request.requirementId());
            if (requirement == null || !requirement.getTenantId().equals(tenantId)) {
                return DesignAssistantDtos.ResponseHelper.error("需求不存在或无权访问");
            }

            // 找到绑定的助理
            Long assistantId = request.assistantId();
            if (assistantId == null) {
                assistantId = findDefaultAssistant(tenantId);
            }

            if (assistantId == null) {
                return DesignAssistantDtos.ResponseHelper.error("未找到可用的设计助理");
            }

            // 更新需求状态
            requirement.setDesignerApproved(2);
            requirement.setAssistantId(assistantId);
            requirement.setStatus("assistant_processing");
            designRequirementMapper.updateById(requirement);

            // 预拆分任务（draft 状态）
            if (requirement.getAiSummary() != null) {
                List<Map<String, Object>> tasks = aiService.splitTasks(requirement.getAiSummary(), tenantId);
                LocalDateTime defaultDeadline = LocalDateTime.now().plusDays(7);

                for (Map<String, Object> taskData : tasks) {
                    String taskType = (String) taskData.get("taskType");
                    Long assigneeId = findAssignee(taskType, taskData, tenantId);

                    if (assigneeId != null) {
                        DesignTask task = DesignTask.builder()
                                .requirementId(request.requirementId())
                                .taskType(taskType)
                                .assigneeType(taskType.equals("fabric") ? "supplier" : "pattern_service")
                                .assigneeId(assigneeId)
                                .content(objectMapper.writeValueAsString(taskData))
                                .status("draft")
                                .deadline(defaultDeadline)
                                .createdAt(LocalDateTime.now())
                                .build();
                        designTaskMapper.insert(task);
                    }
                }
            }

            // 通知助理
            messageService.sendMessage(assistantId, "新的待处理需求", "您有一个新的设计需求待处理", "task", request.requirementId());

            return DesignAssistantDtos.ResponseHelper.success("需求已转给设计助理");

        } catch (Exception e) {
            logger.error("Transfer to assistant failed", e);
            return DesignAssistantDtos.ResponseHelper.error("转交失败: " + e.getMessage());
        }
    }

    @Override
    public List<DesignAssistantDtos.RequirementListItem> getRequirementList(Long tenantId, String status, int page, int size) {
        try {
            LambdaQueryWrapper<DesignRequirement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DesignRequirement::getTenantId, tenantId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(DesignRequirement::getStatus, status);
            }
            wrapper.orderByDesc(DesignRequirement::getCreatedAt);

            IPage<DesignRequirement> pageObj = designRequirementMapper.selectPage(new Page<>(page, size), wrapper);

            List<DesignAssistantDtos.RequirementListItem> result = new ArrayList<>();
            for (DesignRequirement req : pageObj.getRecords()) {
                Long taskCount = designTaskMapper.selectCount(
                    new LambdaQueryWrapper<DesignTask>().eq(DesignTask::getRequirementId, req.getId())
                );

                result.add(new DesignAssistantDtos.RequirementListItem(
                    req.getId(), req.getTitle(), req.getStatus(),
                    req.getTotalTokenCost(), req.getCreatedAt(), taskCount.intValue()
                ));
            }

            return result;

        } catch (Exception e) {
            logger.error("Get requirement list failed", e);
            throw new RuntimeException("获取需求列表失败: " + e.getMessage());
        }
    }

    @Override
    public DesignAssistantDtos.RequirementDetailResponse getRequirementDetail(Long requirementId, Long tenantId) {
        try {
            DesignRequirement requirement = designRequirementMapper.selectById(requirementId);
            if (requirement == null || !requirement.getTenantId().equals(tenantId)) {
                throw new RuntimeException("需求不存在或无权访问");
            }

            List<DesignAssistantDtos.ChatMessage> history = objectMapper.readValue(
                requirement.getConversationHistory() != null ? requirement.getConversationHistory() : "[]",
                new TypeReference<List<DesignAssistantDtos.ChatMessage>>() {}
            );

            List<String> images = objectMapper.readValue(
                requirement.getRawImages() != null ? requirement.getRawImages() : "[]",
                new TypeReference<List<String>>() {}
            );

            List<String> videos = objectMapper.readValue(
                requirement.getRawVideos() != null ? requirement.getRawVideos() : "[]",
                new TypeReference<List<String>>() {}
            );

            // 获取关联任务
            List<DesignTask> tasks = designTaskMapper.selectList(
                new LambdaQueryWrapper<DesignTask>().eq(DesignTask::getRequirementId, requirementId)
            );

            List<DesignAssistantDtos.TaskInfo> taskInfos = new ArrayList<>();
            for (DesignTask task : tasks) {
                Map<String, Object> content = objectMapper.readValue(
                    task.getContent(), new TypeReference<Map<String, Object>>() {}
                );
                taskInfos.add(new DesignAssistantDtos.TaskInfo(
                    task.getId(), task.getRequirementId(), task.getTaskType(),
                    task.getAssigneeType(), task.getAssigneeId(), content,
                    task.getStatus(), task.getDeadline(), task.getResultUrl(),
                    task.getFabricTaskId(), task.getLogisticsCompany(),
                    task.getLogisticsTrackingNo(), task.getLogisticsStatus(),
                    task.getOfflineLogisticsNote(), task.getShippedAt(),
                    task.getDeliveredAt(), task.getRejectReason(),
                    task.getCompletedAt(), task.getCreatedAt(),
                    task.getUpdatedAt(), requirement.getTitle(),
                    false, null
                ));
            }

            // 加载关联的面料列表
            List<RequirementFabric> reqFabrics = requirementFabricMapper.selectList(
                new LambdaQueryWrapper<RequirementFabric>()
                    .eq(RequirementFabric::getRequirementId, requirementId)
            );
            List<DesignAssistantDtos.FabricInfo> linkedFabrics = new ArrayList<>();
            for (RequirementFabric rf : reqFabrics) {
                FabricLibrary fabric = fabricLibraryMapper.selectById(rf.getFabricId());
                if (fabric != null) {
                    linkedFabrics.add(toFabricInfo(fabric));
                }
            }

            return new DesignAssistantDtos.RequirementDetailResponse(
                requirement.getId(), requirement.getTenantId(), requirement.getCreatorId(),
                requirement.getTitle(), images, videos, requirement.getRawAudioUrl(),
                requirement.getRawText(), history, requirement.getAiSummary(),
                requirement.getDesignerApproved(), requirement.getAssistantId(),
                requirement.getStatus(), requirement.getTotalTokenCost(),
                requirement.getCreatedAt(), requirement.getUpdatedAt(), taskInfos, linkedFabrics
            );

        } catch (Exception e) {
            logger.error("Get requirement detail failed", e);
            throw new RuntimeException("获取需求详情失败: " + e.getMessage());
        }
    }

    // 查找任务分配对象
    private Long findAssignee(String taskType, Map<String, Object> taskData, Long tenantId) {
        try {
            // 根据关键词匹配规则
            String content = objectMapper.writeValueAsString(taskData);

            LambdaQueryWrapper<TaskAssignRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskAssignRule::getTaskType, taskType);
            wrapper.eq(TaskAssignRule::getEnabled, 1);
            wrapper.orderByDesc(TaskAssignRule::getPriority);

            List<TaskAssignRule> rules = taskAssignRuleMapper.selectList(wrapper);

            for (TaskAssignRule rule : rules) {
                if (content.contains(rule.getKeyword())) {
                    return rule.getTargetTenantId();
                }
            }

            // 返回默认分配对象（从配置中获取）
            return taskAssignRuleMapper.selectOne(
                new LambdaQueryWrapper<TaskAssignRule>()
                    .eq(TaskAssignRule::getTaskType, taskType)
                    .eq(TaskAssignRule::getEnabled, 1)
                    .eq(TaskAssignRule::getPriority, 0)
            ).getTargetTenantId();

        } catch (Exception e) {
            logger.error("Find assignee failed", e);
            return null;
        }
    }

    // 查找默认助理
    private Long findDefaultAssistant(Long tenantId) {
        // 查找租户绑定的第一个助理
        // 这里简化实现，实际应该有更复杂的逻辑
        return null;
    }

    /** 将 FabricLibrary 实体转为 DTO（简化版，用于需求详情中展示关联面料） */
    private DesignAssistantDtos.FabricInfo toFabricInfo(FabricLibrary fabric) {
        try {
            List<String> imgs = objectMapper.readValue(
                    fabric.getImages() != null ? fabric.getImages() : "[]",
                    new TypeReference<List<String>>() {});
            Map<String, Object> sps = objectMapper.readValue(
                    fabric.getSpecs() != null ? fabric.getSpecs() : "{}",
                    new TypeReference<Map<String, Object>>() {});
            return new DesignAssistantDtos.FabricInfo(
                    fabric.getId(), fabric.getSupplierTenantId(), fabric.getName(),
                    fabric.getCategory(), imgs, fabric.getVideoUrl(),
                    sps, fabric.getPricePerMeter(), fabric.getStockStatus(),
                    fabric.getIsVisible(), fabric.getCreatedAt(), fabric.getUpdatedAt(),
                    fabric.getCreatorId(), null
            );
        } catch (Exception e) {
            return null;
        }
    }
}