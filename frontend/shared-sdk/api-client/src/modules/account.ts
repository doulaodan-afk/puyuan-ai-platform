import type { ApiResponse, RequestContext } from "../core/types";
import { HttpClient } from "../core/httpClient";

export interface BalanceResponse {
  token_balance: number;
  storage_used_gb: number;
  storage_free_quota_gb: number;
  storage_extra_gb: number;
  expire_date: string;
}

export interface RechargeOrderRequest {
  amount: number;
  pay_channel: string;
}

export class AccountApi {
  constructor(private readonly http: HttpClient) {}

  balance(context: RequestContext) {
    return this.http.request<ApiResponse<BalanceResponse>>("GET", "/api/v1/account/balance", context);
  }

  createRechargeOrder(payload: RechargeOrderRequest, context: RequestContext) {
    return this.http.request("POST", "/api/v1/account/recharge/orders", context, payload);
  }

  ledger(context: RequestContext, page = 1, pageSize = 20) {
    return this.http.request(
      "GET",
      `/api/v1/account/ledger?page=${page}&page_size=${pageSize}`,
      context,
    );
  }
}
