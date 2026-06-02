import { merchantRequest } from "../utils/http";

export interface InvokePluginRequest {
  prompt?: string;
  image_size?: string;
  product_desc?: string;
  product_url?: string;
  script_type?: string;
  text?: string;
  target_lang?: string;
}

export interface InvokePluginResponse {
  image_url?: string;
  image_size?: string;
  script?: string;
  script_type?: string;
  translated_text?: string;
  target_lang?: string;
  source_lang?: string;
  token_used: number;
  balance_remaining: number;
}

export function invokePlugin(pluginCode: string, params: InvokePluginRequest) {
  return merchantRequest<InvokePluginResponse>(`/api/plugin/invoke/${pluginCode}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(params),
  });
}

export interface GetBalanceResponse {
  token_balance: number;
  storage_used_gb: number;
  storage_free_quota_gb: number;
  storage_extra_gb: number;
  expire_date?: string;
}

export function getBalance() {
  return merchantRequest<GetBalanceResponse>("/api/v1/account/balance", {
    method: "GET",
  });
}