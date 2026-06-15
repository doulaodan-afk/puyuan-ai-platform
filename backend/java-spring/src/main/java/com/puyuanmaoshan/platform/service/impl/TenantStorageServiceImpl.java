package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.SystemConfigDtos;
import com.puyuanmaoshan.platform.dto.TenantBucketDtos.*;
import com.puyuanmaoshan.platform.entity.*;
import com.puyuanmaoshan.platform.mapper.*;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.service.TenantStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 租户存储空间管理服务实现
 * 
 * 核心设计：类比"电表"模式
 * - 平台给每个租户分配独立的七牛云Bucket（安装电表）
 * - 每日从七牛云获取用量快照（抄表）
 * - 月底汇总生成账单（出账）
 * - 根据套餐计算费用（计费）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantStorageServiceImpl extends ServiceImpl<TenantBucketMapper, TenantBucket> implements TenantStorageService {

    private static final String QINIU_UC_API = "https://uc.qiniuapi.com";
    private static final String QINIU_API_BASE = "https://api.qiniu.com/v6";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TenantBucketMapper tenantBucketMapper;
    private final StoragePlanMapper storagePlanMapper;
    private final TenantStoragePlanMapper tenantStoragePlanMapper;
    private final StorageBillingRecordMapper billingRecordMapper;
    private final StorageUsageLogMapper usageLogMapper;
    private final SystemConfigService systemConfigService;

    @Value("${app.storage.qiniu.access-key:}")
    private String defaultAccessKey;

    @Value("${app.storage.qiniu.secret-key:}")
    private String defaultSecretKey;

    // ========== 存储空间管理 ==========

    @Override
    @Transactional
    public TenantBucketResponse createBucket(CreateBucketRequest request) {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String bucketName = request.getBucketName();

        // 1. 在七牛云创建Bucket
        try {
            createQiniuBucket(bucketName, request.getBucketRegion(), accessKey, secretKey);
        } catch (Exception e) {
            log.error("Failed to create Qiniu bucket: {}", bucketName, e);
            throw new RuntimeException("七牛云Bucket创建失败: " + e.getMessage());
        }

        // 2. 本地记录
        TenantBucket bucket = TenantBucket.builder()
                .tenantId(request.getTenantId())
                .bucketName(bucketName)
                .bucketRegion(request.getBucketRegion())
                .bucketPrivate(request.getBucketPrivate())
                .status("active")
                .notes(request.getNotes())
                .build();

        tenantBucketMapper.insert(bucket);

        // 3. 如果指定了套餐，自动绑定
        if (request.getPlanId() != null) {
            assignPlan(AssignPlanRequest.builder()
                    .tenantId(request.getTenantId())
                    .tenantBucketId(bucket.getId())
                    .planId(request.getPlanId())
                    .autoRenew(true)
                    .build());
        }

        log.info("Tenant bucket created: tenantId={}, bucketName={}, id={}",
                request.getTenantId(), bucketName, bucket.getId());

        return buildBucketResponse(bucket);
    }

    @Override
    public List<TenantBucketResponse> listAllBuckets() {
        List<TenantBucket> buckets = tenantBucketMapper.selectList(
                new LambdaQueryWrapper<TenantBucket>().ne(TenantBucket::getStatus, "deleted"));
        return buckets.stream().map(this::buildBucketResponse).collect(Collectors.toList());
    }

    @Override
    public List<TenantBucketResponse> listBucketsByTenant(Long tenantId) {
        List<TenantBucket> buckets = tenantBucketMapper.findByTenantId(tenantId);
        return buckets.stream().map(this::buildBucketResponse).collect(Collectors.toList());
    }

    @Override
    public TenantBucketResponse getBucketDetail(Long bucketId) {
        TenantBucket bucket = tenantBucketMapper.selectById(bucketId);
        if (bucket == null) {
            throw new RuntimeException("存储空间不存在: " + bucketId);
        }
        return buildBucketResponse(bucket);
    }

    @Override
    @Transactional
    public TenantBucketResponse updateBucket(Long bucketId, UpdateBucketRequest request) {
        TenantBucket bucket = tenantBucketMapper.selectById(bucketId);
        if (bucket == null) {
            throw new RuntimeException("存储空间不存在: " + bucketId);
        }

        if (request.getBucketDomain() != null) bucket.setBucketDomain(request.getBucketDomain());
        if (request.getBucketPrivate() != null) bucket.setBucketPrivate(request.getBucketPrivate());
        if (request.getStatus() != null) bucket.setStatus(request.getStatus());
        if (request.getNotes() != null) bucket.setNotes(request.getNotes());

        tenantBucketMapper.updateById(bucket);
        return buildBucketResponse(bucket);
    }

    @Override
    @Transactional
    public void deleteBucket(Long bucketId) {
        TenantBucket bucket = tenantBucketMapper.selectById(bucketId);
        if (bucket == null) {
            throw new RuntimeException("存储空间不存在: " + bucketId);
        }

        // 在七牛云删除Bucket
        try {
            dropQiniuBucket(bucket.getBucketName(), resolveAccessKey(), resolveSecretKey());
        } catch (Exception e) {
            log.warn("Failed to drop Qiniu bucket: {}, will soft delete locally", bucket.getBucketName(), e);
        }

        // 软删除本地记录
        bucket.setStatus("deleted");
        tenantBucketMapper.updateById(bucket);

        // 取消活跃的套餐绑定
        TenantStoragePlan activePlan = tenantStoragePlanMapper.findActiveByBucketId(bucketId);
        if (activePlan != null) {
            activePlan.setPlanStatus("cancelled");
            tenantStoragePlanMapper.updateById(activePlan);
        }

        log.info("Tenant bucket deleted: id={}, bucketName={}", bucketId, bucket.getBucketName());
    }

    @Override
    public void syncBucketDomains(Long bucketId) {
        TenantBucket bucket = tenantBucketMapper.selectById(bucketId);
        if (bucket == null) return;

        try {
            List<String> domains = fetchQiniuBucketDomains(bucket.getBucketName(),
                    resolveAccessKey(), resolveSecretKey());
            if (!domains.isEmpty()) {
                bucket.setBucketDomain(domains.get(0));
                tenantBucketMapper.updateById(bucket);
                log.info("Synced domains for bucket {}: {}", bucket.getBucketName(), domains);
            }
        } catch (Exception e) {
            log.error("Failed to sync domains for bucket {}", bucket.getBucketName(), e);
        }
    }

    // ========== 存储套餐管理 ==========

    @Override
    public List<StoragePlanResponse> listAllPlans() {
        List<StoragePlan> plans = storagePlanMapper.findAllActive();
        return plans.stream().map(plan -> {
            Long tenantCount = storagePlanMapper.countActiveTenants(plan.getId());
            List<String> features = parseFeatures(plan.getFeaturesJson());
            return StoragePlanResponse.builder()
                    .id(plan.getId())
                    .planName(plan.getPlanName())
                    .planCode(plan.getPlanCode())
                    .planLevel(plan.getPlanLevel())
                    .storageQuotaGb(plan.getStorageQuotaGb())
                    .maxFileCount(plan.getMaxFileCount())
                    .maxFileSizeMb(plan.getMaxFileSizeMb())
                    .monthlyTrafficGb(plan.getMonthlyTrafficGb())
                    .monthlyCdnTrafficGb(plan.getMonthlyCdnTrafficGb())
                    .monthlyGetRequests(plan.getMonthlyGetRequests())
                    .monthlyPutRequests(plan.getMonthlyPutRequests())
                    .basePrice(plan.getBasePrice())
                    .storagePricePerGb(plan.getStoragePricePerGb())
                    .trafficPricePerGb(plan.getTrafficPricePerGb())
                    .requestPricePer10k(plan.getRequestPricePer10k())
                    .freeTrialDays(plan.getFreeTrialDays())
                    .status(plan.getStatus())
                    .sortOrder(plan.getSortOrder())
                    .description(plan.getDescription())
                    .features(features)
                    .tenantCount(tenantCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public StoragePlanResponse getPlanDetail(Long planId) {
        StoragePlan plan = storagePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("存储套餐不存在: " + planId);
        }
        Long tenantCount = storagePlanMapper.countActiveTenants(planId);
        List<String> features = parseFeatures(plan.getFeaturesJson());
        return StoragePlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .planCode(plan.getPlanCode())
                .planLevel(plan.getPlanLevel())
                .storageQuotaGb(plan.getStorageQuotaGb())
                .maxFileCount(plan.getMaxFileCount())
                .maxFileSizeMb(plan.getMaxFileSizeMb())
                .monthlyTrafficGb(plan.getMonthlyTrafficGb())
                .monthlyCdnTrafficGb(plan.getMonthlyCdnTrafficGb())
                .monthlyGetRequests(plan.getMonthlyGetRequests())
                .monthlyPutRequests(plan.getMonthlyPutRequests())
                .basePrice(plan.getBasePrice())
                .storagePricePerGb(plan.getStoragePricePerGb())
                .trafficPricePerGb(plan.getTrafficPricePerGb())
                .requestPricePer10k(plan.getRequestPricePer10k())
                .freeTrialDays(plan.getFreeTrialDays())
                .status(plan.getStatus())
                .sortOrder(plan.getSortOrder())
                .description(plan.getDescription())
                .features(features)
                .tenantCount(tenantCount)
                .build();
    }

    @Override
    @Transactional
    public TenantBucketResponse assignPlan(AssignPlanRequest request) {
        TenantBucket bucket = tenantBucketMapper.selectById(request.getTenantBucketId());
        if (bucket == null) {
            throw new RuntimeException("存储空间不存在: " + request.getTenantBucketId());
        }

        StoragePlan plan = storagePlanMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw new RuntimeException("存储套餐不存在: " + request.getPlanId());
        }

        // 取消旧的活跃套餐
        TenantStoragePlan oldPlan = tenantStoragePlanMapper.findActiveByBucketId(request.getTenantBucketId());
        if (oldPlan != null) {
            oldPlan.setPlanStatus("upgraded");
            tenantStoragePlanMapper.updateById(oldPlan);
        }

        // 绑定新套餐
        LocalDate today = LocalDate.now();
        TenantStoragePlan newPlan = TenantStoragePlan.builder()
                .tenantId(request.getTenantId())
                .planId(request.getPlanId())
                .tenantBucketId(request.getTenantBucketId())
                .planStatus("active")
                .effectiveDate(today)
                .expireDate(plan.getFreeTrialDays() != null && plan.getFreeTrialDays() > 0
                        ? today.plusDays(plan.getFreeTrialDays()) : null)
                .autoRenew(request.getAutoRenew())
                .build();
        tenantStoragePlanMapper.insert(newPlan);

        log.info("Plan assigned: tenantId={}, bucketId={}, planCode={}",
                request.getTenantId(), request.getTenantBucketId(), plan.getPlanCode());

        return buildBucketResponse(bucket);
    }

    @Override
    @Transactional
    public StoragePlanResponse createPlan(SavePlanRequest request) {
        log.info("createPlan called: planName={}, planCode={}, planLevel={}, storageQuotaGb={}, monthlyTrafficGb={}, basePrice={}",
                request.getPlanName(), request.getPlanCode(), request.getPlanLevel(),
                request.getStorageQuotaGb(), request.getMonthlyTrafficGb(), request.getBasePrice());
        try {
            // 必填字段校验
            if (request.getPlanName() == null || request.getPlanName().isBlank()) {
                throw new RuntimeException("套餐名称不能为空");
            }
            if (request.getPlanCode() == null || request.getPlanCode().isBlank()) {
                throw new RuntimeException("套餐编码不能为空");
            }
            if (request.getStorageQuotaGb() == null) {
                throw new RuntimeException("存储配额不能为空");
            }
            if (request.getMonthlyTrafficGb() == null) {
                throw new RuntimeException("月流量配额不能为空");
            }
            if (request.getBasePrice() == null) {
                throw new RuntimeException("基础月费不能为空");
            }
            // 检查 plan_code 唯一性
            StoragePlan existing = storagePlanMapper.findByCode(request.getPlanCode());
            if (existing != null) {
                throw new RuntimeException("套餐编码 " + request.getPlanCode() + " 已存在");
            }

            StoragePlan plan = new StoragePlan();
            fillPlanFromRequest(plan, request);
            log.info("About to insert plan: {}", plan);
            storagePlanMapper.insert(plan);
            log.info("Storage plan created: id={}, code={}, name={}", plan.getId(), plan.getPlanCode(), plan.getPlanName());
            return buildPlanResponse(plan);
        } catch (Exception e) {
            log.error("createPlan failed: {}", e.getMessage(), e);
            throw new RuntimeException("创建套餐失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public StoragePlanResponse updatePlan(Long planId, SavePlanRequest request) {
        StoragePlan plan = storagePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("存储套餐不存在: " + planId);
        }
        // 检查 plan_code 唯一性（排除自身）
        if (request.getPlanCode() != null) {
            StoragePlan existing = storagePlanMapper.findByCode(request.getPlanCode());
            if (existing != null && !existing.getId().equals(planId)) {
                throw new RuntimeException("套餐编码 " + request.getPlanCode() + " 已被其他套餐使用");
            }
        }
        fillPlanFromRequest(plan, request);
        storagePlanMapper.updateById(plan);
        log.info("Storage plan updated: id={}, code={}, name={}", planId, plan.getPlanCode(), plan.getPlanName());
        return buildPlanResponse(plan);
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        StoragePlan plan = storagePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("存储套餐不存在: " + planId);
        }
        plan.setStatus(false);
        storagePlanMapper.updateById(plan);
        log.info("Storage plan soft-deleted: id={}, code={}", planId, plan.getPlanCode());
    }

    /** 将请求字段填充到实体 */
    private void fillPlanFromRequest(StoragePlan plan, SavePlanRequest request) {
        if (request.getPlanName() != null) plan.setPlanName(request.getPlanName());
        if (request.getPlanCode() != null) plan.setPlanCode(request.getPlanCode());
        if (request.getPlanLevel() != null) plan.setPlanLevel(request.getPlanLevel());
        if (request.getStorageQuotaGb() != null) plan.setStorageQuotaGb(request.getStorageQuotaGb());
        if (request.getMaxFileCount() != null) plan.setMaxFileCount(request.getMaxFileCount());
        if (request.getMaxFileSizeMb() != null) plan.setMaxFileSizeMb(request.getMaxFileSizeMb());
        if (request.getMonthlyTrafficGb() != null) plan.setMonthlyTrafficGb(request.getMonthlyTrafficGb());
        if (request.getMonthlyCdnTrafficGb() != null) plan.setMonthlyCdnTrafficGb(request.getMonthlyCdnTrafficGb());
        if (request.getMonthlyGetRequests() != null) plan.setMonthlyGetRequests(request.getMonthlyGetRequests());
        if (request.getMonthlyPutRequests() != null) plan.setMonthlyPutRequests(request.getMonthlyPutRequests());
        if (request.getBasePrice() != null) plan.setBasePrice(request.getBasePrice());
        if (request.getStoragePricePerGb() != null) plan.setStoragePricePerGb(request.getStoragePricePerGb());
        if (request.getTrafficPricePerGb() != null) plan.setTrafficPricePerGb(request.getTrafficPricePerGb());
        if (request.getRequestPricePer10k() != null) plan.setRequestPricePer10k(request.getRequestPricePer10k());
        if (request.getFreeTrialDays() != null) plan.setFreeTrialDays(request.getFreeTrialDays());
        if (request.getStatus() != null) plan.setStatus(request.getStatus());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getFeatures() != null) {
            try {
                plan.setFeaturesJson(objectMapper.writeValueAsString(request.getFeatures()));
            } catch (Exception e) {
                log.warn("Failed to serialize features to JSON: {}", e.getMessage());
            }
        }
    }

    /** 将实体转为响应 */
    private StoragePlanResponse buildPlanResponse(StoragePlan plan) {
        Long tenantCount = storagePlanMapper.countActiveTenants(plan.getId());
        List<String> features = parseFeatures(plan.getFeaturesJson());
        return StoragePlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .planCode(plan.getPlanCode())
                .planLevel(plan.getPlanLevel())
                .storageQuotaGb(plan.getStorageQuotaGb())
                .maxFileCount(plan.getMaxFileCount())
                .maxFileSizeMb(plan.getMaxFileSizeMb())
                .monthlyTrafficGb(plan.getMonthlyTrafficGb())
                .monthlyCdnTrafficGb(plan.getMonthlyCdnTrafficGb())
                .monthlyGetRequests(plan.getMonthlyGetRequests())
                .monthlyPutRequests(plan.getMonthlyPutRequests())
                .basePrice(plan.getBasePrice())
                .storagePricePerGb(plan.getStoragePricePerGb())
                .trafficPricePerGb(plan.getTrafficPricePerGb())
                .requestPricePer10k(plan.getRequestPricePer10k())
                .freeTrialDays(plan.getFreeTrialDays())
                .status(plan.getStatus())
                .sortOrder(plan.getSortOrder())
                .description(plan.getDescription())
                .features(features)
                .tenantCount(tenantCount)
                .build();
    }

    // ========== 计费管理 ==========

    @Override
    public List<BillingRecordResponse> listBillingRecords(Long tenantId) {
        List<StorageBillingRecord> records = billingRecordMapper.findByTenantId(tenantId);
        return records.stream().map(this::buildBillingResponse).collect(Collectors.toList());
    }

    @Override
    public List<BillingRecordResponse> listBillingRecordsByBucket(Long bucketId) {
        List<StorageBillingRecord> records = billingRecordMapper.findByBucketId(bucketId);
        return records.stream().map(this::buildBillingResponse).collect(Collectors.toList());
    }

    @Override
    public List<BillingRecordResponse> listBillingRecordsByPeriod(String period) {
        List<StorageBillingRecord> records = billingRecordMapper.findByPeriod(period);
        return records.stream().map(this::buildBillingResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BillingRecordResponse calculateBill(CalculateBillRequest request) {
        TenantBucket bucket = tenantBucketMapper.selectById(request.getTenantBucketId());
        if (bucket == null) {
            throw new RuntimeException("存储空间不存在: " + request.getTenantBucketId());
        }

        String billPeriod = request.getBillPeriod();
        if (billPeriod == null || billPeriod.isEmpty()) {
            billPeriod = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        // 检查是否已生成账单
        StorageBillingRecord existing = billingRecordMapper.findByTenantBucketPeriod(
                bucket.getTenantId(), bucket.getId(), billPeriod);
        if (existing != null) {
            return buildBillingResponse(existing);
        }

        // 获取当前套餐
        TenantStoragePlan tsp = tenantStoragePlanMapper.findActiveByBucketId(bucket.getId());
        StoragePlan plan = tsp != null ? storagePlanMapper.selectById(tsp.getPlanId()) : null;

        // 从七牛云获取该月用量数据
        LocalDate monthStart = YearMonth.parse(billPeriod).atDay(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        double standardStorageGb = 0, lineStorageGb = 0, archiveStorageGb = 0;
        long standardFileCount = 0;
        double externalTrafficGb = 0, cdnTrafficGb = 0;
        long getRequests = 0, putRequests = 0;

        try {
            // 获取存储量统计
            JsonNode spaceData = fetchQiniuStatApi(bucket.getBucketName(), "space",
                    monthStart.format(DATE_FMT), monthEnd.format(DATE_FMT), "day");
            JsonNode datas = spaceData.path("datas");
            if (datas.isArray() && !datas.isEmpty()) {
                JsonNode latest = datas.get(datas.size() - 1);
                if (latest.has("values")) {
                    Iterator<String> fields = latest.get("values").fieldNames();
                    if (fields.hasNext()) {
                        standardStorageGb = latest.get("values").get(fields.next()).asLong(0) / BYTES_PER_GB;
                    }
                }
            }

            // 获取流量和请求统计
            JsonNode blobIoData = fetchQiniuStatApi(bucket.getBucketName(), "blob_io",
                    monthStart.format(DATE_FMT), monthEnd.format(DATE_FMT), "day");
            JsonNode ioDatas = blobIoData.path("datas");
            if (ioDatas.isArray()) {
                for (JsonNode item : ioDatas) {
                    externalTrafficGb += item.path("flux").asLong(0);
                    cdnTrafficGb += item.path("cdn_flux").asLong(0);
                    getRequests += item.path("get_count").asLong(0);
                }
            }
            externalTrafficGb /= BYTES_PER_GB;
            cdnTrafficGb /= BYTES_PER_GB;

        } catch (Exception e) {
            log.warn("Failed to fetch Qiniu stats for billing: bucket={}, period={}, error={}",
                    bucket.getBucketName(), billPeriod, e.getMessage());
        }

        // 计算费用
        BigDecimal baseFee = BigDecimal.ZERO;
        BigDecimal storageOverageFee = BigDecimal.ZERO;
        BigDecimal trafficOverageFee = BigDecimal.ZERO;
        BigDecimal requestOverageFee = BigDecimal.ZERO;

        double quotaStorageGb = 10.0;
        double quotaTrafficGb = 100.0;

        if (plan != null) {
            baseFee = plan.getBasePrice();
            quotaStorageGb = plan.getStorageQuotaGb();
            quotaTrafficGb = plan.getMonthlyTrafficGb();

            // 超额存储费
            double overStorage = standardStorageGb - plan.getStorageQuotaGb();
            if (overStorage > 0) {
                storageOverageFee = plan.getStoragePricePerGb()
                        .multiply(BigDecimal.valueOf(overStorage))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // 超额流量费
            double overTraffic = externalTrafficGb - plan.getMonthlyTrafficGb();
            if (overTraffic > 0) {
                trafficOverageFee = plan.getTrafficPricePerGb()
                        .multiply(BigDecimal.valueOf(overTraffic))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalFee = baseFee.add(storageOverageFee).add(trafficOverageFee).add(requestOverageFee);

        // 保存账单
        StorageBillingRecord record = StorageBillingRecord.builder()
                .tenantId(bucket.getTenantId())
                .tenantBucketId(bucket.getId())
                .billPeriod(billPeriod)
                .standardStorageGb(BigDecimal.valueOf(standardStorageGb))
                .lineStorageGb(BigDecimal.valueOf(lineStorageGb))
                .archiveStorageGb(BigDecimal.valueOf(archiveStorageGb))
                .standardFileCount(standardFileCount)
                .externalTrafficGb(BigDecimal.valueOf(externalTrafficGb))
                .cdnTrafficGb(BigDecimal.valueOf(cdnTrafficGb))
                .getRequests(getRequests)
                .putRequests(putRequests)
                .quotaStorageGb(BigDecimal.valueOf(quotaStorageGb))
                .quotaTrafficGb(BigDecimal.valueOf(quotaTrafficGb))
                .baseFee(baseFee)
                .storageOverageFee(storageOverageFee)
                .trafficOverageFee(trafficOverageFee)
                .requestOverageFee(requestOverageFee)
                .totalFee(totalFee)
                .billStatus("calculated")
                .calculatedAt(java.time.LocalDateTime.now())
                .build();

        billingRecordMapper.insert(record);

        return buildBillingResponse(record);
    }

    @Override
    @Transactional
    public int calculateAllMonthlyBills(String billPeriod) {
        if (billPeriod == null || billPeriod.isEmpty()) {
            billPeriod = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        List<TenantBucket> activeBuckets = tenantBucketMapper.selectList(
                new LambdaQueryWrapper<TenantBucket>().eq(TenantBucket::getStatus, "active"));

        int count = 0;
        for (TenantBucket bucket : activeBuckets) {
            try {
                StorageBillingRecord existing = billingRecordMapper.findByTenantBucketPeriod(
                        bucket.getTenantId(), bucket.getId(), billPeriod);
                if (existing == null) {
                    calculateBill(CalculateBillRequest.builder()
                            .tenantBucketId(bucket.getId())
                            .billPeriod(billPeriod)
                            .build());
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to calculate bill for bucket {}: {}", bucket.getId(), e.getMessage());
            }
        }

        log.info("Calculated {} monthly bills for period {}", count, billPeriod);
        return count;
    }

    // ========== 用量统计 ==========

    @Override
    public List<UsageSnapshotResponse> getUsageSnapshots(Long bucketId, String begin, String end) {
        LocalDate startDate = begin != null ? LocalDate.parse(begin, DATE_FMT) : LocalDate.now().minusDays(30);
        LocalDate endDate = end != null ? LocalDate.parse(end, DATE_FMT) : LocalDate.now();

        List<StorageUsageLog> logs = usageLogMapper.findByBucketIdAndDateRange(bucketId, startDate, endDate);
        return logs.stream().map(this::buildSnapshotResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void snapshotDailyUsage(Long bucketId) {
        TenantBucket bucket = tenantBucketMapper.selectById(bucketId);
        if (bucket == null) return;

        LocalDate today = LocalDate.now();
        StorageUsageLog existing = usageLogMapper.findByBucketIdAndDate(bucketId, today);
        if (existing != null) {
            log.debug("Usage snapshot already exists for bucket {} on {}", bucketId, today);
            return;
        }

        try {
            String begin = today.format(DATE_FMT);
            String end = today.format(DATE_FMT);

            long standardBytes = 0, lineBytes = 0, archiveBytes = 0;
            long standardCount = 0, lineCount = 0, archiveCount = 0;
            long externalFlux = 0, cdnFlux = 0;
            long getRequests = 0, putRequests = 0;

            try {
                JsonNode spaceData = fetchQiniuStatApi(bucket.getBucketName(), "space", begin, end, "day");
                JsonNode datas = spaceData.path("datas");
                if (datas.isArray() && !datas.isEmpty()) {
                    JsonNode latest = datas.get(datas.size() - 1);
                    if (latest.has("values")) {
                        Iterator<String> fields = latest.get("values").fieldNames();
                        if (fields.hasNext()) {
                            standardBytes = latest.get("values").get(fields.next()).asLong(0);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch space stat: {}", e.getMessage());
            }

            try {
                JsonNode blobIoData = fetchQiniuStatApi(bucket.getBucketName(), "blob_io", begin, end, "day");
                JsonNode ioDatas = blobIoData.path("datas");
                if (ioDatas.isArray() && !ioDatas.isEmpty()) {
                    JsonNode latest = ioDatas.get(ioDatas.size() - 1);
                    externalFlux = latest.path("flux").asLong(0);
                    cdnFlux = latest.path("cdn_flux").asLong(0);
                    getRequests = latest.path("get_count").asLong(0);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch blob_io stat: {}", e.getMessage());
            }

            StorageUsageLog logEntry = StorageUsageLog.builder()
                    .tenantId(bucket.getTenantId())
                    .tenantBucketId(bucketId)
                    .snapshotDate(today)
                    .standardStorageBytes(standardBytes)
                    .lineStorageBytes(lineBytes)
                    .archiveStorageBytes(archiveBytes)
                    .standardFileCount(standardCount)
                    .lineFileCount(lineCount)
                    .archiveFileCount(archiveCount)
                    .externalFluxBytes(externalFlux)
                    .cdnFluxBytes(cdnFlux)
                    .getRequests(getRequests)
                    .putRequests(putRequests)
                    .fetchStatus("success")
                    .build();

            usageLogMapper.insert(logEntry);
            log.info("Usage snapshot saved for bucket {} on {}", bucketId, today);

        } catch (Exception e) {
            log.error("Failed to snapshot usage for bucket {}: {}", bucketId, e.getMessage());
            // 保存失败记录
            StorageUsageLog failLog = StorageUsageLog.builder()
                    .tenantId(bucket.getTenantId())
                    .tenantBucketId(bucketId)
                    .snapshotDate(today)
                    .fetchStatus("failed")
                    .fetchError(e.getMessage())
                    .build();
            usageLogMapper.insert(failLog);
        }
    }

    @Override
    public int snapshotAllDailyUsage() {
        List<TenantBucket> activeBuckets = tenantBucketMapper.selectList(
                new LambdaQueryWrapper<TenantBucket>().eq(TenantBucket::getStatus, "active"));
        int count = 0;
        for (TenantBucket bucket : activeBuckets) {
            try {
                snapshotDailyUsage(bucket.getId());
                count++;
            } catch (Exception e) {
                log.error("Failed to snapshot usage for bucket {}: {}", bucket.getId(), e.getMessage());
            }
        }
        return count;
    }

    // ========== 平台概览 ==========

    @Override
    public PlatformStorageOverview getPlatformOverview() {
        long totalBuckets = tenantBucketMapper.selectCount(
                new LambdaQueryWrapper<TenantBucket>().ne(TenantBucket::getStatus, "deleted"));
        long activeBuckets = tenantBucketMapper.countActiveBuckets();
        long totalTenants = tenantBucketMapper.selectList(
                new LambdaQueryWrapper<TenantBucket>().eq(TenantBucket::getStatus, "active"))
                .stream().map(TenantBucket::getTenantId).distinct().count();
        long pendingBills = billingRecordMapper.countPendingBills();
        BigDecimal totalRevenue = billingRecordMapper.sumPaidFees();

        return PlatformStorageOverview.builder()
                .totalBuckets(totalBuckets)
                .activeBuckets(activeBuckets)
                .totalTenants(totalTenants)
                .totalStorageUsedGb(0.0) // 需要汇总计算
                .totalTrafficGb(0.0)
                .totalRevenue(totalRevenue)
                .pendingBills(pendingBills)
                .build();
    }

    // ========== 凭证管理 ==========

    @Override
    public CredentialsStatusResponse getCredentialsStatus() {
        String ak = systemConfigService.getConfigValue("oss", "access_key");
        String sk = systemConfigService.getConfigValue("oss", "secret_key");

        // Fallback to YAML
        if (ak == null || ak.isEmpty()) ak = defaultAccessKey;
        if (sk == null || sk.isEmpty()) sk = defaultSecretKey;

        boolean configured = ak != null && !ak.isEmpty() && sk != null && !sk.isEmpty();
        String maskedAk = "";
        if (ak != null && ak.length() > 6) {
            maskedAk = ak.substring(0, 6) + "****" + ak.substring(ak.length() - 4);
        } else if (ak != null && !ak.isEmpty()) {
            maskedAk = ak.substring(0, 2) + "****";
        }

        // 从system_config查更新时间
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemConfig> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigGroup, "oss")
                        .eq(SystemConfig::getConfigKey, "access_key");
        SystemConfig config = systemConfigService.getOne(wrapper);
        LocalDateTime lastUpdated = config != null ? config.getUpdatedAt() : null;

        return CredentialsStatusResponse.builder()
                .configured(configured)
                .hasAccessKey(ak != null && !ak.isEmpty())
                .hasSecretKey(sk != null && !sk.isEmpty())
                .maskedAccessKey(maskedAk)
                .lastUpdatedAt(lastUpdated)
                .build();
    }

    @Override
    public CredentialsTestResult testCredentials(String accessKey, String secretKey) {
        // 优先使用传入的凭证，否则fallback
        String ak = (accessKey != null && !accessKey.isEmpty()) ? accessKey : resolveAccessKey();
        String sk = (secretKey != null && !secretKey.isEmpty()) ? secretKey : resolveSecretKey();

        if (ak == null || ak.isEmpty() || sk == null || sk.isEmpty()) {
            return CredentialsTestResult.builder()
                    .success(false)
                    .message("AccessKey 或 SecretKey 未配置，请先填入凭证")
                    .buckets(Collections.emptyList())
                    .bucketCount(0)
                    .latencyMs(0)
                    .build();
        }

        long start = System.currentTimeMillis();
        try {
            String accessToken = buildQiniuAccessToken("GET", "/buckets", "uc.qiniuapi.com",
                    "application/x-www-form-urlencoded", null, ak, sk);

            java.net.URL url = new java.net.URL(QINIU_UC_API + "/buckets");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", accessToken);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;

            if (code == 200) {
                String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode arr = objectMapper.readTree(body);
                List<String> buckets = new ArrayList<>();
                if (arr.isArray()) {
                    for (JsonNode node : arr) {
                        buckets.add(node.asText());
                    }
                }
                return CredentialsTestResult.builder()
                        .success(true)
                        .message("连接成功！七牛云账号下共有 " + buckets.size() + " 个 Bucket")
                        .buckets(buckets)
                        .bucketCount(buckets.size())
                        .latencyMs(latency)
                        .build();
            } else {
                String error = conn.getErrorStream() != null
                        ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + code;
                return CredentialsTestResult.builder()
                        .success(false)
                        .message("认证失败（HTTP " + code + "）：" + error)
                        .buckets(Collections.emptyList())
                        .bucketCount(0)
                        .latencyMs(latency)
                        .build();
            }
        } catch (java.net.UnknownHostException e) {
            long latency = System.currentTimeMillis() - start;
            return CredentialsTestResult.builder()
                    .success(false)
                    .message("网络连接失败：无法访问七牛云API（uc.qiniuapi.com），请检查网络/DNS")
                    .buckets(Collections.emptyList())
                    .bucketCount(0)
                    .latencyMs(latency)
                    .build();
        } catch (java.net.SocketTimeoutException e) {
            long latency = System.currentTimeMillis() - start;
            return CredentialsTestResult.builder()
                    .success(false)
                    .message("连接超时：七牛云API响应过慢，请重试")
                    .buckets(Collections.emptyList())
                    .bucketCount(0)
                    .latencyMs(latency)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return CredentialsTestResult.builder()
                    .success(false)
                    .message("测试失败：" + e.getMessage())
                    .buckets(Collections.emptyList())
                    .bucketCount(0)
                    .latencyMs(latency)
                    .build();
        }
    }

    @Override
    @Transactional
    public void saveCredentials(CredentialsRequest request) {
        if (request.getAccessKey() == null || request.getAccessKey().isEmpty()
                || request.getSecretKey() == null || request.getSecretKey().isEmpty()) {
            throw new RuntimeException("AccessKey 和 SecretKey 不能为空");
        }

        // 先测试凭证可用性
        CredentialsTestResult testResult = testCredentials(request.getAccessKey(), request.getSecretKey());
        if (!testResult.isSuccess()) {
            throw new RuntimeException("凭证验证失败，无法保存：" + testResult.getMessage());
        }

        // 保存AccessKey
        systemConfigService.saveOrUpdateConfig(SystemConfigDtos.SaveConfigRequest.builder()
                .configGroup("oss")
                .configKey("access_key")
                .configValue(request.getAccessKey())
                .enabled(true)
                .sortOrder(1)
                .description("七牛云对象存储 - AccessKey")
                .build());

        // 保存SecretKey
        systemConfigService.saveOrUpdateConfig(SystemConfigDtos.SaveConfigRequest.builder()
                .configGroup("oss")
                .configKey("secret_key")
                .configValue(request.getSecretKey())
                .enabled(true)
                .sortOrder(2)
                .description("七牛云对象存储 - SecretKey")
                .build());

        log.info("Qiniu credentials saved successfully");
    }

    @Override
    public List<String> listQiniuBuckets() {
        try {
            String accessKey = resolveAccessKey();
            String secretKey = resolveSecretKey();
            String accessToken = buildQiniuAccessToken("GET", "/buckets", "uc.qiniuapi.com",
                    "application/x-www-form-urlencoded", null, accessKey, secretKey);

            java.net.URL url = new java.net.URL(QINIU_UC_API + "/buckets");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", accessToken);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            int code = conn.getResponseCode();
            if (code == 200) {
                String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode arr = objectMapper.readTree(body);
                List<String> buckets = new ArrayList<>();
                if (arr.isArray()) {
                    for (JsonNode node : arr) {
                        buckets.add(node.asText());
                    }
                }
                return buckets;
            } else {
                String error = conn.getErrorStream() != null
                        ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) : "";
                throw new RuntimeException("Failed to list Qiniu buckets: HTTP " + code + " " + error);
            }
        } catch (Exception e) {
            log.error("Failed to list Qiniu buckets", e);
            return Collections.emptyList();
        }
    }

    // ========== 七牛云 API 调用 ==========

    private void createQiniuBucket(String bucketName, String region, String accessKey, String secretKey) throws Exception {
        String path = "/mkbucketv3/" + bucketName + "/region/" + region;
        String accessToken = buildQiniuAccessTokenPost("POST", path, "uc.qiniuapi.com", accessKey, secretKey);

        java.net.URL url = new java.net.URL(QINIU_UC_API + path);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", accessToken);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        int code = conn.getResponseCode();
        if (code != 200 && code != 614) { // 614 = bucket already exists
            String error = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) : "";
            throw new RuntimeException("创建Bucket失败: HTTP " + code + " " + error);
        }
        log.info("Qiniu bucket created/confirmed: {}", bucketName);
    }

    private void dropQiniuBucket(String bucketName, String accessKey, String secretKey) throws Exception {
        String path = "/drop/" + bucketName;
        String accessToken = buildQiniuAccessToken("POST", path, "uc.qiniuapi.com", accessKey, secretKey);

        java.net.URL url = new java.net.URL(QINIU_UC_API + path);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", accessToken);

        int code = conn.getResponseCode();
        if (code != 200) {
            String error = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) : "";
            throw new RuntimeException("删除Bucket失败: HTTP " + code + " " + error);
        }
        log.info("Qiniu bucket deleted: {}", bucketName);
    }

    private List<String> fetchQiniuBucketDomains(String bucketName, String accessKey, String secretKey) throws Exception {
        String path = "/v2/domains";
        String query = "tbl=" + URLEncoder.encode(bucketName, StandardCharsets.UTF_8);
        String pathAndQuery = path + "?" + query;
        String accessToken = buildQiniuAccessToken("GET", pathAndQuery, "uc.qiniuapi.com", accessKey, secretKey);

        java.net.URL url = new java.net.URL(QINIU_UC_API + path + "?" + query);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", accessToken);

        int code = conn.getResponseCode();
        if (code == 200) {
            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode arr = objectMapper.readTree(body);
            List<String> domains = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    domains.add(node.asText());
                }
            }
            return domains;
        }
        return Collections.emptyList();
    }

    private JsonNode fetchQiniuStatApi(String bucketName, String statType, String begin, String end, String granularity)
            throws Exception {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();

        long beginTs = LocalDate.parse(begin, DATE_FMT).atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.ofHours(8));
        long endTs = LocalDate.parse(end, DATE_FMT).plusDays(1).atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.ofHours(8));

        String path = "/v6/" + statType;
        String query = "begin=" + beginTs + "&end=" + endTs
                + "&bucket=" + URLEncoder.encode(bucketName, StandardCharsets.UTF_8)
                + "&g=" + granularity;

        String pathAndQuery = path + "?" + query;
        String accessToken = buildQiniuAccessToken("GET", pathAndQuery, "api.qiniu.com", accessKey, secretKey);

        java.net.URL url = new java.net.URL(QINIU_API_BASE + "/" + statType + "?" + query);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        int code = conn.getResponseCode();
        if (code == 200) {
            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readTree(body);
        } else {
            String error = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + code;
            throw new RuntimeException("七牛云统计API调用失败: " + error);
        }
    }

    // ========== 辅助方法 ==========

    private String resolveAccessKey() {
        String dbVal = systemConfigService.getConfigValue("oss", "access_key");
        return (dbVal != null && !dbVal.isEmpty()) ? dbVal : defaultAccessKey;
    }

    private String resolveSecretKey() {
        String dbVal = systemConfigService.getConfigValue("oss", "secret_key");
        return (dbVal != null && !dbVal.isEmpty()) ? dbVal : defaultSecretKey;
    }

    private TenantBucketResponse buildBucketResponse(TenantBucket bucket) {
        TenantStoragePlan tsp = tenantStoragePlanMapper.findActiveByBucketId(bucket.getId());
        StoragePlan plan = tsp != null ? storagePlanMapper.selectById(tsp.getPlanId()) : null;

        String tenantName = "租户" + bucket.getTenantId(); // 可扩展查tenant表

        return TenantBucketResponse.builder()
                .id(bucket.getId())
                .tenantId(bucket.getTenantId())
                .tenantName(tenantName)
                .bucketName(bucket.getBucketName())
                .bucketRegion(bucket.getBucketRegion())
                .bucketRegionLabel(getRegionLabel(bucket.getBucketRegion()))
                .bucketDomain(bucket.getBucketDomain())
                .bucketPrivate(bucket.getBucketPrivate())
                .status(bucket.getStatus())
                .notes(bucket.getNotes())
                .planName(plan != null ? plan.getPlanName() : null)
                .planCode(plan != null ? plan.getPlanCode() : null)
                .storageQuotaGb(plan != null ? plan.getStorageQuotaGb() : 10.0)
                .storageUsedGb(0.0) // 可通过七牛云实时查询
                .monthlyTrafficGb(plan != null ? plan.getMonthlyTrafficGb() : 100.0)
                .trafficUsedGb(0.0)
                .createdAt(bucket.getCreatedAt())
                .updatedAt(bucket.getUpdatedAt())
                .build();
    }

    private BillingRecordResponse buildBillingResponse(StorageBillingRecord record) {
        TenantBucket bucket = tenantBucketMapper.selectById(record.getTenantBucketId());
        return BillingRecordResponse.builder()
                .id(record.getId())
                .tenantId(record.getTenantId())
                .tenantName("租户" + record.getTenantId())
                .tenantBucketId(record.getTenantBucketId())
                .bucketName(bucket != null ? bucket.getBucketName() : "未知")
                .billPeriod(record.getBillPeriod())
                .standardStorageGb(record.getStandardStorageGb())
                .lineStorageGb(record.getLineStorageGb())
                .archiveStorageGb(record.getArchiveStorageGb())
                .externalTrafficGb(record.getExternalTrafficGb())
                .cdnTrafficGb(record.getCdnTrafficGb())
                .getRequests(record.getGetRequests())
                .putRequests(record.getPutRequests())
                .quotaStorageGb(record.getQuotaStorageGb())
                .quotaTrafficGb(record.getQuotaTrafficGb())
                .baseFee(record.getBaseFee())
                .storageOverageFee(record.getStorageOverageFee())
                .trafficOverageFee(record.getTrafficOverageFee())
                .requestOverageFee(record.getRequestOverageFee())
                .totalFee(record.getTotalFee())
                .billStatus(record.getBillStatus())
                .calculatedAt(record.getCalculatedAt())
                .paidAt(record.getPaidAt())
                .build();
    }

    private UsageSnapshotResponse buildSnapshotResponse(StorageUsageLog log) {
        return UsageSnapshotResponse.builder()
                .id(log.getId())
                .snapshotDate(log.getSnapshotDate())
                .standardStorageGb(log.getStandardStorageBytes() / BYTES_PER_GB)
                .lineStorageGb(log.getLineStorageBytes() / BYTES_PER_GB)
                .archiveStorageGb(log.getArchiveStorageBytes() / BYTES_PER_GB)
                .standardFileCount(log.getStandardFileCount())
                .externalTrafficGb(log.getExternalFluxBytes() / BYTES_PER_GB)
                .cdnTrafficGb(log.getCdnFluxBytes() / BYTES_PER_GB)
                .getRequests(log.getGetRequests())
                .putRequests(log.getPutRequests())
                .fetchStatus(log.getFetchStatus())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(featuresJson, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String getRegionLabel(String region) {
        return switch (region) {
            case "z0" -> "华东";
            case "z1" -> "华北";
            case "z2" -> "华南";
            case "na0" -> "北美";
            case "as0" -> "东南亚";
            default -> region;
        };
    }

    /**
     * 构建七牛云管理凭证（Qiniu Access Token）
     * 签名串格式（七牛云官方规范）：
     *   <Method> <Path>[?<Query>]\nHost: <Host>[\nContent-Type: <type>]\n\n[<Body>]
     * 签名方式：HMAC-SHA1 + URL安全Base64编码
     */
    private String buildQiniuAccessToken(String method, String pathAndQuery, String host,
                                         String contentType, String body, String accessKey, String secretKey) {
        StringBuilder signingStr = new StringBuilder();
        signingStr.append(method).append(" ").append(pathAndQuery).append("\n");
        signingStr.append("Host: ").append(host);

        if (contentType != null && !contentType.isEmpty()) {
            signingStr.append("\n").append("Content-Type: ").append(contentType);
        }

        signingStr.append("\n\n");

        if (body != null && !body.isEmpty() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            signingStr.append(body);
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            String sign = Base64.getUrlEncoder()
                    .encodeToString(mac.doFinal(signingStr.toString().getBytes(StandardCharsets.UTF_8)));
            return "Qiniu " + accessKey + ":" + sign;
        } catch (Exception e) {
            throw new RuntimeException("七牛云签名构建失败", e);
        }
    }

    /** 无请求体的 GET/DELETE 请求（无 Content-Type） */
    private String buildQiniuAccessToken(String method, String pathAndQuery, String host,
                                         String accessKey, String secretKey) {
        return buildQiniuAccessToken(method, pathAndQuery, host, null, null, accessKey, secretKey);
    }

    /** 无请求体的 POST/PUT 请求（带 Content-Type: application/x-www-form-urlencoded） */
    private String buildQiniuAccessTokenPost(String method, String pathAndQuery, String host,
                                             String accessKey, String secretKey) {
        return buildQiniuAccessToken(method, pathAndQuery, host,
                "application/x-www-form-urlencoded", null, accessKey, secretKey);
    }

    // ========== 新租户自动分配存储空间 ==========

    @Override
    @Transactional
    public TenantBucketResponse autoAssignBucketForNewTenant(Long tenantId, String tenantCode) {
        // 1. 检查七牛云凭证是否已配置
        CredentialsStatusResponse credStatus = getCredentialsStatus();
        if (!credStatus.isConfigured()) {
            log.info("Qiniu credentials not configured, skip auto bucket creation for new tenant: {}", tenantId);
            return null;
        }

        // 2. 查找默认套餐：选择 plan_level 最小的启用套餐（通常是免费版）
        List<StoragePlan> plans = storagePlanMapper.findAllActive();
        StoragePlan defaultPlan = plans.stream()
                .min(Comparator.comparingInt(StoragePlan::getPlanLevel))
                .orElse(null);
        if (defaultPlan == null) {
            log.warn("No active storage plan found, skip auto bucket creation for new tenant: {}", tenantId);
            return null;
        }

        // 3. 自动生成 Bucket 名称：{tenantCode}-storage（全小写，符合七牛云规范）
        String bucketName = generateBucketName(tenantCode);

        // 4. 创建 Bucket 并绑定默认套餐
        try {
            CreateBucketRequest request = CreateBucketRequest.builder()
                    .tenantId(tenantId)
                    .bucketName(bucketName)
                    .bucketRegion("z0")       // 默认华东区域
                    .bucketPrivate(false)      // 默认公开空间
                    .planId(defaultPlan.getId())
                    .notes("注册时自动分配（" + defaultPlan.getPlanName() + "）")
                    .build();

            TenantBucketResponse result = createBucket(request);
            log.info("Auto-assigned bucket for new tenant: tenantId={}, bucketName={}, plan={}, quota={}GB",
                    tenantId, bucketName, defaultPlan.getPlanName(), defaultPlan.getStorageQuotaGb());
            return result;
        } catch (Exception e) {
            log.error("Failed to auto-assign bucket for new tenant {}: {}", tenantId, e.getMessage());
            return null;
        }
    }

    /**
     * 根据租户编码生成七牛云 Bucket 名称
     * 规则：{tenantCode}-storage，全小写，去除非字母数字字符
     * 七牛云要求：3-63 个字符，仅允许小写字母、数字和连字符
     */
    private String generateBucketName(String tenantCode) {
        String base = tenantCode.toLowerCase().replaceAll("[^a-z0-9]", "");
        // 限制长度，确保加上 "-storage" 后不超过 63 字符
        if (base.length() > 54) {
            base = base.substring(0, 54);
        }
        return base + "-storage";
    }
}
