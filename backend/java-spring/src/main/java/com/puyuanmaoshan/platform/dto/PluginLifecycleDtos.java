package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Plugin Lifecycle Management DTOs
 */
public final class PluginLifecycleDtos {

    private PluginLifecycleDtos() {}

    // ---- Upload ----

    public record UploadPluginResponse(
            @JsonProperty("plugin_id") String pluginId,
            String name,
            String version,
            @JsonProperty("frontend_path") String frontendPath,
            @JsonProperty("lifecycle_status") String lifecycleStatus
    ) {}

    // ---- Sandbox Test ----

    public record SandboxTestResponse(
            @JsonProperty("sandbox_url") String sandboxUrl,
            @JsonProperty("test_tenant_id") long testTenantId,
            @JsonProperty("test_tenant_name") String testTenantName
    ) {}

    // ---- Gray Publish ----

    public record GrayPublishRequest(
            @NotNull @JsonProperty("gray_tenant_ids") List<Long> grayTenantIds
    ) {}

    public record GrayPublishResponse(
            @JsonProperty("plugin_id") String pluginId,
            @JsonProperty("gray_tenant_count") int grayTenantCount,
            @JsonProperty("lifecycle_status") String lifecycleStatus
    ) {}

    // ---- Status Query ----

    public record PluginStatusResponse(
            @JsonProperty("plugin_id") String pluginId,
            @JsonProperty("lifecycle_status") String lifecycleStatus,
            @JsonProperty("gray_tenant_count") int grayTenantCount,
            @JsonProperty("tested_at") String testedAt,
            @JsonProperty("published_at") String publishedAt,
            @JsonProperty("deployment_status") String deploymentStatus
    ) {}

    // ---- Backend Deploy ----

    public record DeployPluginRequest(
            @JsonProperty("docker_image") String dockerImage,
            @JsonProperty("env_vars") java.util.Map<String, String> envVars
    ) {}

    public record DeploymentTaskResponse(
            @JsonProperty("task_id") long taskId,
            String status,
            @JsonProperty("error_message") String errorMessage
    ) {}

    // ---- manifest.json internal structure ----

    public record PluginManifest(
            @JsonProperty("plugin_id") String pluginId,
            String name,
            String version,
            String description,
            @JsonProperty("icon_url") String iconUrl,
            @JsonProperty("billing_type") String billingType,
            @JsonProperty("default_token_cost") Integer defaultTokenCost,
            @JsonProperty("frontend_entry") String frontendEntry,
            @JsonProperty("backend_api") String backendApi,
            @JsonProperty("need_config") Boolean needConfig,
            @JsonProperty("visible_to") List<String> visibleTo,
            @JsonProperty("required_role") String requiredRole,
            @JsonProperty("backend_docker_image") String backendDockerImage
    ) {}

    // ---- Plugin List Item (enhanced) ----

    public record EnhancedPluginItem(
            @JsonProperty("plugin_id") String pluginId,
            String name,
            String version,
            @JsonProperty("billing_type") String billingType,
            @JsonProperty("default_token_cost") Integer defaultTokenCost,
            @JsonProperty("lifecycle_status") String lifecycleStatus,
            @JsonProperty("review_status") String reviewStatus,
            @JsonProperty("backend_api") String backendApi,
            @JsonProperty("frontend_path") String frontendPath,
            @JsonProperty("description") String description,
            @JsonProperty("gray_tenant_count") int grayTenantCount,
            String createdAt
    ) {}
}