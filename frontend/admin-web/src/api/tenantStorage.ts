import { adminRequest } from "../utils/http";

// ====================== 类型定义 ======================

/** 存储空间 */
export interface TenantBucket {
  id: number;
  tenant_id: number;
  tenant_name: string;
  bucket_name: string;
  bucket_region: string;
  bucket_region_label: string;
  bucket_domain: string;
  bucket_private: boolean;
  status: string;
  notes: string;
  plan_name: string;
  plan_code: string;
  storage_quota_gb: number;
  storage_used_gb: number;
  monthly_traffic_gb: number;
  traffic_used_gb: number;
  created_at: string;
  updated_at: string;
}

/** 存储套餐 */
export interface StoragePlan {
  id: number;
  plan_name: string;
  plan_code: string;
  plan_level: number;
  storage_quota_gb: number;
  max_file_count: number | null;
  max_file_size_mb: number | null;
  monthly_traffic_gb: number;
  monthly_cdn_traffic_gb: number;
  monthly_get_requests: number;
  monthly_put_requests: number;
  base_price: number;
  storage_price_per_gb: number;
  traffic_price_per_gb: number;
  request_price_per_10k: number;
  free_trial_days: number;
  status: boolean;
  sort_order: number;
  description: string;
  features: string[];
  tenant_count: number;
}

/** 计费记录 */
export interface BillingRecord {
  id: number;
  tenant_id: number;
  tenant_name: string;
  tenant_bucket_id: number;
  bucket_name: string;
  bill_period: string;
  standard_storage_gb: number;
  line_storage_gb: number;
  archive_storage_gb: number;
  external_traffic_gb: number;
  cdn_traffic_gb: number;
  get_requests: number;
  put_requests: number;
  quota_storage_gb: number;
  quota_traffic_gb: number;
  base_fee: number;
  storage_overage_fee: number;
  traffic_overage_fee: number;
  request_overage_fee: number;
  total_fee: number;
  bill_status: string;
  calculated_at: string;
  paid_at: string;
}

/** 用量快照 */
export interface UsageSnapshot {
  id: number;
  snapshot_date: string;
  standard_storage_gb: number;
  line_storage_gb: number;
  archive_storage_gb: number;
  standard_file_count: number;
  external_traffic_gb: number;
  cdn_traffic_gb: number;
  get_requests: number;
  put_requests: number;
  fetch_status: string;
}

/** 平台概览 */
export interface PlatformStorageOverview {
  total_buckets: number;
  active_buckets: number;
  total_tenants: number;
  total_storage_used_gb: number;
  total_traffic_gb: number;
  total_revenue: number;
  pending_bills: number;
}

/** 创建Bucket请求 */
export interface CreateBucketRequest {
  tenant_id: number;
  bucket_name: string;
  bucket_region: string;
  bucket_private: boolean;
  plan_id: number;
  notes: string;
}

/** 更新Bucket请求 */
export interface UpdateBucketRequest {
  bucket_domain?: string;
  bucket_private?: boolean;
  status?: string;
  notes?: string;
}

/** 分配套餐请求 */
export interface AssignPlanRequest {
  tenant_id: number;
  tenant_bucket_id: number;
  plan_id: number;
  auto_renew: boolean;
}

/** 计算账单请求 */
export interface CalculateBillRequest {
  tenant_bucket_id: number;
  bill_period: string;
}

// ====================== API 方法 ======================

/** 获取所有存储空间 */
export function getTenantBuckets(): Promise<TenantBucket[]> {
  return adminRequest<TenantBucket[]>("/api/v1/admin/tenant-storage/buckets");
}

/** 获取指定租户的存储空间 */
export function getTenantBucketsByTenant(tenantId: number): Promise<TenantBucket[]> {
  return adminRequest<TenantBucket[]>(`/api/v1/admin/tenant-storage/buckets/tenant/${tenantId}`);
}

/** 获取存储空间详情 */
export function getTenantBucketDetail(bucketId: number): Promise<TenantBucket> {
  return adminRequest<TenantBucket>(`/api/v1/admin/tenant-storage/buckets/${bucketId}`);
}

