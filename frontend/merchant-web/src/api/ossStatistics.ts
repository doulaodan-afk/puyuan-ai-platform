import { merchantRequest } from "../utils/http";

// 商家端存储统计接口

/** 存储概览响应（精简版） */
export interface MerchantStorageOverview {
  standard_space_gb: number;
  standard_count: number;
  line_space_gb: number;
  archive_space_gb: number;
  blob_io_flux_gb: number;
  query_range: string;
  bucket: string;
}

/**
 * 获取商家端存储概览
 */
export function getMerchantStorageOverview(begin?: string, end?: string): Promise<MerchantStorageOverview> {
  const params = new URLSearchParams();
  if (begin) params.set("begin", begin);
  if (end) params.set("end", end);
  return merchantRequest<MerchantStorageOverview>(`/api/v1/account/storage-overview?${params.toString()}`);
}
