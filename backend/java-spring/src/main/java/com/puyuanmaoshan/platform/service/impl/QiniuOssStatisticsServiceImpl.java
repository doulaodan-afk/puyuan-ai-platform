package com.puyuanmaoshan.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.OssStatisticDtos.*;
import com.puyuanmaoshan.platform.service.OssStatisticsService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "false")
public class QiniuOssStatisticsServiceImpl implements OssStatisticsService {

    private static final String QINIU_API_BASE = "https://api.qiniu.com/v6";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.storage.qiniu.access-key:}")
    private String defaultAccessKey;

    @Value("${app.storage.qiniu.secret-key:}")
    private String defaultSecretKey;

    @Value("${app.storage.qiniu.bucket:puyuanmaoshan}")
    private String defaultBucket;

    private final SystemConfigService systemConfigService;

    public QiniuOssStatisticsServiceImpl(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    private String resolveAccessKey() {
        String dbVal = systemConfigService.getConfigValue("oss", "access_key");
        return (dbVal != null && !dbVal.isEmpty()) ? dbVal : defaultAccessKey;
    }

    private String resolveSecretKey() {
        String dbVal = systemConfigService.getConfigValue("oss", "secret_key");
        return (dbVal != null && !dbVal.isEmpty()) ? dbVal : defaultSecretKey;
    }

    private String resolveBucket() {
        String dbVal = systemConfigService.getConfigValue("oss", "bucket");
        return (dbVal != null && !dbVal.isEmpty()) ? dbVal : defaultBucket;
    }

    /** 统计类型 → 中文标签 */
    private static final Map<String, String> STAT_LABELS = new LinkedHashMap<>();
    /** 统计类型 → 数据单位 */
    private static final Map<String, String> STAT_UNITS = new LinkedHashMap<>();

    static {
        STAT_LABELS.put("space", "标准存储量");           STAT_UNITS.put("space", "bytes");
        STAT_LABELS.put("count", "标准文件数");           STAT_UNITS.put("count", "个");
        STAT_LABELS.put("space_line", "低频存储量");      STAT_UNITS.put("space_line", "bytes");
        STAT_LABELS.put("count_line", "低频文件数");      STAT_UNITS.put("count_line", "个");
        STAT_LABELS.put("space_archive", "归档存储量");   STAT_UNITS.put("space_archive", "bytes");
        STAT_LABELS.put("count_archive", "归档文件数");   STAT_UNITS.put("count_archive", "个");
        STAT_LABELS.put("space_deep_archive", "深度归档存储量"); STAT_UNITS.put("space_deep_archive", "bytes");
        STAT_LABELS.put("count_deep_archive", "深度归档文件数"); STAT_UNITS.put("count_deep_archive", "个");
        STAT_LABELS.put("blob_transfer", "跨区域同步流量"); STAT_UNITS.put("blob_transfer", "bytes");
        STAT_LABELS.put("rs_chtype", "存储类型请求次数"); STAT_UNITS.put("rs_chtype", "次");
        STAT_LABELS.put("blob_io", "外网/CDN流量+GET次数"); STAT_UNITS.put("blob_io", "bytes");
        STAT_LABELS.put("rs_put", "PUT请求次数");         STAT_UNITS.put("rs_put", "次");
    }

    @Override
    public StorageOverviewResponse getStorageOverview(String begin, String end) {
        if (begin == null || begin.isEmpty()) begin = LocalDate.now().minusDays(30).format(DATE_FMT);
        if (end == null || end.isEmpty()) end = LocalDate.now().format(DATE_FMT);
        String queryRange = begin + " ~ " + end;

        long standardSpace = getLatestValue("space", begin, end);
        long standardCount = getLatestValue("count", begin, end);
        long lineSpace = getLatestValue("space_line", begin, end);
        long lineCount = getLatestValue("count_line", begin, end);
        long archiveSpace = getLatestValue("space_archive", begin, end);
        long archiveCount = getLatestValue("count_archive", begin, end);

        long blobIoFlux = 0, cdnFlux = 0, getCount = 0;
        try {
            JsonNode blobIoData = fetchQiniuStatApi("blob_io", begin, end, "day");
            JsonNode datas = blobIoData.path("datas");
            if (datas.isArray() && !datas.isEmpty()) {
                JsonNode latest = datas.get(datas.size() - 1);
                blobIoFlux = latest.path("flux").asLong(0);
                cdnFlux = latest.path("cdn_flux").asLong(0);
                getCount = latest.path("get_count").asLong(0);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch blob_io stats: {}", e.getMessage());
        }

        long putCount = getLatestValue("rs_put", begin, end);

        return StorageOverviewResponse.builder()
                .standardSpace(standardSpace)
                .standardCount(standardCount)
                .lineSpace(lineSpace)
                .lineCount(lineCount)
                .archiveSpace(archiveSpace)
                .archiveCount(archiveCount)
                .blobIoFlux(blobIoFlux)
                .cdnFlux(cdnFlux)
                .getCount(getCount)
                .putCount(putCount)
                .standardSpaceGb(standardSpace / BYTES_PER_GB)
                .lineSpaceGb(lineSpace / BYTES_PER_GB)
                .archiveSpaceGb(archiveSpace / BYTES_PER_GB)
                .blobIoFluxGb(blobIoFlux / BYTES_PER_GB)
                .cdnFluxGb(cdnFlux / BYTES_PER_GB)
                .queryRange(queryRange)
                .bucket(resolveBucket())
                .build();
    }

    @Override
    public StatisticTypeResponse getStatistic(String statType, String begin, String end, String granularity) {
        if (begin == null || begin.isEmpty()) begin = LocalDate.now().minusDays(30).format(DATE_FMT);
        if (end == null || end.isEmpty()) end = LocalDate.now().format(DATE_FMT);
        if (granularity == null || granularity.isEmpty()) granularity = "day";

        String label = STAT_LABELS.getOrDefault(statType, statType);
        String unit = STAT_UNITS.getOrDefault(statType, "");

        List<StatisticDataPoint> dataPoints = new ArrayList<>();
        try {
            JsonNode result = fetchQiniuStatApi(statType, begin, end, granularity);
            JsonNode datas = result.path("datas");
            if (datas.isArray()) {
                for (JsonNode item : datas) {
                    String time = item.has("time") ? item.get("time").asText() : "";
                    long value = 0;
                    if (item.has("values")) {
                        JsonNode values = item.get("values");
                        if (values.isObject()) {
                            Iterator<String> fields = values.fieldNames();
                            if (fields.hasNext()) {
                                value = values.get(fields.next()).asLong(0);
                            }
                        } else if (values.isNumber()) {
                            value = values.asLong(0);
                        }
                    } else if (item.has("sizes")) {
                        value = item.get("sizes").asLong(0);
                    } else if (item.has("counts")) {
                        value = item.get("counts").asLong(0);
                    }
                    dataPoints.add(StatisticDataPoint.builder().time(time).value(value).build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch statistic for type={}: {}", statType, e.getMessage(), e);
        }

        return StatisticTypeResponse.builder()
                .statType(statType)
                .statLabel(label)
                .unit(unit)
                .datas(dataPoints)
                .build();
    }

    @Override
    public BlobIoStatisticResponse getBlobIoStatistic(String begin, String end, String granularity) {
        if (begin == null || begin.isEmpty()) begin = LocalDate.now().minusDays(30).format(DATE_FMT);
        if (end == null || end.isEmpty()) end = LocalDate.now().format(DATE_FMT);
        if (granularity == null || granularity.isEmpty()) granularity = "day";

        List<BlobIoDataPoint> dataPoints = new ArrayList<>();
        try {
            JsonNode result = fetchQiniuStatApi("blob_io", begin, end, granularity);
            JsonNode datas = result.path("datas");
            if (datas.isArray()) {
                for (JsonNode item : datas) {
                    BlobIoDataPoint point = BlobIoDataPoint.builder()
                            .time(item.has("time") ? item.get("time").asText() : "")
                            .flux(item.path("flux").asLong(0))
                            .cdnFlux(item.path("cdn_flux").asLong(0))
                            .readBytes(item.path("read_bytes").asLong(0))
                            .getCount(item.path("get_count").asLong(0))
                            .build();
                    dataPoints.add(point);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch blob_io stats: {}", e.getMessage(), e);
        }

        return BlobIoStatisticResponse.builder()
                .datas(dataPoints)
                .bucket(resolveBucket())
                .queryRange(begin + " ~ " + end)
                .build();
    }

    @Override
    public FullStatisticResponse getFullStatistics(String begin, String end, String granularity) {
        StorageOverviewResponse overview = getStorageOverview(begin, end);

        Map<String, StatisticTypeResponse> details = new LinkedHashMap<>();
        for (String statType : List.of("space", "count", "space_line", "count_line",
                "space_archive", "count_archive", "rs_put", "rs_chtype")) {
            try {
                details.put(statType, getStatistic(statType, begin, end, granularity));
            } catch (Exception e) {
                log.warn("Failed to get statistic for {}: {}", statType, e.getMessage());
            }
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
        STAT_LABELS.forEach((type, label) -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("stat_type", type);
            item.put("stat_label", label);
            item.put("unit", STAT_UNITS.getOrDefault(type, ""));
            result.add(item);
        });
        return result;
    }

    private long getLatestValue(String statType, String begin, String end) {
        try {
            JsonNode result = fetchQiniuStatApi(statType, begin, end, "day");
            JsonNode datas = result.path("datas");
            if (datas.isArray() && !datas.isEmpty()) {
                JsonNode latest = datas.get(datas.size() - 1);
                if (latest.has("values")) {
                    JsonNode values = latest.get("values");
                    if (values.isObject()) {
                        Iterator<String> fields = values.fieldNames();
                        if (fields.hasNext()) {
                            return values.get(fields.next()).asLong(0);
                        }
                    } else if (values.isNumber()) {
                        return values.asLong(0);
                    }
                }
                if (latest.has("sizes")) return latest.get("sizes").asLong(0);
                if (latest.has("counts")) return latest.get("counts").asLong(0);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch latest value for statType={}: {}", statType, e.getMessage());
        }
        return 0;
    }

    private JsonNode fetchQiniuStatApi(String statType, String begin, String end, String granularity) throws Exception {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String bucket = resolveBucket();

        long beginTs = LocalDate.parse(begin, DATE_FMT).atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.ofHours(8));
        long endTs = LocalDate.parse(end, DATE_FMT).plusDays(1).atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.ofHours(8));

        String path = "/v6/" + statType;
        String query = "begin=" + beginTs + "&end=" + endTs + "&bucket=" + URLEncoder.encode(bucket, StandardCharsets.UTF_8) + "&g=" + granularity;
        String url = QINIU_API_BASE + "/" + statType + "?" + query;

        String signData = path + "?" + query;
        String sign = hmacSha1(secretKey, signData);
        String accessToken = accessKey + ":" + sign;

        java.net.URL apiUrl = new java.net.URL(url);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Qiniu " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        String responseBody;
        if (responseCode == 200) {
            responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            String errorMsg = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "HTTP " + responseCode;
            log.error("Qiniu stat API error: type={}, code={}, error={}", statType, responseCode, errorMsg);
            throw new RuntimeException("七牛云统计API调用失败: " + errorMsg);
        }

        log.debug("Qiniu stat API response: type={}, body={}", statType, responseBody);
        return objectMapper.readTree(responseBody);
    }

    private String hmacSha1(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA1 signing failed", e);
        }
    }
}