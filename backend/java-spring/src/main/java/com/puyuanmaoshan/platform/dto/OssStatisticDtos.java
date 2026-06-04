package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OSS 存储统计相关 DTO
 * 基于七牛云 Kodo 统计接口: https://developer.qiniu.com/kodo/3906/statistic-interface
 */
public final class OssStatisticDtos {
    private OssStatisticDtos() {}

    /**
     * 统计查询请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticQueryRequest {
        /** 起始日期，格式 yyyy-MM-dd，默认30天前 */
        @JsonProperty("begin")
        private String begin;

        /** 结束日期，格式 yyyy-MM-dd，默认今天 */
        @JsonProperty("end")
        private String end;

        /** 时间粒度: day 或 month，默认 day */
        @JsonProperty("granularity")
        private String granularity;

        /** 统计类型，默认返回所有类型概览 */
        @JsonProperty("stat_type")
        private String statType;
    }

    /**
     * 单个时间点的统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticDataPoint {
        /** 时间点（日期字符串或时间戳） */
        @JsonProperty("time")
        private String time;

        /** 数值 */
        @JsonProperty("value")
        private long value;
    }

    /**
     * 单个统计类型的响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticTypeResponse {
        /** 统计类型名称 */
        @JsonProperty("stat_type")
        private String statType;

        /** 统计类型中文说明 */
        @JsonProperty("stat_label")
        private String statLabel;

        /** 数据单位 */
        @JsonProperty("unit")
        private String unit;

        /** 时间序列数据 */
        @JsonProperty("datas")
        private List<StatisticDataPoint> datas;
    }

    /**
     * 存储概览响应（聚合多种统计类型的当前值）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageOverviewResponse {
        /** 标准存储量（字节） */
        @JsonProperty("standard_space")
        private long standardSpace;

        /** 标准文件数 */
        @JsonProperty("standard_count")
        private long standardCount;

        /** 低频存储量（字节） */
        @JsonProperty("line_space")
        private long lineSpace;

        /** 低频文件数 */
        @JsonProperty("line_count")
        private long lineCount;

        /** 归档存储量（字节） */
        @JsonProperty("archive_space")
        private long archiveSpace;

        /** 归档文件数 */
        @JsonProperty("archive_count")
        private long archiveCount;

        /** 外网流出流量（字节） */
        @JsonProperty("blob_io_flux")
        private long blobIoFlux;

        /** CDN回源流量（字节） */
        @JsonProperty("cdn_flux")
        private long cdnFlux;

        /** GET 请求次数 */
        @JsonProperty("get_count")
        private long getCount;

        /** PUT 请求次数 */
        @JsonProperty("put_count")
        private long putCount;

        /** 标准存储量（GB，便于展示） */
        @JsonProperty("standard_space_gb")
        private double standardSpaceGb;

        /** 低频存储量（GB） */
        @JsonProperty("line_space_gb")
        private double lineSpaceGb;

        /** 归档存储量（GB） */
        @JsonProperty("archive_space_gb")
        private double archiveSpaceGb;

        /** 外网流出流量（GB） */
        @JsonProperty("blob_io_flux_gb")
        private double blobIoFluxGb;

        /** CDN回源流量（GB） */
        @JsonProperty("cdn_flux_gb")
        private double cdnFluxGb;

        /** 查询时间范围 */
        @JsonProperty("query_range")
        private String queryRange;

        /** Bucket 名称 */
        @JsonProperty("bucket")
        private String bucket;
    }

    /**
     * blob_io 接口返回的复合数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlobIoDataPoint {
        @JsonProperty("time")
        private String time;

        /** 外网流出流量（字节） */
        @JsonProperty("flux")
        private long flux;

        /** CDN回源流出流量（字节） */
        @JsonProperty("cdn_flux")
        private long cdnFlux;

        /** 数据读取量（字节） */
        @JsonProperty("read_bytes")
        private long readBytes;

        /** GET 请求次数 */
        @JsonProperty("get_count")
        private long getCount;
    }

    /**
     * blob_io 统计响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlobIoStatisticResponse {
        @JsonProperty("datas")
        private List<BlobIoDataPoint> datas;

        @JsonProperty("bucket")
        private String bucket;

        @JsonProperty("query_range")
        private String queryRange;
    }

    /**
     * 完整统计响应（包含所有统计类型）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullStatisticResponse {
        @JsonProperty("overview")
        private StorageOverviewResponse overview;

        @JsonProperty("details")
        private Map<String, StatisticTypeResponse> details;

        @JsonProperty("blob_io")
        private BlobIoStatisticResponse blobIo;
    }
}
