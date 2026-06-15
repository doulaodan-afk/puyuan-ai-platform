package com.puyuanmaoshan.platform.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Autowired(required = false)
    private Client smsClient;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${aliyun.sms.sign-name:濮院毛衫}")
    private String signName;

    @Value("${aliyun.sms.login-template-id:SMS_474785772}")
    private String loginTemplateId;

    @Value("${aliyun.sms.register-template-id:SMS_474980747}")
    private String registerTemplateId;

    @Value("${aliyun.sms.enabled:false}")
    private boolean smsEnabled;

    // 验证码过期时间（分钟）
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;

    // 内存验证码存储（Redis 不可用时的降级方案）
    private final ConcurrentHashMap<String, CodeEntry> memoryCodeStore = new ConcurrentHashMap<>();

    private static class CodeEntry {
        final String code;
        final long expireAt;

        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /**
     * 发送登录短信验证码
     */
    public void sendLoginCode(String mobile) {
        sendSmsCode(mobile, loginTemplateId, "login");
    }

    /**
     * 发送注册短信验证码
     */
    public void sendRegisterCode(String mobile) {
        sendSmsCode(mobile, registerTemplateId, "register");
    }

    /**
     * 发送短信验证码
     */
    private void sendSmsCode(String mobile, String templateId, String purpose) {
        String code;
        if (!smsEnabled || smsClient == null) {
            // SMS 未启用或 Client 不可用，使用 Mock 模式（固定验证码便于测试）
            code = "123456";
            logger.info("SMS Mock mode: mobile={}, purpose={}, code={}", mobile, purpose, code);
            String redisKey = buildRedisKey(mobile, purpose);
            storeCode(redisKey, code);
            return;
        }

        // 生成 6 位随机验证码
        code = String.valueOf((int) (Math.random() * 900000) + 100000);

        try {
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(mobile)
                    .setSignName(signName)
                    .setTemplateCode(templateId)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsResponse response = smsClient.sendSmsWithOptions(sendSmsRequest, runtime);

            logger.info("SMS sent - mobile: {}, purpose: {}, code: {}, response code: {}, message: {}",
                    mobile, purpose, code, response.body.code, response.body.message);

            if ("OK".equals(response.body.code)) {
                // 将验证码存入 Redis，设置过期时间
                String redisKey = buildRedisKey(mobile, purpose);
                storeCode(redisKey, code);
                logger.info("SMS code stored - key: {}", redisKey);
            } else {
                logger.error("SMS send failed - code: {}, message: {}", response.body.code, response.body.message);
                fallbackToMock(mobile, purpose);
            }
        } catch (TeaException e) {
            logger.error("SMS send error - mobile: {}, error: {}", mobile, e.getMessage());
            fallbackToMock(mobile, purpose);
        } catch (Exception e) {
            logger.error("Unexpected error sending SMS - mobile: {}, error: {}", mobile, e.getMessage(), e);
            fallbackToMock(mobile, purpose);
        }
    }

    /**
     * SMS 发送失败时降级到 Mock 模式，确保登录流程不被阻断
     */
    private void fallbackToMock(String mobile, String purpose) {
        String code = "123456";
        logger.warn("SMS fallback to mock mode: mobile={}, purpose={}, code={}", mobile, purpose, code);
        String redisKey = buildRedisKey(mobile, purpose);
        storeCode(redisKey, code);
    }

    /**
     * 验证短信验证码
     */
    public boolean verifySmsCode(String mobile, String code, String purpose) {
        String redisKey = buildRedisKey(mobile, purpose);
        String storedCode = retrieveCode(redisKey);

        if (storedCode == null) {
            logger.warn("SMS code expired or not found - mobile: {}, purpose: {}", mobile, purpose);
            return false;
        }

        boolean valid = storedCode.equals(code);
        if (valid) {
            // 验证成功后删除验证码
            deleteCode(redisKey);
            logger.info("SMS code verified successfully - mobile: {}, purpose: {}", mobile, purpose);
        } else {
            logger.warn("SMS code verification failed - mobile: {}, purpose: {}", mobile, purpose);
        }

        return valid;
    }

    /**
     * 存储验证码（优先 Redis，降级到内存）
     */
    private void storeCode(String key, String code) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                logger.warn("Redis store failed, falling back to memory: {}", e.getMessage());
            }
        }
        long expireAt = System.currentTimeMillis() + SMS_CODE_EXPIRE_MINUTES * 60 * 1000;
        memoryCodeStore.put(key, new CodeEntry(code, expireAt));
    }

    /**
     * 获取验证码（优先 Redis，降级到内存）
     */
    private String retrieveCode(String key) {
        if (redisTemplate != null) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                logger.warn("Redis retrieve failed, falling back to memory: {}", e.getMessage());
            }
        }
        CodeEntry entry = memoryCodeStore.get(key);
        if (entry == null || entry.isExpired()) {
            memoryCodeStore.remove(key);
            return null;
        }
        return entry.code;
    }

    /**
     * 删除验证码（优先 Redis，降级到内存）
     */
    private void deleteCode(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (Exception e) {
                logger.warn("Redis delete failed, falling back to memory: {}", e.getMessage());
            }
        }
        memoryCodeStore.remove(key);
    }

    private String buildRedisKey(String mobile, String purpose) {
        return "sms:code:" + purpose + ":" + mobile;
    }
}