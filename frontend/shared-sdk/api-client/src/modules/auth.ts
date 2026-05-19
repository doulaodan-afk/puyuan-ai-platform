import type { ApiResponse, RequestContext } from "../core/types";
import { HttpClient } from "../core/httpClient";

export interface LoginRequest {
  mobile: string;
  verify_code: string;
}

export interface LoginResponse {
  access_token: string;
  expires_in: number;
  user_id: number;
  tenant_id: number;
  role_code: string;
}

export interface ProfileResponse {
  tenant_id: number;
  tenant_code: string;
  tenant_name: string;
  tenant_status: number;
  user_id: number;
  role_code: string;
}

export class AuthApi {
  constructor(private readonly http: HttpClient) {}

  login(payload: LoginRequest, context: RequestContext) {
    return this.http.request<ApiResponse<LoginResponse>>("POST", "/api/v1/auth/login", context, payload);
  }

  profile(context: RequestContext) {
    return this.http.request<ApiResponse<ProfileResponse>>("GET", "/api/v1/tenant/profile", context);
  }
}
