package com.puyuanmaoshan.platform.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunSmsConfig {
    private static final Logger logger = LoggerFactory.getLogger(AliyunSmsConfig.class);

    @Value("${aliyun.sms.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.sms.endpoint:dysmsapi.aliyuncs.com}")
    private String endpoint;

    @Bean
    @ConditionalOnProperty(name = "aliyun.sms.enabled", havingValue = "true")
    public Client smsClient() throws Exception {
        logger.info("Initializing Aliyun SMS Client with endpoint: {}", endpoint);

        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);

        return new Client(config);
    }
}
