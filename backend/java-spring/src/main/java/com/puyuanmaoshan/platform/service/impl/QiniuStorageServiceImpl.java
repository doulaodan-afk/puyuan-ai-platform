package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "false")
public class QiniuStorageServiceImpl implements StorageService {

    @Value("${app.storage.qiniu.access-key:}")
    private String accessKey;

    @Value("${app.storage.qiniu.secret-key:}")
    private String secretKey;

    @Value("${app.storage.qiniu.bucket:puyuanmaoshan}")
    private String bucket;

    @Value("${app.storage.qiniu.cdn-domain:www-cdn.puyuanmaoshan.com}")
    private String cdnDomain;

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength) {
        return uploadFile(objectKey, inputStream, contentLength, "application/octet-stream");
    }

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            String uploadToken = generateUploadToken();
            String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);

            byte[] bytes = inputStream.readAllBytes();

            java.net.URL url = new java.net.URL("https://up.qiniup.com/upload/" + encodedKey);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "UpToken " + uploadToken);
            conn.setRequestProperty("Content-Type", contentType);

            conn.getOutputStream().write(bytes);
            conn.getOutputStream().flush();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorMsg = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("Qiniu upload failed: code={}, error={}", responseCode, errorMsg);
                throw new RuntimeException("七牛云上传失败: " + errorMsg);
            }

            log.info("Qiniu upload success: key={}", objectKey);
            return getPublicUrl(objectKey);
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
        long deadline = System.currentTimeMillis() / 1000 + expiresIn;
        String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
        String sign = hmacSha1(secretKey, encodedKey + ":" + deadline);
        String encodedSign = Base64.getEncoder().encodeToString(sign.getBytes(StandardCharsets.UTF_8));
        return "https://" + cdnDomain + "/" + encodedKey + "?e=" + deadline + "&token=" + accessKey + ":" + encodedSign;
    }

    @Override
    public String getPublicUrl(String objectKey) {
        return "https://" + cdnDomain + "/" + objectKey;
    }

    @Override
    public boolean fileExists(String objectKey) {
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
            String uploadToken = generateUploadToken();
            return uploadToken != null && !uploadToken.isEmpty();
        } catch (Exception e) {
            log.error("Qiniu connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private String generateUploadToken() {
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