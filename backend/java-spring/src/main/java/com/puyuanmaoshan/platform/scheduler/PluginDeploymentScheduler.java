package com.puyuanmaoshan.platform.scheduler;

import com.puyuanmaoshan.platform.service.PluginLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PluginDeploymentScheduler {
    private static final Logger log = LoggerFactory.getLogger(PluginDeploymentScheduler.class);

    private final PluginLifecycleService pluginLifecycleService;

    public PluginDeploymentScheduler(PluginLifecycleService pluginLifecycleService) {
        this.pluginLifecycleService = pluginLifecycleService;
    }

    /**
     * 每 5 分钟扫描一次待部署任务并自动执行
     */
    @Scheduled(fixedRate = 300000)
    public void scanAndExecutePendingDeployTasks() {
        log.info("[Scheduler] Triggering pending deployment task scan...");
        try {
            pluginLifecycleService.scanPendingDeployTasks();
        } catch (Exception e) {
            log.error("[Scheduler] Error scanning pending deployment tasks", e);
        }
    }
}