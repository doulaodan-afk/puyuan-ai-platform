import { adminRequest } from "../utils/http";

// ========== Types ==========

export interface PluginListItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  default_token_cost: number;
  description?: string;
  icon_url?: string;
  backend_api?: string;
  frontend_path?: string;
  lifecycle_status: "testing" | "enabled" | "disabled" | "gray";
  review_status: string;
  gray_tenant_count: number;
  created_at?: string;
  updated_at?: string;
}

export interface UploadPluginResponse {
  plugin_id: string;
  name: string;
  version: string;
  frontend_path: string;
  lifecycle_status: string;
}

export interface SandboxTestResponse {
  sandbox_url: string;
  test_tenant_id: number;
  test_tenant_name: string;
}

export interface PluginStatusResponse {
  plugin_id: string;
  lifecycle_status: string;
  gray_tenant_count: number;
  tested_at: string | null;
  published_at: string | null;
  deployment_status: string;
}

export interface DeploymentTaskResponse {
  task_id: number;
  status: string;
  error_message: string | null;
}

export interface TenantItem {
  tenant_id: number;
  tenant_name: string;
  tenant_code: string;
}

// ========== API Methods ==========

export async function uploadPlugin(file: File, overrideExisting: boolean): Promise<UploadPluginResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("override_existing", String(overrideExisting));
  return adminRequest<UploadPluginResponse>("/api/v1/admin/plugins/upload", {
    method: "POST",
    body: formData,
  });
}

export async function sandboxTest(pluginId: string): Promise<SandboxTestResponse> {
  return adminRequest<SandboxTestResponse>(`/api/v1/admin/plugins/${pluginId}/test`, {
    method: "POST",
  });
}

export async function publishFull(pluginId: string): Promise<void> {
  return adminRequest<void>(`/api/v1/admin/plugins/${pluginId}/publish-full`, {
    method: "POST",
  });
}

export async function offlinePlugin(pluginId: string): Promise<void> {
  return adminRequest<void>(`/api/v1/admin/plugins/${pluginId}/offline`, {
    method: "POST",
  });
}

export async function grayPublish(pluginId: string, tenantIds: number[]): Promise<void> {
  return adminRequest<void>(`/api/v1/admin/plugins/${pluginId}/gray`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ gray_tenant_ids: tenantIds }),
  });
}

export async function getPluginStatus(pluginId: string): Promise<PluginStatusResponse> {
  return adminRequest<PluginStatusResponse>(`/api/v1/admin/plugins/${pluginId}/status`);
}

export async function deployPlugin(
  pluginId: string,
  dockerImage: string,
  envVars: Record<string, string>
): Promise<DeploymentTaskResponse> {
  return adminRequest<DeploymentTaskResponse>(`/api/v1/admin/plugins/${pluginId}/deploy`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ docker_image: dockerImage, env_vars: envVars }),
  });
}

export async function listTenants(
  page?: number,
  pageSize?: number,
  keyword?: string
): Promise<{ list: TenantItem[]; page: number; page_size: number; total: number }> {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("page_size", String(pageSize));
  if (keyword) params.set("keyword", keyword);
  return adminRequest<{ list: TenantItem[]; page: number; page_size: number; total: number }>(
    `/api/v1/admin/plugins/tenants?${params.toString()}`
  );
}