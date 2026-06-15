package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.TenantBucketDtos.*;

import java.util.List;

/**
 * 租户存储空间管理服务接口
 * 核心功能：
 * 1. 存储空间（Bucket）的创建、删除、分配（类比"安装电表"）
 * 2. 存储套餐管理（定价方案）
 * 3. 租户套餐绑定（订购关系）
 * 4. 计费账单生成与管理
 * 5. 用量快照与统计
 */
public interface TenantStorageService {

    // ========== 存储空间管理 ==========

    /**
     * 为租户创建并分配存储空间（在七牛云创建Bucket + 本地记录）
     */
    TenantBucketResponse createBucket(CreateBucketRequest request);

    /**
     * 获取所有存储空间列表
     */
    List<TenantBucketResponse> listAllBuckets();

    /**
     * 获取指定租户的存储空间列表
     */
    List<TenantBucketResponse> listBucketsByTenant(Long tenantId);

    /**
     * 获取存储空间详情
     */
    TenantBucketResponse getBucketDetail(Long bucketId);

    /**
     * 更新存储空间配置
     */
    TenantBucketResponse updateBucket(Long bucketId, UpdateBucketRequest request);

    /**
     * 删除存储空间（删除七牛云Bucket + 软删除本地记录）
     */
    void deleteBucket(Long bucketId);

    /**
     * 同步七牛云Bucket域名信息
     */
    void syncBucketDomains(Long bucketId);

    // ========== 存储套餐管理 ==========

    /**
     * 获取所有可用存储套餐
     */
    List<StoragePlanResponse> listAllPlans();

    /**
     * 获取套餐详情
     */
    StoragePlanResponse getPlanDetail(Long planId);

    /**
     * 为租户分配/变更存储套餐
     */
    TenantBucketResponse assignPlan(AssignPlanRequest request);

    /**
     * 创建新套餐
     */
    StoragePlanResponse createPlan(SavePlanRequest request);

    /**
     * 更新套餐
     */
    StoragePlanResponse updatePlan(Long planId, SavePlanRequest request);

    /**
     * 删除套餐（软删除：设置 status=0）
     */
    void deletePlan(Long planId);

    // ========== 计费管理 ==========

    /**
     * 获取指定租户的计费记录
     */
    List<BillingRecordResponse> listBillingRecords(Long tenantId);

    /**
     * 获取指定存储空间的计费记录
     */
    List<BillingRecordResponse> listBillingRecordsByBucket(Long bucketId);

    /**
     * 按月份获取计费记录
     */
    List<BillingRecordResponse> listBillingRecordsByPeriod(String period);

    /**
     * 计算月度账单
     */
    BillingRecordResponse calculateBill(CalculateBillRequest request);

    /**
     * 批量计算所有活跃空间的月度账单
     */
    int calculateAllMonthlyBills(String billPeriod);

    // ========== 用量统计 ==========

    /**
     * 获取指定存储空间的用量快照数据
     */
    List<UsageSnapshotResponse> getUsageSnapshots(Long bucketId, String begin, String end);

    /**
     * 从七牛云抓取当日用量快照（定时任务用）
     */
    void snapshotDailyUsage(Long bucketId);

    /**
     * 批量抓取所有活跃空间的当日用量
     */
    int snapshotAllDailyUsage();

    // ========== 平台概览 ==========

    /**
     * 获取平台存储概览统计
     */
    PlatformStorageOverview getPlatformOverview();

    /**
     * 获取七牛云账号下所有Bucket列表
     */
    List<String> listQiniuBuckets();

    // ========== 凭证管理 ==========

    /**
     * 获取七牛云凭证配置状态
     */
    CredentialsStatusResponse getCredentialsStatus();

    /**
     * 测试七牛云凭证是否可用（通过调用listBuckets验证）
     * @param accessKey 临时传入的AK，为空则用已保存的
     * @param secretKey 临时传入的SK，为空则用已保存的
     */
    CredentialsTestResult testCredentials(String accessKey, String secretKey);

    /**
     * 保存七牛云凭证到system_config（自动加密）
     */
    void saveCredentials(CredentialsRequest request);

    // ========== 新租户自动分配 ==========

    /**
     * 新租户注册后自动分配存储空间
     * 使用默认免费套餐（plan_level 最小的启用套餐），Bucket名称由系统自动生成
     * 如果七牛云凭证未配置或套餐不存在，静默跳过不报错
     *
     * @param tenantId   租户ID
     * @param tenantCode 租户编码（用于生成Bucket名称）
     * @return 创建的Bucket响应，失败时返回 null
     */
    TenantBucketResponse autoAssignBucketForNewTenant(Long tenantId, String tenantCode);
}
