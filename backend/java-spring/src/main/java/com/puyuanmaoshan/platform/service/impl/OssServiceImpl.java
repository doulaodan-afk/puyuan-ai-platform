package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "true", matchIfMissing = true)
public class OssServiceImpl implements OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);

    @Value("${upload.base-path:./uploads}")
    private String basePath;

    @Override
    public String uploadBytes(byte[] bytes, String objectKey) {
        try {
            Path uploadPath = Paths.get(basePath, objectKey);
            Files.createDirectories(uploadPath.getParent());
            Files.write(uploadPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 返回相对路径，由前端/Vite代理/Nginx统一处理转发
            String url = "/uploads/" + objectKey;
            logger.info("Uploaded file to {}: {}", uploadPath, url);
            return url;
        } catch (IOException e) {
            logger.error("Failed to upload file: {}", objectKey, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public String uploadFile(String fileName, String objectKey) {
        try {
            Path sourcePath = Paths.get(fileName);
            byte[] bytes = Files.readAllBytes(sourcePath);
            return uploadBytes(bytes, objectKey);
        } catch (IOException e) {
            logger.error("Failed to read file: {}", fileName, e);
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        try {
            Path filePath = Paths.get(basePath, objectKey);
            Files.deleteIfExists(filePath);
            logger.info("Deleted file: {}", objectKey);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", objectKey, e);
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        return "/uploads/" + objectKey;
    }
}