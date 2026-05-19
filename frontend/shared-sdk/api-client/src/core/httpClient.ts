import type { RequestContext } from "./types";

export class HttpClient {
  constructor(private readonly baseUrl: string) {}

  async request<T>(
    method: "GET" | "POST" | "PUT" | "PATCH",
    path: string,
    context: RequestContext,
    body?: unknown,
  ): Promise<T> {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Request-Id": context.requestId ?? crypto.randomUUID(),
    };

    // # MEMORY: tenant-id and idempotency-key are attached centrally to avoid endpoint-level omissions that could break billing consistency.
    if (context.tenantId) headers["X-Tenant-Id"] = context.tenantId;
    if (context.idempotencyKey) headers["Idempotency-Key"] = context.idempotencyKey;
    if (context.accessToken) headers.Authorization = `Bearer ${context.accessToken}`;

    const response = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    return (await response.json()) as T;
  }
}
