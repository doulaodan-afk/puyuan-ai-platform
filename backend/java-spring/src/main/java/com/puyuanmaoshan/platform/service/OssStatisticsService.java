package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.OssStatisticDtos.*;

import java.util.List;
import java.util.Map;

/**
 * OSS 存储统计服务接口
 * 基于七牛云 Kodo 统计接口: https://developer.qiniu.com/kodo/3906/statistic-interface
 */
public interface OssStatisticsService {

    /**
     * 获取存储概览（聚合当前值）
     * @param begin 起始日期 yyyy-MM-dd
     * @param end 结束日期 yyyy-MM-dd
     * @return 存储概览响应
     */
    StorageOverviewResponse getStorageOverview(String begin, String end);

    /**
     * 获取指定统计类型的时序数据
     * @param statType 统计类型 (space/count/space_line/count_line/space_archive/count_archive/blob_io/rs_put/rs_chtype 等)
     * @param begin 起始日期 yyyy-MM-dd
     * @param end 结束日期 yyyy-MM-dd
     * @param granularity 时间粒度 (day/month)
     * @return 统计类型响应
     */
    StatisticTypeResponse getStatistic(String statType, String begin, String end, String granularity);

    /**
     * 获取 blob_io 统计（外网流出流量、CDN回源流量、GET请求次数）
     * @param begin 起始日期 yyyy-MM-dd
     * @param end 结束日期 yyyy-MM-dd
     * @param granularity 时间粒度 (day/month)
     * @return blob_io 统计响应
     */
    BlobIoStatisticResponse getBlobIoStatistic(String begin, String end, String granularity);

    /**
     * 获取完整统计（概览 + 各类型详情 + blob_io）
     * @param begin 起始日期 yyyy-MM-dd
     * @param end 结束日期 yyyy-MM-dd
     * @param granularity 时间粒度 (day/month)
     * @return 完整统计响应
     */
    FullStatisticResponse getFullStatistics(String begin, String end, String granularity);

    /**
     * 获取所有支持的统计类型列表
     * @return 统计类型列表
     */
    List<Map<String, String>> getSupportedStatTypes();
}