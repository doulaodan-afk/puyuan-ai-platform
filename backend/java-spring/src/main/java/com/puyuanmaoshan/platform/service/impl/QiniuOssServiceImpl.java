package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "false")
public class QiniuOssServiceImpl implements OssService {

    @Value("${app.storage.qiniu.access-key:}")
    private String accessKey;

    @Value("${app.storage.qiniu.secret-key:}")
    private String secretKey;

    @Value("${app.storage.qiniu.bucket:puyuanmaoshan}")
    private String bucket;

    @Value("${app.storage.qiniu.cdn-domain:www-cdn.puyuanmaoshan.com}")
    private String cdnDomain;

    @Override
    public String uploadBytes(byte[] bytes, String objectKey) {
        try {
            String uploadToken = generateUploadToken();
            String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);

            java.net.URL url = new java.net.URL("https://up.qiniup.com/upload/" + encodedKey);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "UpToken " + uploadToken);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            conn.getOutputStream().write(bytes);
            conn.getOutputStream().flush();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                java.io.InputStream errStream = conn.getErrorStream();
                String errorMsg = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                log.error("Qiniu upload failed: code={}, error={}", responseCode, errorMsg);
                throw new RuntimeException("七牛云上传失败: " + errorMsg);
            }

            log.info("Qiniu upload success: key={}", objectKey);
            return getFileUrl(objectKey);
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
        return "https://" + cdnDomain + "/" + objectKey;
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