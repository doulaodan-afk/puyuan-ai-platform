export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  request_id: string;
}

export interface RequestContext {
  tenantId?: string;
  requestId?: string;
  idempotencyKey?: string;
  accessToken?: string;
}
