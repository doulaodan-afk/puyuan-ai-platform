package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;

public interface DesignTaskService {

    // 编辑子任务（助理使用）
    DesignAssistantDtos.CommonResponse editTask(DesignAssistantDtos.EditTaskRequest request, Long tenantId, Long userId);

    // 新增子任务（助理使用）
    DesignAssistantDtos.CommonResponse createTask(DesignAssistantDtos.CreateTaskRequest request, Long tenantId, Long userId);

    // 删除子任务（助理使用）
    DesignAssistantDtos.CommonResponse deleteTask(Long taskId, Long tenantId, Long userId);

    // 助理发布需求
    DesignAssistantDtos.CommonResponse publishRequirement(Long requirementId, Long tenantId, Long userId, boolean forcePublish);

    // 获取我的任务列表
    DesignAssistantDtos.MyTasksResponse getMyTasks(Long userId, Long tenantId, String status, String taskType, int page, int size);

    // 获取任务详情
    DesignAssistantDtos.TaskInfo getTaskDetail(Long taskId, Long userId, Long tenantId);

    // 更新任务状态
    DesignAssistantDtos.CommonResponse updateTaskStatus(DesignAssistantDtos.UpdateTaskStatusRequest request, Long userId, Long tenantId);

    // 发货（面料任务）
    DesignAssistantDtos.CommonResponse shipTask(DesignAssistantDtos.ShipTaskRequest request, Long userId, Long tenantId);

    // 上传结果文件
    DesignAssistantDtos.CommonResponse uploadResult(DesignAssistantDtos.UploadResultRequest request, Long userId, Long tenantId);

    // 检查任务是否可以接受（pattern 任务检查面料是否完成）
    boolean canAcceptTask(Long taskId, Long userId, Long tenantId);
}