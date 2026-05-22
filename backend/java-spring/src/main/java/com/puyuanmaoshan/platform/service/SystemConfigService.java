package com.puyuanmaoshan.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.puyuanmaoshan.platform.dto.SystemConfigDtos.*;
import com.puyuanmaoshan.platform.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 获取指定分组的所有配置（解密后）
     * @param configGroup 配置分组
     * @return 配置列表（解密后的值）
     */
    List<SystemConfig> getGroupConfigs(String configGroup);

    /**
     * 获取指定分组的所有配置（按优先级排序，仅返回启用的）
     * @param configGroup 配置分组
     * @return 配置列表（按 sort_order 升序，enabled=true）
     */
    List<SystemConfig> getActiveGroupConfigs(String configGroup);

    /**
     * 获取指定分组的配置为 Map（key -> value）
     * @param configGroup 配置分组
     * @return 配置 Map（解密后的值）
     */
    Map<String, String> getGroupConfigMap(String configGroup);

    /**
     * 获取指定分组下所有提供商的配置列表
     * 每个提供商可能有多条配置记录（按 priority 分组）
     * @param configGroup 配置分组（如 ai_image, ai_text, oss）
     * @return 提供商配置列表
     */
    List<Map<String, String>> getProviderConfigs(String configGroup);

    /**
     * 获取指定分组的启用配置（按优先级排序）
     * 用于 AI Service 或 StorageService 调用，返回按优先级排序的配置
     * @param configGroup 配置分组
     * @return 配置列表（按 priority 分组，每个 provider 一组配置）
     */
    List<Map<String, String>> getActiveProviderConfigs(String configGroup);

    /**
     * 获取单个配置值（解密）
     * @param configGroup 配置分组
     * @param configKey 配置键
     * @return 配置值（明文）
     */
    String getConfigValue(String configGroup, String configKey);

    /**
     * 保存或更新配置（自动加密）
     * @param request 保存请求
     * @return 保存后的配置
     */
    SystemConfig saveOrUpdateConfig(SaveConfigRequest request);

    /**
     * 删除配置
     * @param id 配置 ID
     */
    void deleteConfig(Long id);

    /**
     * 获取配置响应（包含脱敏后的值）
     * @param config 配置实体
     * @return 配置响应
     */
    ConfigResponse toConfigResponse(SystemConfig config);

    /**
     * 测试配置是否可用
     * @param id 配置 ID
     * @return 测试结果
     */
    TestConfigResponse testConfig(Long id);

    /**
     * 测试 AI 配置是否可用
     * @param configGroup 配置分组（ai_image, ai_text, ai_translate）
     * @return 测试结果
     */
    TestConfigResponse testAiConfig(String configGroup);

    /**
     * 测试 OSS 配置是否可用
     * @return 测试结果
     */
    TestConfigResponse testOssConfig();
}