/** 创建存储空间 */
export function createTenantBucket(request: CreateBucketRequest): Promise<TenantBucket> {
  return adminRequest<TenantBucket>("/api/v1/admin/tenant-storage/buckets", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 更新存储空间 */
export function updateTenantBucket(bucketId: number, request: UpdateBucketRequest): Promise<TenantBucket> {
  return adminRequest<TenantBucket>(`/api/v1/admin/tenant-storage/buckets/${bucketId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 删除存储空间 */
export function deleteTenantBucket(bucketId: number): Promise<{ status: string }> {
  return adminRequest<{ status: string }>(`/api/v1/admin/tenant-storage/buckets/${bucketId}`, {
    method: "DELETE",
  });
}

/** 同步Bucket域名 */
export function syncBucketDomains(bucketId: number): Promise<{ status: string }> {
  return adminRequest<{ status: string }>(`/api/v1/admin/tenant-storage/buckets/${bucketId}/sync-domains`, {
    method: "POST",
  });
}

/** 获取七牛云Bucket列表 */
export function getQiniuBuckets(): Promise<string[]> {
  return adminRequest<string[]>("/api/v1/admin/tenant-storage/qiniu-buckets");
}

/** 获取所有存储套餐 */
export function getStoragePlans(): Promise<StoragePlan[]> {
  return adminRequest<StoragePlan[]>("/api/v1/admin/tenant-storage/plans");
}

/** 获取套餐详情 */
export function getStoragePlanDetail(planId: number): Promise<StoragePlan> {
  return adminRequest<StoragePlan>(`/api/v1/admin/tenant-storage/plans/${planId}`);
}

/** 分配套餐 */
export function assignStoragePlan(request: AssignPlanRequest): Promise<TenantBucket> {
  return adminRequest<TenantBucket>("/api/v1/admin/tenant-storage/plans/assign", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 套餐保存请求 */
export interface SavePlanRequest {
  plan_name: string;
  plan_code: string;
  plan_level: number;
  storage_quota_gb: number;
  max_file_count?: number | null;
  max_file_size_mb?: number | null;
  monthly_traffic_gb: number;
  monthly_cdn_traffic_gb?: number;
  monthly_get_requests?: number;
  monthly_put_requests?: number;
  base_price: number;
  storage_price_per_gb?: number;
  traffic_price_per_gb?: number;
  request_price_per_10k?: number;
  free_trial_days?: number;
  status?: boolean;
  sort_order?: number;
  description?: string;
  features?: string[];
}

/** 创建套餐 */
export function createStoragePlan(request: SavePlanRequest): Promise<StoragePlan> {
  return adminRequest<StoragePlan>("/api/v1/admin/tenant-storage/plans", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 更新套餐 */
export function updateStoragePlan(planId: number, request: SavePlanRequest): Promise<StoragePlan> {
  return adminRequest<StoragePlan>(`/api/v1/admin/tenant-storage/plans/${planId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 删除套餐 */
export function deleteStoragePlan(planId: number): Promise<{ status: string }> {
  return adminRequest<{ status: string }>(`/api/v1/admin/tenant-storage/plans/${planId}`, {
    method: "DELETE",
  });
}

/** 获取租户的计费记录 */
export function getBillingByTenant(tenantId: number): Promise<BillingRecord[]> {
  return adminRequest<BillingRecord[]>(`/api/v1/admin/tenant-storage/billing/tenant/${tenantId}`);
}

/** 获取空间的计费记录 */
export function getBillingByBucket(bucketId: number): Promise<BillingRecord[]> {
  return adminRequest<BillingRecord[]>(`/api/v1/admin/tenant-storage/billing/bucket/${bucketId}`);
}

/** 按月份获取计费记录 */
export function getBillingByPeriod(period: string): Promise<BillingRecord[]> {
  return adminRequest<BillingRecord[]>(`/api/v1/admin/tenant-storage/billing/period/${period}`);
}

/** 计算账单 */
export function calculateBill(request: CalculateBillRequest): Promise<BillingRecord> {
  return adminRequest<BillingRecord>("/api/v1/admin/tenant-storage/billing/calculate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 批量计算账单 */
export function calculateAllBills(period?: string): Promise<{ calculated: number; period: string }> {
  const params = period ? `?period=${period}` : "";
  return adminRequest<{ calculated: number; period: string }>(
    `/api/v1/admin/tenant-storage/billing/calculate-all${params}`,
    { method: "POST" }
  );
}

/** 获取用量快照 */
export function getUsageSnapshots(bucketId: number, begin?: string, end?: string): Promise<UsageSnapshot[]> {
  const params = new URLSearchParams();
  if (begin) params.set("begin", begin);
  if (end) params.set("end", end);
  return adminRequest<UsageSnapshot[]>(
    `/api/v1/admin/tenant-storage/usage/${bucketId}?${params.toString()}`
  );
}

/** 抓取当日快照 */
export function snapshotDailyUsage(bucketId: number): Promise<{ status: string }> {
  return adminRequest<{ status: string }>(`/api/v1/admin/tenant-storage/usage/snapshot/${bucketId}`, {
    method: "POST",
  });
}

/** 批量抓取快照 */
export function snapshotAllDailyUsage(): Promise<{ snapshotted: number }> {
  return adminRequest<{ snapshotted: number }>("/api/v1/admin/tenant-storage/usage/snapshot-all", {
    method: "POST",
  });
}

/** 获取平台概览 */
export function getPlatformStorageOverview(): Promise<PlatformStorageOverview> {
  return adminRequest<PlatformStorageOverview>("/api/v1/admin/tenant-storage/overview");
}

// ====================== 凭证管理 ======================

/** 凭证状态 */
export interface CredentialsStatus {
  configured: boolean;
  has_access_key: boolean;
  has_secret_key: boolean;
  masked_access_key: string;
  last_updated_at: string | null;
}

/** 凭证测试结果 */
export interface CredentialsTestResult {
  success: boolean;
  message: string;
  buckets: string[];
  bucket_count: number;
  latency_ms: number;
}

/** 凭证请求 */
export interface CredentialsRequest {
  access_key: string;
  secret_key: string;
}

/** 获取凭证配置状态 */
export function getCredentialsStatus(): Promise<CredentialsStatus> {
  return adminRequest<CredentialsStatus>("/api/v1/admin/tenant-storage/credentials/status");
}

/** 测试凭证连通性 */
export function testCredentials(request: CredentialsRequest): Promise<CredentialsTestResult> {
  return adminRequest<CredentialsTestResult>("/api/v1/admin/tenant-storage/credentials/test", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** 保存凭证 */
export function saveCredentials(request: CredentialsRequest): Promise<{ status: string; message: string }> {
  return adminRequest<{ status: string; message: string }>("/api/v1/admin/tenant-storage/credentials/save", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}
