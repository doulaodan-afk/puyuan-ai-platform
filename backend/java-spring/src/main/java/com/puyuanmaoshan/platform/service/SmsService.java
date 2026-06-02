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

import java.util.concurrent.TimeUnit;

@Service
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Autowired(required = false)
    private Client smsClient;

    private final StringRedisTemplate redisTemplate;

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

    public SmsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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
        // 生成 6 位随机验证码
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        if (!smsEnabled || smsClient == null) {
            // SMS 未启用或 Client 不可用，使用 Mock 模式
            logger.info("SMS Mock mode: mobile={}, purpose={}, code={}", mobile, purpose, code);
            String redisKey = buildRedisKey(mobile, purpose);
            redisTemplate.opsForValue().set(redisKey, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return;
        }

        try {
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(mobile)
                    .setSignName(signName)
                    .setTemplateCode(templateId)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsResponse response = smsClient.sendSmsWithOptions(sendSmsRequest, runtime);

            logger.info("SMS sent - mobile: {}, purpose: {}, code: {}, response: {}",
                    mobile, purpose, code, response.body.message);

            if ("OK".equals(response.body.message)) {
                // 将验证码存入 Redis，设置过期时间
                String redisKey = buildRedisKey(mobile, purpose);
                redisTemplate.opsForValue().set(redisKey, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                logger.info("SMS code stored in Redis - key: {}", redisKey);
            } else {
                logger.error("SMS send failed - response: {}", response.body.message);
                throw new AppException(ErrorCode.INTERNAL_ERROR, "failed to send SMS");
            }
        } catch (TeaException e) {
            logger.error("SMS send error - mobile: {}, error: {}", mobile, e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "SMS service error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending SMS - mobile: {}, error: {}", mobile, e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "unexpected error");
        }
    }

    /**
     * 验证短信验证码
     */
    public boolean verifySmsCode(String mobile, String code, String purpose) {
        String redisKey = buildRedisKey(mobile, purpose);
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            logger.warn("SMS code expired or not found - mobile: {}, purpose: {}", mobile, purpose);
            return false;
        }

        boolean valid = storedCode.equals(code);
        if (valid) {
            // 验证成功后删除验证码
            redisTemplate.delete(redisKey);
            logger.info("SMS code verified successfully - mobile: {}, purpose: {}", mobile, purpose);
        } else {
            logger.warn("SMS code verification failed - mobile: {}, purpose: {}", mobile, purpose);
        }

        return valid;
    }

    private String buildRedisKey(String mobile, String purpose) {
        return "sms:code:" + purpose + ":" + mobile;
    }
}