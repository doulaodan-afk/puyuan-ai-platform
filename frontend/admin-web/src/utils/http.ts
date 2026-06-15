import { useAdminAuthStore } from "../stores/adminAuth";
import { generateUUID } from "./uuid";

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export function buildAdminHeaders(extra?: Record<string, string>): HeadersInit {
  const auth = useAdminAuthStore();
  const headers: Record<string, string> = {
    "X-Request-Id": generateUUID(),
    ...(extra ?? {}),
  };

  if (auth.accessToken) {
    headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  if (auth.tenantId) {
    headers["X-Tenant-Id"] = auth.tenantId;
  }

  return headers;
}

function getApiBaseUrl(): string {
  const baseUrl = import.meta.env.VITE_API_BASE_URL;
  if (baseUrl) {
    return baseUrl;
  }
  // 开发环境使用相对路径（通过 vite proxy）
  // 生产环境使用 VITE_API_BASE_URL 环境变量
  return '';
}

export async function adminRequest<T>(url: string, init?: RequestInit): Promise<T> {
  const baseUrl = getApiBaseUrl();
  const fullUrl = baseUrl ? `${baseUrl}${url}` : url;

  const merged: RequestInit = {
    ...init,
    headers: {
      ...buildAdminHeaders(),
      ...(init?.headers ?? {}),
    },
  };

  const response = await fetch(fullUrl, merged);
  if (!response.ok) {
    // 尝试解析响应体中的错误消息
    try {
      const errorPayload = await response.json() as ApiEnvelope<unknown>;
      throw new Error(errorPayload.message || `HTTP ${response.status}`);
    } catch (parseError) {
      if (parseError instanceof Error && parseError.message !== `HTTP ${response.status}`) {
        throw parseError;
      }
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
  }

  const payload = (await response.json()) as ApiEnvelope<T>;
  if (payload.code !== 0) {
    throw new Error(payload.message || "request failed");
  }
  return payload.data;
}