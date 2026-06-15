package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.TenantBucketDtos.*;
import com.puyuanmaoshan.platform.service.TenantStorageService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户存储空间管理控制器
 * 
 * 核心功能（类比电表管理系统）：
 * - 存储空间分配：给租户安装"电表"（创建Bucket）
 * - 套餐管理：定义计费套餐（定价方案）
 * - 计费管理：生成和管理月度账单
 * - 用量监控：查看每日/每月用量快照
 */
@RestController
@RequestMapping("/api/v1/admin/tenant-storage")
@RequiredArgsConstructor
public class AdminTenantStorageController {

    private final TenantStorageService tenantStorageService;

    // ==================== 存储空间管理 ====================

    /**
     * 为租户创建存储空间（在七牛云创建Bucket + 分配）
     * 类比：给租户安装电表
     */
    @PostMapping("/buckets")
    public ApiResponse<TenantBucketResponse> createBucket(
            @RequestBody CreateBucketRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        TenantBucketResponse data = tenantStorageService.createBucket(request);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-create"));
    }

    /**
     * 获取所有存储空间列表
     */
    @GetMapping("/buckets")
    public ApiResponse<List<TenantBucketResponse>> listAllBuckets(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<TenantBucketResponse> data = tenantStorageService.listAllBuckets();
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-list"));
    }

    /**
     * 获取指定租户的存储空间
     */
    @GetMapping("/buckets/tenant/{tenantId}")
    public ApiResponse<List<TenantBucketResponse>> listBucketsByTenant(
            @PathVariable Long tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<TenantBucketResponse> data = tenantStorageService.listBucketsByTenant(tenantId);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-tenant"));
    }

    /**
     * 获取存储空间详情
     */
    @GetMapping("/buckets/{bucketId}")
    public ApiResponse<TenantBucketResponse> getBucketDetail(
            @PathVariable Long bucketId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        TenantBucketResponse data = tenantStorageService.getBucketDetail(bucketId);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-detail"));
    }

    /**
     * 更新存储空间配置
     */
    @PutMapping("/buckets/{bucketId}")
    public ApiResponse<TenantBucketResponse> updateBucket(
            @PathVariable Long bucketId,
            @RequestBody UpdateBucketRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        TenantBucketResponse data = tenantStorageService.updateBucket(bucketId, request);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-update"));
    }

