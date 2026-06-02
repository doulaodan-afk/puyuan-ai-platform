import { adminRequest } from "../utils/http";

// 系统配置相关接口

export interface ConfigGroup {
  value: string;
  label: string;
}

export interface SystemConfig {
  id: number;
  config_group: string;
  config_key: string;
  config_value: string;
  enabled: boolean;
  sort_order: number;
  description: string | null;
  created_at: string;
  updated_at: string;
}

export interface AiProviderConfig {
  provider_name: string;
  model_name: string;
  api_key: string;
  endpoint: string;
  priority: number;
  enabled: boolean;
  config_id: number | null;
}

export interface OssConfig {
  provider_name: string;
  access_key_id: string;
  access_key_secret: string;
  endpoint: string;
  bucket_name: string;
  region: string;
  priority: number;
  enabled: boolean;
  config_id: number | null;
}

export interface SaveConfigRequest {
  id?: number;
  config_group: string;
  config_key: string;
  config_value: string;
  enabled: boolean;
  sort_order: number;
  description: string | null;
}

export interface TestConfigRequest {
  id: number;
}

export interface TestConfigResponse {
  success: boolean;
  message: string;
  latency: number | null;
}

/**
 * 获取所有配置分组
 */
export async function getConfigGroups(): Promise<ConfigGroup[]> {
  return adminRequest<ConfigGroup[]>("/api/v1/admin/system-config/groups");
}

/**
 * 获取指定分组的所有配置
 */
export async function getConfigs(group: string): Promise<SystemConfig[]> {
  return adminRequest<SystemConfig[]>(`/api/v1/admin/system-config/list?group=${group}`);
}

/**
 * 获取 AI 配置列表
 */
export async function getAiConfigs(group: string): Promise<AiProviderConfig[]> {
  return adminRequest<AiProviderConfig[]>(`/api/v1/admin/system-config/ai?group=${group}`);
}

/**
 * 获取 OSS 配置列表
 */
export async function getOssConfigs(): Promise<OssConfig[]> {
  return adminRequest<OssConfig[]>("/api/v1/admin/system-config/oss");
}

/**
 * 保存或更新配置
 */
export async function saveConfig(request: SaveConfigRequest): Promise<SystemConfig> {
  return adminRequest<SystemConfig>("/api/v1/admin/system-config/save", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/**
 * 保存 AI 提供商配置
 */
export async function saveAiConfig(request: Record<string, unknown>): Promise<Record<string, unknown>> {
  return adminRequest<Record<string, unknown>>("/api/v1/admin/system-config/ai/save", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/**
 * 删除配置
 */
export async function deleteConfig(id: number): Promise<Record<string, unknown>> {
  return adminRequest<Record<string, unknown>>(`/api/v1/admin/system-config/${id}`, {
    method: "DELETE",
  });
}

/**
 * 测试配置
 */
export async function testConfig(request: TestConfigRequest): Promise<TestConfigResponse> {
  return adminRequest<TestConfigResponse>("/api/v1/admin/system-config/test", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}
