import type { ApiResponse, RequestContext } from "../core/types";
import { HttpClient } from "../core/httpClient";

export interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: "token" | "times" | "free";
  enabled: boolean;
}

export class PluginsApi {
  constructor(private readonly http: HttpClient) {}

  list(context: RequestContext) {
    return this.http.request<ApiResponse<PluginItem[]>>("GET", "/api/v1/plugins", context);
  }

  enable(pluginId: string, context: RequestContext) {
    return this.http.request("POST", `/api/v1/plugins/${pluginId}/enable`, context);
  }

  disable(pluginId: string, context: RequestContext) {
    return this.http.request("POST", `/api/v1/plugins/${pluginId}/disable`, context);
  }

  invoke(pluginId: string, payload: Record<string, unknown>, context: RequestContext) {
    return this.http.request("POST", `/api/v1/plugins/${pluginId}/invoke`, context, payload);
  }
}