    /**
     * 删除存储空间
     */
    @DeleteMapping("/buckets/{bucketId}")
    public ApiResponse<Map<String, Object>> deleteBucket(
            @PathVariable Long bucketId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        tenantStorageService.deleteBucket(bucketId);
        return ApiResponse.ok(Map.of("status", "deleted", "id", bucketId),
                RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-delete"));
    }

    /**
     * 同步Bucket域名
     */
    @PostMapping("/buckets/{bucketId}/sync-domains")
    public ApiResponse<Map<String, Object>> syncBucketDomains(
            @PathVariable Long bucketId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        tenantStorageService.syncBucketDomains(bucketId);
        return ApiResponse.ok(Map.of("status", "synced"),
                RequestContextUtil.resolveRequestId(requestId, "req-tenant-bucket-sync-domains"));
    }

    /**
     * 获取七牛云账号下所有Bucket列表
     */
    @GetMapping("/qiniu-buckets")
    public ApiResponse<List<String>> listQiniuBuckets(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<String> data = tenantStorageService.listQiniuBuckets();
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-qiniu-buckets"));
    }

    // ==================== 存储套餐管理 ====================

    /**
     * 获取所有存储套餐
     */
    @GetMapping("/plans")
    public ApiResponse<List<StoragePlanResponse>> listAllPlans(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<StoragePlanResponse> data = tenantStorageService.listAllPlans();
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-plan-list"));
    }

    /**
     * 获取套餐详情
     */
    @GetMapping("/plans/{planId}")
    public ApiResponse<StoragePlanResponse> getPlanDetail(
            @PathVariable Long planId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        StoragePlanResponse data = tenantStorageService.getPlanDetail(planId);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-plan-detail"));
    }

    /**
     * 为租户分配/变更存储套餐
     */
    @PostMapping("/plans/assign")
    public ApiResponse<TenantBucketResponse> assignPlan(
            @RequestBody AssignPlanRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        TenantBucketResponse data = tenantStorageService.assignPlan(request);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-plan-assign"));
    }

    /**
     * 创建新套餐
     */
    @PostMapping("/plans")
    public ApiResponse<StoragePlanResponse> createPlan(
            @RequestBody SavePlanRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        StoragePlanResponse data = tenantStorageService.createPlan(request);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-plan-create"));
    }

    /**
     * 更新套餐
     */
    @PutMapping("/plans/{planId}")
    public ApiResponse<StoragePlanResponse> updatePlan(
            @PathVariable Long planId,
            @RequestBody SavePlanRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        StoragePlanResponse data = tenantStorageService.updatePlan(planId, request);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-plan-update"));
    }

    /**
     * 删除套餐（软删除，设 status=0）
     */
    @DeleteMapping("/plans/{planId}")
    public ApiResponse<Map<String, Object>> deletePlan(
            @PathVariable Long planId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        tenantStorageService.deletePlan(planId);
        return ApiResponse.ok(Map.of("status", "deleted", "id", planId),
                RequestContextUtil.resolveRequestId(requestId, "req-tenant-plan-delete"));
    }

    // ==================== 计费管理 ====================

    /**
     * 获取指定租户的计费记录
     */
    @GetMapping("/billing/tenant/{tenantId}")
    public ApiResponse<List<BillingRecordResponse>> listBillingByTenant(
            @PathVariable Long tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<BillingRecordResponse> data = tenantStorageService.listBillingRecords(tenantId);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-billing-tenant"));
    }

    /**
     * 获取指定存储空间的计费记录
     */
    @GetMapping("/billing/bucket/{bucketId}")
    public ApiResponse<List<BillingRecordResponse>> listBillingByBucket(
            @PathVariable Long bucketId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<BillingRecordResponse> data = tenantStorageService.listBillingRecordsByBucket(bucketId);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-billing-bucket"));
    }

    /**
     * 按月份获取所有计费记录
     */
    @GetMapping("/billing/period/{period}")
    public ApiResponse<List<BillingRecordResponse>> listBillingByPeriod(
            @PathVariable String period,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<BillingRecordResponse> data = tenantStorageService.listBillingRecordsByPeriod(period);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-billing-period"));
    }

    /**
     * 计算指定空间的月度账单
     */
    @PostMapping("/billing/calculate")
    public ApiResponse<BillingRecordResponse> calculateBill(
            @RequestBody CalculateBillRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        BillingRecordResponse data = tenantStorageService.calculateBill(request);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-billing-calc"));
    }

    /**
     * 批量计算所有空间的月度账单
     */
    @PostMapping("/billing/calculate-all")
    public ApiResponse<Map<String, Object>> calculateAllBills(
            @RequestParam(name = "period", required = false) String period,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        int count = tenantStorageService.calculateAllMonthlyBills(period);
        return ApiResponse.ok(Map.of("calculated", count, "period", period != null ? period : "上个月"),
                RequestContextUtil.resolveRequestId(requestId, "req-tenant-billing-calc-all"));
    }

    // ==================== 用量统计 ====================

    /**
     * 获取指定空间的用量快照数据
     */
    @GetMapping("/usage/{bucketId}")
    public ApiResponse<List<UsageSnapshotResponse>> getUsageSnapshots(
            @PathVariable Long bucketId,
            @RequestParam(name = "begin", required = false) String begin,
            @RequestParam(name = "end", required = false) String end,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<UsageSnapshotResponse> data = tenantStorageService.getUsageSnapshots(bucketId, begin, end);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-usage-snapshot"));
    }

    /**
     * 抓取当日用量快照（手动触发）
     */
    @PostMapping("/usage/snapshot/{bucketId}")
    public ApiResponse<Map<String, Object>> snapshotDailyUsage(
            @PathVariable Long bucketId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        tenantStorageService.snapshotDailyUsage(bucketId);
        return ApiResponse.ok(Map.of("status", "snapshotted", "bucketId", bucketId),
                RequestContextUtil.resolveRequestId(requestId, "req-tenant-usage-snapshot"));
    }

    /**
     * 批量抓取所有空间的当日用量
     */
    @PostMapping("/usage/snapshot-all")
    public ApiResponse<Map<String, Object>> snapshotAllDailyUsage(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        int count = tenantStorageService.snapshotAllDailyUsage();
        return ApiResponse.ok(Map.of("snapshotted", count),
                RequestContextUtil.resolveRequestId(requestId, "req-tenant-usage-snapshot-all"));
    }

    // ==================== 平台概览 ====================

    /**
     * 获取平台存储概览统计
     */
    @GetMapping("/overview")
    public ApiResponse<PlatformStorageOverview> getPlatformOverview(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        PlatformStorageOverview data = tenantStorageService.getPlatformOverview();
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-tenant-storage-overview"));
    }

    // ==================== 凭证管理 ====================

    /**
     * 获取凭证配置状态
     */
    @GetMapping("/credentials/status")
    public ApiResponse<CredentialsStatusResponse> getCredentialsStatus(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        CredentialsStatusResponse data = tenantStorageService.getCredentialsStatus();
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-credentials-status"));
    }

    /**
     * 测试凭证是否可用（不保存）
     * 传入AK/SK后调用七牛云API验证连通性
     */
    @PostMapping("/credentials/test")
    public ApiResponse<CredentialsTestResult> testCredentials(
            @RequestBody CredentialsRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        CredentialsTestResult data = tenantStorageService.testCredentials(
                request.getAccessKey(), request.getSecretKey());
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-credentials-test"));
    }

    /**
     * 保存凭证（自动先验证再保存）
     */
    @PostMapping("/credentials/save")
    public ApiResponse<Map<String, Object>> saveCredentials(
            @RequestBody CredentialsRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        tenantStorageService.saveCredentials(request);
        return ApiResponse.ok(Map.of("status", "saved", "message", "凭证保存成功"),
                RequestContextUtil.resolveRequestId(requestId, "req-credentials-save"));
    }
}
