package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.StorageService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "false")
public class QiniuStorageServiceImpl implements StorageService {

    @Value("${app.storage.qiniu.access-key:}")
    private String defaultAccessKey;

    @Value("${app.storage.qiniu.secret-key:}")
    private String defaultSecretKey;

    @Value("${app.storage.qiniu.bucket:puyuanmaoshan}")
    private String defaultBucket;

    @Value("${app.storage.qiniu.cdn-domain:www-cdn.puyuanmaoshan.com}")
    private String defaultCdnDomain;

    private final SystemConfigService systemConfigService;

    public QiniuStorageServiceImpl(SystemConfigService systemConfigService) {
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

    private String resolveCdnDomain() {
        String dbVal = systemConfigService.getConfigValue("oss", "cdn_domain");
        return (dbVal != null && !dbVal.isEmpty()) ? dbVal : defaultCdnDomain;
    }

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength) {
        return uploadFile(objectKey, inputStream, contentLength, "application/octet-stream");
    }

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String bucket = resolveBucket();
        String cdnDomain = resolveCdnDomain();

        try {
            String uploadToken = generateUploadToken(accessKey, secretKey, bucket);
            byte[] bytes = inputStream.readAllBytes();
            String boundary = "----QiniuBoundary" + UUID.randomUUID().toString().replace("-", "");

            String fileName = objectKey;
            int lastSlash = objectKey.lastIndexOf('/');
            if (lastSlash >= 0) {
                fileName = objectKey.substring(lastSlash + 1);
            }

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Disposition: form-data; name=\"token\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(uploadToken.getBytes(StandardCharsets.UTF_8));
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Disposition: form-data; name=\"key\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(objectKey.getBytes(StandardCharsets.UTF_8));
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(bytes);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            java.net.URL url = new java.net.URL("https://up.qiniup.com/");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            conn.getOutputStream().write(body.toByteArray());
            conn.getOutputStream().flush();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                java.io.InputStream errStream = conn.getErrorStream();
                String errorMsg = errStream != null
                        ? new String(errStream.readAllBytes(), StandardCharsets.UTF_8)
                        : "HTTP " + responseCode;
                log.error("Qiniu upload failed: code={}, error={}", responseCode, errorMsg);
                throw new RuntimeException("七牛云上传失败: " + errorMsg);
            }

            log.info("Qiniu upload success: key={}", objectKey);
            return getPublicUrl(objectKey, cdnDomain);
        } catch (Exception e) {
            log.error("Qiniu upload error: key={}, error={}", objectKey, e.getMessage(), e);
            throw new RuntimeException("七牛云上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        try {
            String url = getSignedUrl(objectKey, 3600);
            java.net.URL downloadUrl = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) downloadUrl.openConnection();
            return conn.getInputStream();
        } catch (Exception e) {
            log.error("Qiniu download error: key={}, error={}", objectKey, e.getMessage(), e);
            throw new RuntimeException("七牛云下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String bucket = resolveBucket();

        try {
            String encodedEntry = Base64.getEncoder().encodeToString(
                    (bucket + ":" + objectKey).getBytes(StandardCharsets.UTF_8));
            String sign = hmacSha1(secretKey, "/delete/" + encodedEntry);
            String accessToken = accessKey + ":" + sign;

            java.net.URL url = new java.net.URL("https://rs.qiniu.com/delete/" + encodedEntry);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Qiniu " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                log.info("Qiniu delete success: key={}", objectKey);
            } else {
                log.warn("Qiniu delete response: code={}", responseCode);
            }
        } catch (Exception e) {
            log.error("Qiniu delete error: key={}, error={}", objectKey, e.getMessage(), e);
        }
    }

    @Override
    public String getSignedUrl(String objectKey, int expiresIn) {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String cdnDomain = resolveCdnDomain();

        long deadline = System.currentTimeMillis() / 1000 + expiresIn;
        String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
        String sign = hmacSha1(secretKey, encodedKey + ":" + deadline);
        String encodedSign = Base64.getEncoder().encodeToString(sign.getBytes(StandardCharsets.UTF_8));
        return "https://" + cdnDomain + "/" + encodedKey + "?e=" + deadline + "&token=" + accessKey + ":" + encodedSign;
    }

    @Override
    public String getPublicUrl(String objectKey) {
        return getPublicUrl(objectKey, resolveCdnDomain());
    }

    private String getPublicUrl(String objectKey, String cdnDomain) {
        return "https://" + cdnDomain + "/" + objectKey;
    }

    @Override
    public boolean fileExists(String objectKey) {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String bucket = resolveBucket();

        try {
            String encodedEntry = Base64.getEncoder().encodeToString(
                    (bucket + ":" + objectKey).getBytes(StandardCharsets.UTF_8));
            String sign = hmacSha1(secretKey, "/stat/" + encodedEntry);
            String accessToken = accessKey + ":" + sign;

            java.net.URL url = new java.net.URL("https://rs.qiniu.com/stat/" + encodedEntry);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Qiniu " + accessToken);

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            log.error("Qiniu stat error: key={}, error={}", objectKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int getAvailableConfigCount() {
        return 1;
    }

    @Override
    public boolean testConnection() {
        try {
            String uploadToken = generateUploadToken(resolveAccessKey(), resolveSecretKey(), resolveBucket());
            return uploadToken != null && !uploadToken.isEmpty();
        } catch (Exception e) {
            log.error("Qiniu connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private String generateUploadToken(String accessKey, String secretKey, String bucket) {
        long deadline = System.currentTimeMillis() / 1000 + 3600;
        String putPolicy = "{\"scope\":\"" + bucket + "\",\"deadline\":" + deadline + "}";
        String encodedPutPolicy = Base64.getEncoder().encodeToString(
                putPolicy.getBytes(StandardCharsets.UTF_8));
        String sign = hmacSha1(secretKey, encodedPutPolicy);
        String encodedSign = Base64.getEncoder().encodeToString(
                sign.getBytes(StandardCharsets.UTF_8));
        return accessKey + ":" + encodedSign + ":" + encodedPutPolicy;
    }

    private String hmacSha1(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return new String(Base64.getEncoder().encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA1 signing failed", e);
        }
    }
}