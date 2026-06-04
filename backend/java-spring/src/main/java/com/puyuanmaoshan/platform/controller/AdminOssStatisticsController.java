package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.OssStatisticDtos.*;
import com.puyuanmaoshan.platform.service.OssStatisticsService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OSS 存储统计管理控制器
 * 基于七牛云 Kodo 统计接口: https://developer.qiniu.com/kodo/3906/statistic-interface
 */
@RestController
@RequestMapping("/api/v1/admin/oss-statistics")
@RequiredArgsConstructor
public class AdminOssStatisticsController {

    private final OssStatisticsService ossStatisticsService;

    /**
     * 获取存储概览（聚合当前值）
     */
    @GetMapping("/overview")
    public ApiResponse<StorageOverviewResponse> overview(
            @RequestParam(name = "begin", required = false) String begin,
            @RequestParam(name = "end", required = false) String end,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        StorageOverviewResponse data = ossStatisticsService.getStorageOverview(begin, end);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-oss-stat-overview"));
    }

    /**
     * 获取指定统计类型的时序数据
     */
    @GetMapping("/stat")
    public ApiResponse<StatisticTypeResponse> stat(
            @RequestParam(name = "stat_type") String statType,
            @RequestParam(name = "begin", required = false) String begin,
            @RequestParam(name = "end", required = false) String end,
            @RequestParam(name = "granularity", defaultValue = "day") String granularity,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        StatisticTypeResponse data = ossStatisticsService.getStatistic(statType, begin, end, granularity);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-oss-stat-detail"));
    }

    /**
     * 获取 blob_io 统计（外网流出流量、CDN回源流量、GET请求次数）
     */
    @GetMapping("/blob-io")
    public ApiResponse<BlobIoStatisticResponse> blobIo(
            @RequestParam(name = "begin", required = false) String begin,
            @RequestParam(name = "end", required = false) String end,
            @RequestParam(name = "granularity", defaultValue = "day") String granularity,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        BlobIoStatisticResponse data = ossStatisticsService.getBlobIoStatistic(begin, end, granularity);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-oss-stat-blobio"));
    }

    /**
     * 获取完整统计（概览 + 各类型详情 + blob_io）
     */
    @GetMapping("/full")
    public ApiResponse<FullStatisticResponse> full(
            @RequestParam(name = "begin", required = false) String begin,
            @RequestParam(name = "end", required = false) String end,
            @RequestParam(name = "granularity", defaultValue = "day") String granularity,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        FullStatisticResponse data = ossStatisticsService.getFullStatistics(begin, end, granularity);
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-oss-stat-full"));
    }

    /**
     * 获取所有支持的统计类型列表
     */
    @GetMapping("/types")
    public ApiResponse<List<Map<String, String>>> types(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        List<Map<String, String>> data = ossStatisticsService.getSupportedStatTypes();
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-admin-oss-stat-types"));
    }
}