package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.OssService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "false")
public class QiniuOssServiceImpl implements OssService {

    @Value("${app.storage.qiniu.access-key:}")
    private String defaultAccessKey;

    @Value("${app.storage.qiniu.secret-key:}")
    private String defaultSecretKey;

    @Value("${app.storage.qiniu.bucket:puyuanmaoshan}")
    private String defaultBucket;

    @Value("${app.storage.qiniu.cdn-domain:www-cdn.puyuanmaoshan.com}")
    private String defaultCdnDomain;

    private final SystemConfigService systemConfigService;

    public QiniuOssServiceImpl(SystemConfigService systemConfigService) {
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
    public String uploadBytes(byte[] bytes, String objectKey) {
        String accessKey = resolveAccessKey();
        String secretKey = resolveSecretKey();
        String bucket = resolveBucket();
        String cdnDomain = resolveCdnDomain();

        try {
            String uploadToken = generateUploadToken(accessKey, secretKey, bucket);
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

            java.io.InputStream in = conn.getInputStream();
            byte[] respBytes = in.readAllBytes();
            log.info("Qiniu upload success: key={}, response={}", objectKey,
                    new String(respBytes, StandardCharsets.UTF_8));
            return getFileUrl(objectKey, cdnDomain);
        } catch (Exception e) {
            log.error("Qiniu upload error: key={}, error={}", objectKey, e.getMessage(), e);
            throw new RuntimeException("七牛云上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(String fileName, String objectKey) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fileName));
            return uploadBytes(bytes, objectKey);
        } catch (Exception e) {
            log.error("Failed to read file: {}", fileName, e);
            throw new RuntimeException("文件读取失败", e);
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
    public String getFileUrl(String objectKey) {
        return getFileUrl(objectKey, resolveCdnDomain());
    }

    private String getFileUrl(String objectKey, String cdnDomain) {
        return "https://" + cdnDomain + "/" + objectKey;
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