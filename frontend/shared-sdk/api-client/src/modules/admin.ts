import { HttpClient } from "../core/httpClient";
import type { RequestContext } from "../core/types";

export class AdminApi {
  constructor(private readonly http: HttpClient) {}

  listTenants(context: RequestContext, page = 1, pageSize = 20) {
    return this.http.request("GET", `/api/v1/admin/tenants?page=${page}&page_size=${pageSize}`, context);
  }

  freezeTenant(tenantId: number, context: RequestContext) {
    return this.http.request("POST", `/api/v1/admin/tenants/${tenantId}/freeze`, context);
  }

  createPlugin(payload: Record<string, unknown>, context: RequestContext) {
    return this.http.request("POST", "/api/v1/admin/plugins", context, payload);
  }

  updatePricing(payload: Record<string, unknown>, context: RequestContext) {
    return this.http.request("PUT", "/api/v1/admin/pricing", context, payload);
  }
}
