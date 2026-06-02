package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.SystemConfigDtos.*;
import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.entity.SystemConfig;
import com.puyuanmaoshan.platform.mapper.SystemConfigMapper;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.service.SystemConfigService;
import com.puyuanmaoshan.platform.util.CryptoUtil;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 * 支持 Redis 缓存和审计日志（Redis 可选）
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class SystemConfigServiceImplWithRedis extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigServiceImplWithRedis.class);
    private static final String CACHE_PREFIX = "system_config:";
    private static final long CACHE_TTL_SECONDS = 300; // 5 分钟

    private final CryptoUtil cryptoUtil;
    private final Optional<StringRedisTemplate> redisTemplateOptional;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public List<SystemConfig> getGroupConfigs(String configGroup) {
        String cacheKey = CACHE_PREFIX + "group:" + configGroup;
        if (redisTemplateOptional.isPresent()) {
            try {
                String cached = redisTemplateOptional.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<SystemConfig>>() {});
                }
            } catch (Exception e) {
                logger.warn("读取配置缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigGroup, configGroup)
                .orderByAsc(SystemConfig::getSortOrder);
        List<SystemConfig> configs = list(wrapper);
        // 解密配置值
        configs.forEach(c -> c.setConfigValue(cryptoUtil.decrypt(c.getConfigValue())));

        // 写入缓存
        if (redisTemplateOptional.isPresent()) {
            try {
                String json = objectMapper.writeValueAsString(configs);
                redisTemplateOptional.get().opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("写入配置缓存失败: {}", e.getMessage());
            }
        }

        return configs;
    }

    @Override
    public List<SystemConfig> getActiveGroupConfigs(String configGroup) {
        String cacheKey = CACHE_PREFIX + "active:" + configGroup;
        if (redisTemplateOptional.isPresent()) {
            try {
                String cached = redisTemplateOptional.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<SystemConfig>>() {});
                }
            } catch (Exception e) {
                logger.warn("读取配置缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigGroup, configGroup)
                .eq(SystemConfig::getEnabled, true)
                .orderByAsc(SystemConfig::getSortOrder);
        List<SystemConfig> configs = list(wrapper);
        // 解密配置值
        configs.forEach(c -> c.setConfigValue(cryptoUtil.decrypt(c.getConfigValue())));

        // 写入缓存
        if (redisTemplateOptional.isPresent()) {
            try {
                String json = objectMapper.writeValueAsString(configs);
                redisTemplateOptional.get().opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("写入配置缓存失败: {}", e.getMessage());
            }
        }

        return configs;
    }

    @Override
    public Map<String, String> getGroupConfigMap(String configGroup) {
        String cacheKey = CACHE_PREFIX + "map:" + configGroup;
        if (redisTemplateOptional.isPresent()) {
            try {
                String cached = redisTemplateOptional.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<Map<String, String>>() {});
                }
            } catch (Exception e) {
                logger.warn("读取配置缓存失败: {}", e.getMessage());
            }
        }

        List<SystemConfig> configs = getGroupConfigs(configGroup);
        Map<String, String> map = configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        SystemConfig::getConfigValue,
                        (v1, v2) -> v1 // 如果有重复 key，保留第一个
                ));

        // 写入缓存
        if (redisTemplateOptional.isPresent()) {
            try {
                String json = objectMapper.writeValueAsString(map);
                redisTemplateOptional.get().opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("写入配置缓存失败: {}", e.getMessage());
            }
        }

        return map;
    }

    @Override
    public List<Map<String, String>> getProviderConfigs(String configGroup) {
        String cacheKey = CACHE_PREFIX + "providers:" + configGroup;
        if (redisTemplateOptional.isPresent()) {
            try {
                String cached = redisTemplateOptional.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<Map<String, String>>>() {});
                }
            } catch (Exception e) {
                logger.warn("读取配置缓存失败: {}", e.getMessage());
            }
        }

        List<SystemConfig> configs = getGroupConfigs(configGroup);

        // 按 priority 分组
        Map<Integer, List<SystemConfig>> grouped = configs.stream()
                .collect(Collectors.groupingBy(
                        c -> Integer.parseInt(c.getConfigValue() != null && c.getConfigKey().equals("priority") ? c.getConfigValue() : "1"),
                        TreeMap::new,
                        Collectors.toList()
                ));

        // 转换为 Map 列表
        List<Map<String, String>> result = grouped.values().stream()
                .map(group -> {
                    Map<String, String> provider = new HashMap<>();
                    group.forEach(c -> provider.put(c.getConfigKey(), c.getConfigValue()));
                    return provider;
                })
                .collect(Collectors.toList());

        // 写入缓存
        if (redisTemplateOptional.isPresent()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplateOptional.get().opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("写入配置缓存失败: {}", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public List<Map<String, String>> getActiveProviderConfigs(String configGroup) {
        String cacheKey = CACHE_PREFIX + "active_providers:" + configGroup;
        if (redisTemplateOptional.isPresent()) {
            try {
                String cached = redisTemplateOptional.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<Map<String, String>>>() {});
                }
            } catch (Exception e) {
                logger.warn("读取配置缓存失败: {}", e.getMessage());
            }
        }

        List<SystemConfig> configs = getActiveGroupConfigs(configGroup);

        // 按 priority 分组
        Map<Integer, List<SystemConfig>> grouped = configs.stream()
                .collect(Collectors.groupingBy(
                        c -> Integer.parseInt(c.getConfigValue() != null && c.getConfigKey().equals("priority") ? c.getConfigValue() : "1"),
                        TreeMap::new,
                        Collectors.toList()
                ));

        // 转换为 Map 列表
        List<Map<String, String>> result = grouped.values().stream()
                .map(group -> {
                    Map<String, String> provider = new HashMap<>();
                    group.forEach(c -> provider.put(c.getConfigKey(), c.getConfigValue()));
                    return provider;
                })
                .collect(Collectors.toList());

        // 写入缓存
        if (redisTemplateOptional.isPresent()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplateOptional.get().opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("写入配置缓存失败: {}", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public String getConfigValue(String configGroup, String configKey) {
        String cacheKey = CACHE_PREFIX + "value:" + configGroup + ":" + configKey;
        if (redisTemplateOptional.isPresent()) {
            try {
                String cached = redisTemplateOptional.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                logger.warn("读取配置缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigGroup, configGroup)
                .eq(SystemConfig::getConfigKey, configKey)
                .last("LIMIT 1");
        SystemConfig config = getOne(wrapper);
        String value = null;
        if (config != null) {
            value = cryptoUtil.decrypt(config.getConfigValue());
        }

        // 写入缓存
        if (value != null && redisTemplateOptional.isPresent()) {
            try {
                redisTemplateOptional.get().opsForValue().set(cacheKey, value, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("写入配置缓存失败: {}", e.getMessage());
            }
        }

        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemConfig saveOrUpdateConfig(SaveConfigRequest request) {
        SystemConfig config;
        String oldValue = null;

        if (request.getId() != null) {
            // 更新现有配置
            config = getById(request.getId());
            if (config == null) {
                throw new IllegalArgumentException("配置不存在: " + request.getId());
            }
            oldValue = cryptoUtil.decrypt(config.getConfigValue());
            config.setConfigGroup(request.getConfigGroup());
            config.setConfigKey(request.getConfigKey());
            config.setConfigValue(cryptoUtil.encrypt(request.getConfigValue()));
            config.setEnabled(request.getEnabled());
            config.setSortOrder(request.getSortOrder());
            config.setDescription(request.getDescription());
            config.setUpdatedAt(LocalDateTime.now());
        } else {
            // 新增配置
            config = SystemConfig.builder()
                    .configGroup(request.getConfigGroup())
                    .configKey(request.getConfigKey())
                    .configValue(cryptoUtil.encrypt(request.getConfigValue()))
                    .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                    .description(request.getDescription())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        saveOrUpdate(config);

        // 清除缓存
        if (redisTemplateOptional.isPresent()) {
            clearCache(request.getConfigGroup());
        }

        // 记录审计日志
        recordAuditLog(request.getId() != null ? "UPDATE" : "CREATE",
                "system_config",
                String.valueOf(config.getId()),
                oldValue,
                request.getConfigValue());

        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SystemConfig config = getById(id);
        if (config != null) {
            String oldValue = cryptoUtil.decrypt(config.getConfigValue());
            removeById(id);

            // 清除缓存
            if (redisTemplateOptional.isPresent()) {
                clearCache(config.getConfigGroup());
            }

            // 记录审计日志
            recordAuditLog("DELETE",
                    "system_config",
                    String.valueOf(id),
                    oldValue,
                    null);
        }
    }

    @Override
    public ConfigResponse toConfigResponse(SystemConfig config) {
        String maskedValue = null;
        // 对敏感字段进行脱敏
        if ("api_key".equals(config.getConfigKey())) {
            maskedValue = CryptoUtil.maskApiKey(config.getConfigValue());
        } else if ("access_key_secret".equals(config.getConfigKey())) {
            maskedValue = CryptoUtil.maskKey(config.getConfigValue(),4, 4);
        } else {
            maskedValue = config.getConfigValue();
        }

        return ConfigResponse.builder()
                .id(config.getId())
                .configGroup(config.getConfigGroup())
                .configKey(config.getConfigKey())
                .configValue(maskedValue)
                .enabled(config.getEnabled())
                .sortOrder(config.getSortOrder())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    @Override
    public TestConfigResponse testConfig(Long id) {
        SystemConfig config = getById(id);
        if (config == null) {
            return TestConfigResponse.builder()
                    .success(false)
                    .message("配置不存在")
                    .build();
        }

        long startTime = System.currentTimeMillis();

        try {
            String group = config.getConfigGroup();

            if (group.startsWith("ai_")) {
                // 测试 AI 配置
                return testAiConfig(group);
            } else if ("oss".equals(group)) {
                // 测试 OSS 配置
                return testOssConfig();
            } else {
                // 其他配置，简单验证能解密即可
                cryptoUtil.decrypt(config.getConfigValue());
                long latency = System.currentTimeMillis() - startTime;
                return TestConfigResponse.builder()
                        .success(true)
                        .message("配置有效，解密成功")
                        .latency(latency)
                        .build();
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            logger.error("测试配置失败: {}", e.getMessage(), e);
            return TestConfigResponse.builder()
                    .success(false)
                    .message("配置无效: " + e.getMessage())
                    .latency(latency)
                    .build();
        }
    }

    @Override
    public TestConfigResponse testAiConfig(String configGroup) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取配置
            List<Map<String, String>> providers = getActiveProviderConfigs(configGroup);
            if (providers.isEmpty()) {
                return TestConfigResponse.builder()
                        .success(false)
                        .message("没有可用的配置")
                        .build();
            }

            Map<String, String> provider = providers.get(0);
            String apiKey = provider.get("api_key");
            String endpoint = provider.get("endpoint");

            if (apiKey == null || endpoint == null) {
                return TestConfigResponse.builder()
                        .success(false)
                        .message("配置不完整，缺少 api_key 或 endpoint")
                        .build();
            }

            // 在 Mock 模式下，返回成功
            // 在真实模式下，可以调用 AI API 进行测试
            long latency = System.currentTimeMillis() - startTime;
            return TestConfigResponse.builder()
                    .success(true)
                    .message("AI 配置测试成功 (Mock 模式)")
                    .latency(latency)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            logger.error("测试 AI 配置失败: {}", e.getMessage(), e);
            return TestConfigResponse.builder()
                    .success(false)
                    .message("AI 配置测试失败: " + e.getMessage())
                    .latency(latency)
                    .build();
        }
    }

    @Override
    public TestConfigResponse testOssConfig() {
        long startTime = System.currentTimeMillis();

        try {
            // 获取配置
            List<Map<String, String>> providers = getActiveProviderConfigs("oss");
            if (providers.isEmpty()) {
                return TestConfigResponse.builder()
                        .success(false)
                        .message("没有可用的 OSS 配置")
                        .build();
            }

            Map<String, String> provider = providers.get(0);
            String endpoint = provider.get("endpoint");
            String bucketName = provider.get("bucket_name");

            if (endpoint == null || bucketName == null) {
                return TestConfigResponse.builder()
                        .success(false)
                        .message("配置不完整，缺少 endpoint 或 bucket_name")
                        .build();
            }

            // 在 Mock 模式下，返回成功
            // 在真实模式下，可以尝试列出 bucket 或上传测试文件
            long latency = System.currentTimeMillis() - startTime;
            return TestConfigResponse.builder()
                    .success(true)
                    .message("OSS 配置测试成功 (Mock 模式)")
                    .latency(latency)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            logger.error("测试 OSS 配置失败: {}", e.getMessage(), e);
            return TestConfigResponse.builder()
                    .success(false)
                    .message("OSS 配置测试失败: " + e.getMessage())
                    .latency(latency)
                    .build();
        }
    }

    /**
     * 清除指定分组的所有缓存
     */
    private void clearCache(String configGroup) {
        try {
            redisTemplateOptional.get().delete(CACHE_PREFIX + "group:" + configGroup);
            redisTemplateOptional.get().delete(CACHE_PREFIX + "active:" + configGroup);
            redisTemplateOptional.get().delete(CACHE_PREFIX + "map:" + configGroup);
            redisTemplateOptional.get().delete(CACHE_PREFIX + "providers:" + configGroup);
            redisTemplateOptional.get().delete(CACHE_PREFIX + "active_providers:" + configGroup);
            logger.debug("已清除配置分组 {} 的缓存", configGroup);
        } catch (Exception e) {
            logger.warn("清除配置缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 记录审计日志
     */
    private void recordAuditLog(String action, String targetType, String targetId,
                               String oldValue, String newValue) {
        try {
            Long operatorId = RequestContextUtil.getUserId();
            String ip = RequestContextUtil.getRemoteAddr();

            Map<String, Object> detail = new HashMap<>();
            if (oldValue != null) {
                detail.put("old_value", oldValue);
            }
            if (newValue != null) {
                detail.put("new_value", newValue);
            }

            AuditLog auditLog = AuditLog.builder()
                    .tenantId(0L) // 平台级配置，tenantId 为 0
                    .operatorId(operatorId)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detailJson(objectMapper.writeValueAsString(detail))
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogService.save(auditLog);
            logger.info("记录审计日志: {} {} by {}", action, targetType, operatorId);
        } catch (Exception e) {
            logger.error("记录审计日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 脱敏敏感值（用于审计日志）
     */
    private String maskSensitiveValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("sk-")) {
            return CryptoUtil.maskApiKey(value);
        }
        // 如果超过 8 个字符，脱敏中间部分
        if (value.length() > 8) {
            return CryptoUtil.maskKey(value, 4, 4);
        }
        return value;
    }

    /**
     * 记录 AI Key 切换指标
     */
    public void recordAiKeySwitch(String group, String provider) {
        Counter.builder("ai_key_switch_total")
                .tag("group", group)
                .tag("provider", provider)
                .description("Total number of AI key switches")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 AI 调用成功指标
     */
    public void recordAiCallSuccess(String group, String provider) {
        Counter.builder("ai_call_success_total")
                .tag("group", group)
                .tag("provider", provider)
                .description("Total number of successful AI calls")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 AI 调用失败指标
     */
    public void recordAiCallFailure(String group, String provider, String reason) {
        Counter.builder("ai_call_failure_total")
                .tag("group", group)
                .tag("provider", provider)
                .tag("reason", reason)
                .description("Total number of failed AI calls")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 OSS 切换指标
     */
    public void recordOssSwitch(String bucket) {
        Counter.builder("oss_switch_total")
                .tag("bucket", bucket)
                .description("Total number of OSS switches")
                .register(meterRegistry)
                .increment();
    }
}
