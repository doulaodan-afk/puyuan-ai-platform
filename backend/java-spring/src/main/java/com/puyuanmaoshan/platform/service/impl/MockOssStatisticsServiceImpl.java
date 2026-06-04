package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.dto.OssStatisticDtos.*;
import com.puyuanmaoshan.platform.service.OssStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * OSS 统计服务 Mock 实现
 * 当 app.storage.mock.enabled=true 时使用，返回模拟数据
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockOssStatisticsServiceImpl implements OssStatisticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public StorageOverviewResponse getStorageOverview(String begin, String end) {
        if (begin == null || begin.isEmpty()) begin = LocalDate.now().minusDays(30).format(DATE_FMT);
        if (end == null || end.isEmpty()) end = LocalDate.now().format(DATE_FMT);

        log.info("Mock OSS statistics overview: {} ~ {}", begin, end);

        return StorageOverviewResponse.builder()
                .standardSpace(1024L * 1024 * 512)     // 0.5 GB
                .standardCount(128)
                .lineSpace(1024L * 1024 * 256)          // 0.25 GB
                .lineCount(32)
                .archiveSpace(0)
                .archiveCount(0)
                .blobIoFlux(1024L * 1024 * 1024)        // 1 GB
                .cdnFlux(1024L * 1024 * 512)
                .getCount(2560)
                .putCount(128)
                .standardSpaceGb(0.5)
                .lineSpaceGb(0.25)
                .archiveSpaceGb(0.0)
                .blobIoFluxGb(1.0)
                .cdnFluxGb(0.5)
                .queryRange(begin + " ~ " + end)
                .bucket("mock-bucket")
                .build();
    }

    @Override
    public StatisticTypeResponse getStatistic(String statType, String begin, String end, String granularity) {
        if (begin == null || begin.isEmpty()) begin = LocalDate.now().minusDays(30).format(DATE_FMT);
        if (end == null || end.isEmpty()) end = LocalDate.now().format(DATE_FMT);
        if (granularity == null || granularity.isEmpty()) granularity = "day";

        log.info("Mock OSS statistic: type={}, {} ~ {}, granularity={}", statType, begin, end, granularity);

        List<StatisticDataPoint> datas = new ArrayList<>();
        LocalDate start = LocalDate.parse(begin, DATE_FMT);
        LocalDate finish = LocalDate.parse(end, DATE_FMT);

        long mockValue = switch (statType) {
            case "space", "space_line", "space_archive", "space_deep_archive", "space_archive_ir",
                 "space_intelligent_tiering" -> 1024L * 1024 * 512;
            case "count", "count_line", "count_archive", "count_deep_archive", "count_archive_ir",
                 "count_intelligent_tiering", "count_intelligent_tiering_monitor" -> 128L;
            case "blob_transfer" -> 1024L * 1024 * 256;
            case "rs_chtype", "rs_put" -> 64L;
            default -> 0L;
        };

        for (LocalDate d = start; !d.isAfter(finish); d = d.plusDays(1)) {
            // 模拟数据随时间缓慢增长
            long daysDiff = d.toEpochDay() - start.toEpochDay();
            long value = mockValue + (daysDiff * 1024);
            datas.add(StatisticDataPoint.builder()
                    .time(d.format(DATE_FMT))
                    .value(value)
                    .build());
        }

        String label = getLabel(statType);
        String unit = getUnit(statType);

        return StatisticTypeResponse.builder()
                .statType(statType)
                .statLabel(label)
                .unit(unit)
                .datas(datas)
                .build();
    }

    @Override
    public BlobIoStatisticResponse getBlobIoStatistic(String begin, String end, String granularity) {
        if (begin == null || begin.isEmpty()) begin = LocalDate.now().minusDays(30).format(DATE_FMT);
        if (end == null || end.isEmpty()) end = LocalDate.now().format(DATE_FMT);

        log.info("Mock OSS blob_io statistic: {} ~ {}", begin, end);

        List<BlobIoDataPoint> datas = new ArrayList<>();
        LocalDate start = LocalDate.parse(begin, DATE_FMT);
        LocalDate finish = LocalDate.parse(end, DATE_FMT);

        for (LocalDate d = start; !d.isAfter(finish); d = d.plusDays(1)) {
            long daysDiff = d.toEpochDay() - start.toEpochDay();
            datas.add(BlobIoDataPoint.builder()
                    .time(d.format(DATE_FMT))
                    .flux(1024L * 1024 * 64 + daysDiff * 1024)
                    .cdnFlux(1024L * 1024 * 32 + daysDiff * 512)
                    .readBytes(1024L * 1024 * 48 + daysDiff * 768)
                    .getCount(80 + daysDiff)
                    .build());
        }

        return BlobIoStatisticResponse.builder()
                .datas(datas)
                .bucket("mock-bucket")
                .queryRange(begin + " ~ " + end)
                .build();
    }

    @Override
    public FullStatisticResponse getFullStatistics(String begin, String end, String granularity) {
        StorageOverviewResponse overview = getStorageOverview(begin, end);

        Map<String, StatisticTypeResponse> details = new LinkedHashMap<>();
        for (String statType : List.of("space", "count", "space_line", "count_line",
                "space_archive", "count_archive", "rs_put", "rs_chtype")) {
            details.put(statType, getStatistic(statType, begin, end, granularity));
        }

        BlobIoStatisticResponse blobIo = getBlobIoStatistic(begin, end, granularity);

        return FullStatisticResponse.builder()
                .overview(overview)
                .details(details)
                .blobIo(blobIo)
                .build();
    }

    @Override
    public List<Map<String, String>> getSupportedStatTypes() {
        List<Map<String, String>> result = new ArrayList<>();
        String[][] types = {
                {"space", "标准存储量", "bytes"},
                {"count", "标准文件数", "个"},
                {"space_line", "低频存储量", "bytes"},
                {"count_line", "低频文件数", "个"},
                {"space_intelligent_tiering", "智能分层存储量", "bytes"},
                {"count_intelligent_tiering", "智能分层文件数", "个"},
                {"count_intelligent_tiering_monitor", "智能分层监控文件数", "个"},
                {"space_archive_ir", "归档直读存储量", "bytes"},
                {"count_archive_ir", "归档直读文件数", "个"},
                {"space_archive", "归档存储量", "bytes"},
                {"count_archive", "归档文件数", "个"},
                {"space_deep_archive", "深度归档存储量", "bytes"},
                {"count_deep_archive", "深度归档文件数", "个"},
                {"blob_transfer", "跨区域同步流量", "bytes"},
                {"rs_chtype", "存储类型请求次数", "次"},
                {"blob_io", "外网/CDN流量+GET次数", "bytes"},
                {"rs_put", "PUT请求次数", "次"},
        };
        for (String[] t : types) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("stat_type", t[0]);
            item.put("stat_label", t[1]);
            item.put("unit", t[2]);
            result.add(item);
        }
        return result;
    }

    private String getLabel(String statType) {
        return switch (statType) {
            case "space" -> "标准存储量";
            case "count" -> "标准文件数";
            case "space_line" -> "低频存储量";
            case "count_line" -> "低频文件数";
            case "space_archive" -> "归档存储量";
            case "count_archive" -> "归档文件数";
            case "space_deep_archive" -> "深度归档存储量";
            case "count_deep_archive" -> "深度归档文件数";
            case "blob_transfer" -> "跨区域同步流量";
            case "rs_chtype" -> "存储类型请求次数";
            case "blob_io" -> "外网/CDN流量+GET次数";
            case "rs_put" -> "PUT请求次数";
            default -> statType;
        };
    }

    private String getUnit(String statType) {
        return switch (statType) {
            case "space", "space_line", "space_archive", "space_deep_archive",
                 "space_archive_ir", "space_intelligent_tiering",
                 "blob_transfer", "blob_io" -> "bytes";
            case "rs_chtype", "rs_put" -> "次";
            default -> "个";
        };
    }
}
