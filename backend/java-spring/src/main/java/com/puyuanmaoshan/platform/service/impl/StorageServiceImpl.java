package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.service.StorageService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 对象存储服务实现
 * 支持从 ConfigService 获取多配置并自动切换
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.mock.enabled", havingValue = "true")
public class StorageServiceImpl implements StorageService {

    private final SystemConfigService systemConfigService;

    private static final String MOCK_BASE_URL = "https://mock-oss.puyuanmaoshan.com";

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength) {
        return uploadFile(objectKey, inputStream, contentLength, "application/octet-stream");
    }

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        log.info("Uploading file: {}, size: {}, type: {}", objectKey, contentLength, contentType);

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            for (Map<String, String> provider : providers) {
                try {
                    String providerName = provider.get("provider_name");
                    String endpoint = provider.get("endpoint");
                    String bucketName = provider.get("bucket_name");
                    String accessKeyId = provider.get("access_key_id");
                    String accessKeySecret = provider.get("access_key_secret");

                    log.info("Using OSS provider: {}, bucket: {}, endpoint: {}",
                            providerName, bucketName, endpoint);

                    // TODO: 在真实模式下，这里调用 OSS SDK 上传文件
                    // return uploadToOss(providerName, endpoint, bucketName, accessKeyId, accessKeySecret, objectKey, inputStream, contentLength, contentType);

                    // Mock 模式：返回占位 URL
                    return MOCK_BASE_URL + "/" + objectKey;
                } catch (Exception e) {
                    log.warn("OSS provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败，使用默认 Mock 模式
            log.warn("All OSS providers failed, falling back to default mock URL");
            return MOCK_BASE_URL + "/" + objectKey;

        } catch (Exception e) {
            log.error("Failed to get OSS config: {}", e.getMessage(), e);
            // 降级到 Mock 模式
            return MOCK_BASE_URL + "/" + objectKey;
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        log.info("Downloading file: {}", objectKey);

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            for (Map<String, String> provider : providers) {
                try {
                    String providerName = provider.get("provider_name");
                    String endpoint = provider.get("endpoint");
                    String bucketName = provider.get("bucket_name");

                    log.info("Using OSS provider: {}, bucket: {}, endpoint: {}",
                            providerName, bucketName, endpoint);

                    // TODO: 在真实模式下，这里调用 OSS SDK 下载文件
                    // return downloadFromOss(providerName, endpoint, bucketName, objectKey);

                    // Mock 模式：返回空的输入流
                    return new ByteArrayInputStream(new byte[0]);
                } catch (Exception e) {
                    log.warn("OSS provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败，返回空的输入流
            log.warn("All OSS providers failed, returning empty input stream");
            return new ByteArrayInputStream(new byte[0]);

        } catch (Exception e) {
            log.error("Failed to get OSS config: {}", e.getMessage(), e);
            // 降级到 Mock 模式
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        log.info("Deleting file: {}", objectKey);

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            for (Map<String, String> provider : providers) {
                try {
                    String providerName = provider.get("provider_name");
                    String endpoint = provider.get("endpoint");
                    String bucketName = provider.get("bucket_name");

                    log.info("Using OSS provider: {}, bucket: {}, endpoint: {}",
                            providerName, bucketName, endpoint);

                    // TODO: 在真实模式下，这里调用 OSS SDK 删除文件
                    // deleteFromOss(providerName, endpoint, bucketName, objectKey);
                    return; // 成功删除
                } catch (Exception e) {
                    log.warn("OSS provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败
            log.warn("All OSS providers failed, file may not be deleted");

        } catch (Exception e) {
            log.error("Failed to get OSS config: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getSignedUrl(String objectKey, int expiresIn) {
        log.info("Getting signed URL for: {}, expiresIn: {}s", objectKey, expiresIn);

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            for (Map<String, String> provider : providers) {
                try {
                    String providerName = provider.get("provider_name");
                    String endpoint = provider.get("endpoint");
                    String bucketName = provider.get("bucket_name");
                    String region = provider.get("region");

                    log.info("Using OSS provider: {}, bucket: {}, endpoint: {}",
                            providerName, bucketName, endpoint);

                    // TODO: 在真实模式下，这里调用 OSS SDK 生成签名 URL
                    // return generateSignedUrl(providerName, endpoint, bucketName, region, objectKey, expiresIn);

                    // Mock 模式：返回占位 URL
                    return MOCK_BASE_URL + "/" + objectKey + "?expires=" + expiresIn;
                } catch (Exception e) {
                    log.warn("OSS provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败，返回默认 Mock URL
            log.warn("All OSS providers failed, returning default mock URL");
            return MOCK_BASE_URL + "/" + objectKey + "?expires=" + expiresIn;

        } catch (Exception e) {
            log.error("Failed to get OSS config: {}", e.getMessage(), e);
            // 降级到 Mock 模式
            return MOCK_BASE_URL + "/" + objectKey + "?expires=" + expiresIn;
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        log.info("Getting public URL for: {}", objectKey);

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            if (!providers.isEmpty()) {
                Map<String, String> provider = providers.get(0);
                String endpoint = provider.get("endpoint");
                String bucketName = provider.get("bucket_name");

                // 生成公共 URL
                return "https://" + bucketName + "." + endpoint + "/" + objectKey;
            }

            // 返回默认 Mock URL
            return MOCK_BASE_URL + "/" + objectKey;

        } catch (Exception e) {
            log.error("Failed to get OSS config: {}", e.getMessage(), e);
            // 降级到 Mock 模式
            return MOCK_BASE_URL + "/" + objectKey;
        }
    }

    @Override
    public boolean fileExists(String objectKey) {
        log.info("Checking file exists: {}", objectKey);

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            for (Map<String, String> provider : providers) {
                try {
                    String providerName = provider.get("provider_name");
                    String endpoint = provider.get("endpoint");
                    String bucketName = provider.get("bucket_name");

                    log.info("Using OSS provider: {}, bucket: {}, endpoint: {}",
                            providerName, bucketName, endpoint);

                    // TODO: 在真实模式下，这里调用 OSS SDK 检查文件是否存在
                    // return checkFileExists(providerName, endpoint, bucketName, objectKey);

                    // Mock 模式：返回 true
                    return true;
                } catch (Exception e) {
                    log.warn("OSS provider failed, trying next provider: {}", e.getMessage());
                    // 继续尝试下一个提供商
                }
            }

            // 所有提供商都失败，返回 false
            log.warn("All OSS providers failed, returning false");
            return false;

        } catch (Exception e) {
            log.error("Failed to get OSS config: {}", e.getMessage(), e);
            // 降级到 false
            return false;
        }
    }

    @Override
    public int getAvailableConfigCount() {
        try {
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");
            return providers.size();
        } catch (Exception e) {
            log.error("Failed to get OSS config count: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean testConnection() {
        log.info("Testing OSS connection");

        try {
            // 尝试从数据库获取 OSS 配置
            List<Map<String, String>> providers = systemConfigService.getActiveProviderConfigs("oss");

            if (providers.isEmpty()) {
                log.warn("No OSS providers configured");
                return false;
            }

            // 测试第一个可用的提供商
            Map<String, String> provider = providers.get(0);
            String providerName = provider.get("provider_name");
            String endpoint = provider.get("endpoint");

            log.info("Testing OSS provider: {}, endpoint: {}", providerName, endpoint);

            // TODO: 在真实模式下，这里调用 OSS SDK 测试连接
            // return testOssConnection(provider);

            // Mock 模式：返回 true
            log.info("OSS connection test passed (Mock mode)");
            return true;

        } catch (Exception e) {
            log.error("OSS connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
