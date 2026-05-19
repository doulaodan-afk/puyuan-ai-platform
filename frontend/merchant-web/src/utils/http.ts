import { useAuthStore } from "../stores/auth";

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export function buildMerchantHeaders(extra?: Record<string, string>): HeadersInit {
  const auth = useAuthStore();
  const headers: Record<string, string> = {
    "X-Request-Id": crypto.randomUUID(),
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

export async function merchantRequest<T>(url: string, init?: RequestInit): Promise<T> {
  const merged: RequestInit = {
    ...init,
    headers: {
      ...buildMerchantHeaders(),
      ...(init?.headers ?? {}),
    },
  };

  const response = await fetch(url, merged);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const payload = (await response.json()) as ApiEnvelope<T>;
  if (payload.code !== 0) {
    throw new Error(payload.message || "request failed");
  }
  return payload.data;
}