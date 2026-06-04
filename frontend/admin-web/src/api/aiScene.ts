import { adminRequest } from "../utils/http";

// ==================== AI 提供商管理 ====================

export interface AiProvider {
  id: number;
  name: string;
  display_name: string;
  base_url: string;
  api_key: string; // 脱敏
  enabled: boolean;
  priority: number;
  description: string | null;
  has_api_key: boolean;
  created_at: string;
  updated_at: string;
}

export interface AiProviderRequest {
  id?: number;
  name: string;
  display_name: string;
  base_url: string;
  api_key?: string;
  enabled?: boolean;
  priority?: number;
  description?: string;
}

export async function listProviders(): Promise<AiProvider[]> {
  return adminRequest<AiProvider[]>("/api/v1/admin/ai-scene/providers");
}

export async function getProvider(id: number): Promise<AiProvider> {
  return adminRequest<AiProvider>(`/api/v1/admin/ai-scene/providers/${id}`);
}

export async function createProvider(request: AiProviderRequest): Promise<AiProvider> {
  return adminRequest<AiProvider>("/api/v1/admin/ai-scene/providers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function updateProvider(id: number, request: AiProviderRequest): Promise<AiProvider> {
  return adminRequest<AiProvider>(`/api/v1/admin/ai-scene/providers/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function deleteProvider(id: number): Promise<Record<string, unknown>> {
  return adminRequest<Record<string, unknown>>(`/api/v1/admin/ai-scene/providers/${id}`, {
    method: "DELETE",
  });
}

export interface TestModelResponse {
  success: boolean;
  message: string;
  result?: string;
  latency_ms: number;
}

export async function testProvider(id: number, modelId?: string, prompt?: string): Promise<TestModelResponse> {
  return adminRequest<TestModelResponse>(`/api/v1/admin/ai-scene/providers/${id}/test`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ model_id: modelId || "default", prompt }),
  });
}

// ==================== 提供商模型列表 ====================

export interface ProviderModelEntry {
  model_id: string;
  provider_name: string;
  provider_display_name: string;
}

export async function listProviderModels(providerId: number): Promise<ProviderModelEntry[]> {
  return adminRequest<ProviderModelEntry[]>(`/api/v1/admin/ai-scene/providers/${providerId}/models`);
}

// ==================== 场景模型绑定 ====================

export interface SceneModelBinding {
  id: number;
  scene_id: number;
  scene_code: string;
  scene_name: string;
  provider_id: number;
  provider_name: string;
  provider_display_name: string;
  model_id: string;
  is_primary: boolean;
  is_fallback: boolean;
  priority: number;
}

export interface SceneOverview {
  scene_id: number;
  scene_code: string;
  scene_name: string;
  api_type: string;
  scene_description: string;
  enabled: boolean;
  models: SceneModelBinding[];
}

export interface SceneModelBindingRequest {
  scene_id?: number;
  provider_id: number;
  model_id: string;
  is_primary?: boolean;
  is_fallback?: boolean;
  priority?: number;
}

export async function listScenes(): Promise<SceneOverview[]> {
  return adminRequest<SceneOverview[]>("/api/v1/admin/ai-scene/scenes");
}

export async function getScene(sceneCode: string): Promise<SceneOverview> {
  return adminRequest<SceneOverview>(`/api/v1/admin/ai-scene/scenes/${sceneCode}`);
}

export async function bindModel(sceneCode: string, request: SceneModelBindingRequest): Promise<SceneModelBinding> {
  return adminRequest<SceneModelBinding>(`/api/v1/admin/ai-scene/scenes/${sceneCode}/bind`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function updateBinding(bindingId: number, request: SceneModelBindingRequest): Promise<SceneModelBinding> {
  return adminRequest<SceneModelBinding>(`/api/v1/admin/ai-scene/scenes/bindings/${bindingId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function unbindModel(bindingId: number): Promise<Record<string, unknown>> {
  return adminRequest<Record<string, unknown>>(`/api/v1/admin/ai-scene/scenes/bindings/${bindingId}`, {
    method: "DELETE",
  });
}

export async function setPrimary(sceneCode: string, bindingId: number): Promise<SceneModelBinding> {
  return adminRequest<SceneModelBinding>(`/api/v1/admin/ai-scene/scenes/${sceneCode}/set-primary/${bindingId}`, {
    method: "PUT",
  });
}

export async function setFallback(sceneCode: string, bindingId: number): Promise<SceneModelBinding> {
  return adminRequest<SceneModelBinding>(`/api/v1/admin/ai-scene/scenes/${sceneCode}/set-fallback/${bindingId}`, {
    method: "PUT",
  });
}

// ==================== AI 推荐模型 ====================

export interface RecommendedModel {
  model_id: string;
  provider_id: number;
  provider_name: string;
  reason: string;
}

export interface RecommendResponse {
  recommended_models: RecommendedModel[];
}

export async function recommendModels(sceneCode: string, description?: string): Promise<RecommendResponse> {
  return adminRequest<RecommendResponse>(`/api/v1/admin/ai-scene/scenes/${sceneCode}/recommend`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(description ? { description } : {}),
  });
}

// ==================== 测试模型调用 ====================

export interface TestModelRequest {
  provider_id: number;
  model_id: string;
  prompt?: string;
}

export async function testSceneModel(sceneCode: string, request: TestModelRequest): Promise<TestModelResponse> {
  return adminRequest<TestModelResponse>(`/api/v1/admin/ai-scene/scenes/${sceneCode}/test`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}
