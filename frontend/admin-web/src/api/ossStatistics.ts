import { adminRequest } from "../utils/http";

// OSS 存储统计相关接口
// 基于七牛云 Kodo 统计接口: https://developer.qiniu.com/kodo/3906/statistic-interface

/** 统计数据点 */
export interface StatisticDataPoint {
  time: string;
  value: number;
}

/** 单个统计类型响应 */
export interface StatisticTypeResponse {
  stat_type: string;
  stat_label: string;
  unit: string;
  datas: StatisticDataPoint[];
}

/** blob_io 数据点 */
export interface BlobIoDataPoint {
  time: string;
  flux: number;
  cdn_flux: number;
  read_bytes: number;
  get_count: number;
}

/** blob_io 统计响应 */
export interface BlobIoStatisticResponse {
  datas: BlobIoDataPoint[];
  bucket: string;
  query_range: string;
}

/** 存储概览响应 */
export interface StorageOverviewResponse {
  standard_space: number;
  standard_count: number;
  line_space: number;
  line_count: number;
  archive_space: number;
  archive_count: number;
  blob_io_flux: number;
  cdn_flux: number;
  get_count: number;
  put_count: number;
  standard_space_gb: number;
  line_space_gb: number;
  archive_space_gb: number;
  blob_io_flux_gb: number;
  cdn_flux_gb: number;
  query_range: string;
  bucket: string;
}

/** 支持的统计类型 */
export interface StatTypeInfo {
  stat_type: string;
  stat_label: string;
  unit: string;
}

/**
 * 获取存储概览
 */
export function getOssStatisticsOverview(begin?: string, end?: string): Promise<StorageOverviewResponse> {
  const params = new URLSearchParams();
  if (begin) params.set("begin", begin);
  if (end) params.set("end", end);
  return adminRequest<StorageOverviewResponse>(`/api/v1/admin/oss-statistics/overview?${params.toString()}`);
}

/**
 * 获取指定统计类型的时序数据
 */
export function getOssStatistic(
  statType: string,
  begin?: string,
  end?: string,
  granularity: string = "day"
): Promise<StatisticTypeResponse> {
  const params = new URLSearchParams();
  params.set("stat_type", statType);
  if (begin) params.set("begin", begin);
  if (end) params.set("end", end);
  params.set("granularity", granularity);
  return adminRequest<StatisticTypeResponse>(`/api/v1/admin/oss-statistics/stat?${params.toString()}`);
}

/**
 * 获取 blob_io 统计
 */
export function getOssBlobIoStatistic(
  begin?: string,
  end?: string,
  granularity: string = "day"
): Promise<BlobIoStatisticResponse> {
  const params = new URLSearchParams();
  if (begin) params.set("begin", begin);
  if (end) params.set("end", end);
  params.set("granularity", granularity);
  return adminRequest<BlobIoStatisticResponse>(`/api/v1/admin/oss-statistics/blob-io?${params.toString()}`);
}

/**
 * 获取所有支持的统计类型
 */
export function getOssStatTypes(): Promise<StatTypeInfo[]> {
  return adminRequest<StatTypeInfo[]>("/api/v1/admin/oss-statistics/types");
}
