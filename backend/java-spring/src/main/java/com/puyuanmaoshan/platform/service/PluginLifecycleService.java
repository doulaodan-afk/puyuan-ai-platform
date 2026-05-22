package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.PluginLifecycleDtos.*;
import org.springframework.web.multipart.MultipartFile;

public interface PluginLifecycleService {

    /**
     * 上传插件包（zip），解压校验 manifest.json，发布前端资源
     */
    UploadPluginResponse uploadPlugin(MultipartFile file, boolean overrideExisting, Long operatorId);

    /**
     * 创建沙箱测试租户，返回沙箱访问 URL
     */
    SandboxTestResponse createSandboxTest(String pluginId, Long operatorId);

    /**
     * 发布插件（全量启用）
     */
    void publishPlugin(String pluginId, Long operatorId);

    /**
     * 下架插件
     */
    void offlinePlugin(String pluginId, Long operatorId);

    /**
     * 灰度发布插件
     */
    GrayPublishResponse grayPublish(String pluginId, java.util.List<Long> grayTenantIds, Long operatorId);

    /**
     * 查询插件状态
     */
    PluginStatusResponse getPluginStatus(String pluginId);

    /**
     * 后端部署（Mock 实现）
     */
    DeploymentTaskResponse deployBackend(String pluginId, DeployPluginRequest request, Long operatorId);

    /**
     * 执行部署任务（手动触发）
     */
    java.util.Map<String, Object> executeDeployTask(Long taskId, Long operatorId);

    /**
     * 扫描并自动执行待部署任务（定时任务）
     */
    void scanPendingDeployTasks();
}