package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.puyuanmaoshan.platform.dto.PluginLifecycleDtos.*;
import com.puyuanmaoshan.platform.entity.*;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.mapper.*;
import com.puyuanmaoshan.platform.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PluginLifecycleServiceImpl implements PluginLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleServiceImpl.class);

    private final PluginService pluginService;
    private final TenantService tenantService;
    private final TenantPluginService tenantPluginService;
    private final AccountWalletService accountWalletService;
    private final AuditLogService auditLogService;
    private final PluginDeploymentTaskMapper deploymentTaskMapper;
    private final OssService ossService;
    private final ObjectMapper objectMapper;

    @Value("${upload.base-path:./uploads}")
    private String uploadBasePath;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public PluginLifecycleServiceImpl(
            PluginService pluginService,
            TenantService tenantService,
            TenantPluginService tenantPluginService,
            AccountWalletService accountWalletService,
            AuditLogService auditLogService,
            PluginDeploymentTaskMapper deploymentTaskMapper,
            OssService ossService,
            ObjectMapper objectMapper) {
        this.pluginService = pluginService;
        this.tenantService = tenantService;
        this.tenantPluginService = tenantPluginService;
        this.accountWalletService = accountWalletService;
        this.auditLogService = auditLogService;
        this.deploymentTaskMapper = deploymentTaskMapper;
        this.ossService = ossService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadPluginResponse uploadPlugin(MultipartFile file, boolean overrideExisting, Long operatorId) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "只支持 .zip 文件");
        }

        log.info("[UploadPlugin] filename={}, size={}, override={}, operator={}",
                originalFilename, file.getSize(), overrideExisting, operatorId);

        // 解压到临时目录
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("plugin_upload_");
        } catch (IOException e) {
            log.error("[UploadPlugin] Failed to create temp dir", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "无法创建临时目录: " + e.getMessage());
        }

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = tempDir.resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(tempDir)) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "压缩包内路径遍历异常: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "解压失败: " + e.getMessage());
        }

        // 读取并校验 manifest.json
        Path manifestPath = tempDir.resolve("manifest.json");
        if (!Files.exists(manifestPath)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "压缩包根目录缺少 manifest.json");
        }

        PluginManifest manifest;
        try {
            String manifestJson = Files.readString(manifestPath, StandardCharsets.UTF_8);
            manifest = objectMapper.readValue(manifestJson, PluginManifest.class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "manifest.json 格式错误: " + e.getMessage());
        }

        if (manifest.pluginId() == null || manifest.pluginId().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "manifest.json 中 plugin_id 不能为空");
        }
        if (manifest.name() == null || manifest.name().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "manifest.json 中 name 不能为空");
        }
        if (manifest.billingType() == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "manifest.json 中 billing_type 不能为空");
        }
        if ("token".equals(manifest.billingType()) &&
                (manifest.defaultTokenCost() == null || manifest.defaultTokenCost() <= 0)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "billing_type=token 时 default_token_cost 必须为正整数");
        }

        // 检查 plugin_id 是否重复
        Plugin existing = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, manifest.pluginId())
                .one();
        if (existing != null && !overrideExisting) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "插件 " + manifest.pluginId() + " 已存在，请确认是否覆盖");
        }

        // 校验前端入口文件
        if (manifest.frontendEntry() != null && !manifest.frontendEntry().isBlank()) {
            String entryPath = manifest.frontendEntry()
                    .replaceFirst("^/", "")
                    .replaceFirst("^https?://[^/]+/", "");
            Path entryFile = tempDir.resolve(entryPath);
            if (!Files.exists(entryFile) && !manifest.frontendEntry().startsWith("http")) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "前端入口文件不存在: " + manifest.frontendEntry());
            }
        }

        // 复制前端资源到插件目录
        String resourcePath = "plugins/" + manifest.pluginId() + "/" + manifest.version() + "/";
        copyDirectoryToOss(tempDir, resourcePath);

        // 构建 frontend_path（资源发布的根路径）
        String frontendPath = appBaseUrl + "/uploads/" + resourcePath;

        // 保存/更新 plugin 记录
        Plugin plugin;
        if (existing != null) {
            plugin = existing;
        } else {
            plugin = Plugin.builder()
                    .pluginId(manifest.pluginId())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        plugin.setName(manifest.name() != null ? manifest.name() : "");
        plugin.setVersion(manifest.version() != null ? manifest.version() : "1.0.0");
        plugin.setDescription(manifest.description());
        plugin.setIconUrl(manifest.iconUrl());
        plugin.setBillingType(manifest.billingType());
        plugin.setDefaultTokenCost(manifest.defaultTokenCost() != null ? manifest.defaultTokenCost() : 0);
        plugin.setFrontendEntry(manifest.frontendEntry());
        plugin.setBackendApi(manifest.backendApi());
        plugin.setFrontendPath(frontendPath);
        plugin.setLifecycleStatus("testing");
        plugin.setReviewStatus("pending");
        plugin.setCreatedBy(operatorId);

        pluginService.saveOrUpdate(plugin);

        // 如果 manifest 中有后端部署配置，自动创建部署任务
        if (manifest.backendDockerImage() != null && !manifest.backendDockerImage().isBlank()) {
            createDeploymentTaskInternal(plugin.getPluginId(), manifest.backendDockerImage(), "{}");
        }

        // 清理临时目录
        deleteRecursively(tempDir);

        // 审计日志
        saveAuditLog(operatorId, "plugin_upload", "plugin", manifest.pluginId(),
                objectMapper.createObjectNode()
                        .put("name", manifest.name())
                        .put("version", manifest.version())
                        .toString());

        return new UploadPluginResponse(
                plugin.getPluginId(),
                plugin.getName(),
                plugin.getVersion(),
                frontendPath,
                plugin.getLifecycleStatus()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SandboxTestResponse createSandboxTest(String pluginId, Long operatorId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "插件不存在");
        }
        if ("disabled".equals(plugin.getLifecycleStatus())) {
            throw new AppException(ErrorCode.FORBIDDEN, "插件已下架，无法测试");
        }

        // 创建测试租户
        String testTenantName = "Sandbox_" + pluginId + "_" + System.currentTimeMillis();
        Tenant testTenant = Tenant.builder()
                .tenantCode("sandbox_" + pluginId + "_" + System.currentTimeMillis())
                .name(testTenantName)
                .status(1)
                .level("basic")
                .createdAt(LocalDateTime.now())
                .build();
        tenantService.save(testTenant);

        // 为测试租户开通该插件
        TenantPlugin tenantPlugin = TenantPlugin.builder()
                .tenantId(testTenant.getId())
                .pluginId(pluginId)
                .enabled(1)
                .configJson("{}")
                .createdAt(LocalDateTime.now())
                .build();
        tenantPluginService.save(tenantPlugin);

        // 为测试租户充值 Token
        AccountWallet wallet = AccountWallet.builder()
                .tenantId(testTenant.getId())
                .tokenBalance(10000L)
                .cashBalance(java.math.BigDecimal.ZERO)
                .frozenToken(0L)
                .status(1)
                .build();
        accountWalletService.save(wallet);

        // 更新插件测试时间
        plugin.setTestedAt(LocalDateTime.now());
        pluginService.updateById(plugin);

        // 构建沙箱 URL
        String sandboxUrl = plugin.getFrontendPath();
        if (sandboxUrl == null || sandboxUrl.isBlank()) {
            sandboxUrl = appBaseUrl + "/uploads/plugins/" + pluginId + "/" + (plugin.getVersion() != null ? plugin.getVersion() : "1.0.0") + "/";
        }
        sandboxUrl = sandboxUrl + "?" + String.format("sandbox=true&tenant_id=%d&plugin_id=%s", testTenant.getId(), pluginId);

        saveAuditLog(operatorId, "plugin_sandbox_test", "plugin", pluginId,
                objectMapper.createObjectNode()
                        .put("test_tenant_id", testTenant.getId())
                        .put("test_tenant_name", testTenantName)
                        .toString());

        return new SandboxTestResponse(sandboxUrl, testTenant.getId(), testTenantName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPlugin(String pluginId, Long operatorId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "插件不存在");
        }

        plugin.setLifecycleStatus("enabled");
        plugin.setReviewStatus("pass");
        plugin.setPublishedAt(LocalDateTime.now());
        pluginService.updateById(plugin);

        saveAuditLog(operatorId, "plugin_publish", "plugin", pluginId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlinePlugin(String pluginId, Long operatorId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "插件不存在");
        }

        plugin.setLifecycleStatus("disabled");
        pluginService.updateById(plugin);

        saveAuditLog(operatorId, "plugin_offline", "plugin", pluginId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrayPublishResponse grayPublish(String pluginId, List<Long> grayTenantIds, Long operatorId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "插件不存在");
        }

        plugin.setLifecycleStatus("gray");
        plugin.setGrayTenantIds(String.join(",", grayTenantIds.stream().map(String::valueOf).toList()));
        pluginService.updateById(plugin);

        saveAuditLog(operatorId, "plugin_gray_publish", "plugin", pluginId,
                objectMapper.createObjectNode()
                        .put("gray_tenant_count", grayTenantIds.size())
                        .toString());

        return new GrayPublishResponse(pluginId, grayTenantIds.size(), "gray");
    }

    @Override
    public PluginStatusResponse getPluginStatus(String pluginId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "插件不存在");
        }

        String deploymentStatus = "not_deployed";
        if (plugin.getBackendDeployConfig() != null && !plugin.getBackendDeployConfig().isBlank()) {
            deploymentStatus = "configured";
        }

        int grayTenantCount = 0;
        if (plugin.getGrayTenantIds() != null && !plugin.getGrayTenantIds().isBlank()) {
            grayTenantCount = plugin.getGrayTenantIds().split(",").length;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new PluginStatusResponse(
                plugin.getPluginId(),
                plugin.getLifecycleStatus(),
                grayTenantCount,
                plugin.getTestedAt() != null ? plugin.getTestedAt().format(formatter) : null,
                plugin.getPublishedAt() != null ? plugin.getPublishedAt().format(formatter) : null,
                deploymentStatus
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeploymentTaskResponse deployBackend(String pluginId, DeployPluginRequest request, Long operatorId) {
        Plugin plugin = pluginService.lambdaQuery()
                .eq(Plugin::getPluginId, pluginId)
                .one();
        if (plugin == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "插件不存在");
        }

        String envVarsJson = "{}";
        if (request.envVars() != null) {
            try {
                envVarsJson = objectMapper.writeValueAsString(request.envVars());
            } catch (Exception e) {
                log.warn("Failed to serialize env_vars: {}", e.getMessage());
            }
        }
        PluginDeploymentTask task = createDeploymentTaskInternal(
                pluginId,
                request.dockerImage(),
                envVarsJson
        );

        // 异步执行 Mock 部署
        final Long taskId = task.getId();
        CompletableFuture.runAsync(() -> mockDeployBackend(taskId, pluginId, request.dockerImage()));

        saveAuditLog(operatorId, "plugin_deploy", "plugin", pluginId,
                objectMapper.createObjectNode()
                        .put("task_id", taskId)
                        .put("docker_image", request.dockerImage())
                        .toString());

        return new DeploymentTaskResponse(taskId, "pending", null);
    }

    // ---- Internal helpers ----

    private PluginDeploymentTask createDeploymentTaskInternal(String pluginId, String dockerImage, String envVars) {
        PluginDeploymentTask task = PluginDeploymentTask.builder()
                .pluginId(pluginId)
                .dockerImage(dockerImage)
                .envVars(envVars)
                .status("pending")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        deploymentTaskMapper.insert(task);
        return task;
    }

    @Async
    void mockDeployBackend(Long taskId, String pluginId, String dockerImage) {
        try {
            PluginDeploymentTask task = deploymentTaskMapper.selectById(taskId);
            task.setStatus("running");
            task.setUpdatedAt(LocalDateTime.now());
            deploymentTaskMapper.updateById(task);

            log.info("[Deploy Mock] Starting deployment for plugin: {}, image: {}", pluginId, dockerImage);

            String[] steps = {
                    "Pulling Docker image...",
                    "Building container...",
                    "Running health check...",
                    "Configuring endpoints...",
                    "Deployment verified successfully!"
            };

            for (int i = 0; i < steps.length; i++) {
                Thread.sleep(2000);
                log.info("[Deploy Mock] Step {}/{}: {}", i + 1, steps.length, steps[i]);
            }

            task.setStatus("success");
            task.setUpdatedAt(LocalDateTime.now());
            deploymentTaskMapper.updateById(task);

            log.info("[Deploy Mock] Deployment completed for plugin: {}", pluginId);

            // 更新 plugin 的后端部署配置
            Plugin plugin = pluginService.lambdaQuery()
                    .eq(Plugin::getPluginId, pluginId)
                    .one();
            if (plugin != null) {
                plugin.setBackendDeployConfig(objectMapper.createObjectNode()
                        .put("docker_image", dockerImage)
                        .put("deployed_at", java.time.LocalDateTime.now().toString())
                        .toString());
                pluginService.updateById(plugin);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PluginDeploymentTask task = deploymentTaskMapper.selectById(taskId);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            deploymentTaskMapper.updateById(task);
        }
    }

    private void copyDirectoryToOss(Path sourceDir, String destPrefix) {
        log.info("[UploadPlugin] Copying directory to OSS: {} -> {}", sourceDir, destPrefix);
        try {
            Files.walk(sourceDir).forEach(source -> {
                Path relativePath = sourceDir.relativize(source);
                String relativePathStr = relativePath.toString().replace("\\", "/");
                if (Files.isDirectory(source)) {
                    return;
                }
                String objectKey = destPrefix + relativePathStr;
                try {
                    byte[] bytes = Files.readAllBytes(source);
                    String fileUrl = ossService.uploadBytes(bytes, objectKey);
                    log.info("[UploadPlugin] Uploaded file to OSS: {} -> {}", objectKey, fileUrl);
                } catch (IOException e) {
                    log.error("[UploadPlugin] Failed to upload file to OSS: {}, error: {}", objectKey, e.getMessage());
                    throw new RuntimeException("Failed to upload file: " + objectKey, e);
                }
            });
        } catch (IOException e) {
            log.error("[UploadPlugin] Failed to copy directory to OSS: {}, error: {}", sourceDir, e.getMessage());
            throw new RuntimeException("Failed to copy directory to OSS: " + sourceDir, e);
        }
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {}
                            });
                }
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("Failed to delete temp directory: {}", path, e);
        }
    }

    private void saveAuditLog(Long operatorId, String action, String targetType, String targetId, String detailJson) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .operatorId(operatorId)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detailJson(detailJson)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogService.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: action={}, targetType={}, targetId={}",
                    action, targetType, targetId, e);
        }
    }

    @Override
    public Map<String, Object> executeDeployTask(Long taskId, Long operatorId) {
        PluginDeploymentTask task = deploymentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "部署任务不存在");
        }

        log.info("[ExecuteDeploy] taskId={}, pluginId={}, dockerImage={}",
                taskId, task.getPluginId(), task.getDockerImage());

        // 更新状态为 running
        task.setStatus("running");
        task.setUpdatedAt(LocalDateTime.now());
        deploymentTaskMapper.updateById(task);

        // 异步执行部署
        final Long tId = taskId;
        final String pId = task.getPluginId();
        final String img = task.getDockerImage() != null ? task.getDockerImage() : "";
        CompletableFuture.runAsync(() -> mockDeployBackend(tId, pId, img));

        // 审计日志
        saveAuditLog(operatorId, "plugin_deploy_execute", "plugin_deployment_task", String.valueOf(taskId),
                objectMapper.createObjectNode()
                        .put("plugin_id", task.getPluginId())
                        .put("docker_image", task.getDockerImage())
                        .toString());

        Map<String, Object> result = new HashMap<>();
        result.put("task_id", taskId);
        result.put("status", "running");
        return result;
    }

    @Override
    public void scanPendingDeployTasks() {
        log.info("[ScanPendingDeploy] Starting scan for pending deployment tasks...");
        List<PluginDeploymentTask> pendingTasks = deploymentTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PluginDeploymentTask>()
                        .eq("status", "pending")
        );

        for (PluginDeploymentTask task : pendingTasks) {
            log.info("[ScanPendingDeploy] Found pending task: id={}, pluginId={}",
                    task.getId(), task.getPluginId());
            final Long taskId = task.getId();
            final String pluginId = task.getPluginId();
            final String dockerImage = task.getDockerImage() != null ? task.getDockerImage() : "";
            CompletableFuture.runAsync(() -> mockDeployBackend(taskId, pluginId, dockerImage));
        }

        log.info("[ScanPendingDeploy] Scan complete, processed {} tasks", pendingTasks.size());
    }
}