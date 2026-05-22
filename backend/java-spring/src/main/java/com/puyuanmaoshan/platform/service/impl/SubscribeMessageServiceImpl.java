package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 订阅消息服务实现
 */
@Slf4j
@Service
public class SubscribeMessageServiceImpl implements SubscribeMessageService {

    private final AccountWalletService accountWalletService;
    private final TenantService tenantService;
    private final RestTemplate restTemplate;

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.miniapp.secret:}")
    private String appSecret;

    @Value("${wx.subscribe.access-token:}")
    private String accessToken;

    @Value("${wx.subscribe.mock-enabled:true}")
    private boolean mockEnabled;

    private static final String SEND_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    public SubscribeMessageServiceImpl(
            AccountWalletService accountWalletService,
            TenantService tenantService,
            RestTemplate restTemplate) {
        this.accountWalletService = accountWalletService;
        this.tenantService = tenantService;
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendBalanceLowNotification(Long userId, Long tenantId) {
        try {
            // 获取租户信息
            Tenant tenant = tenantService.getById(tenantId);
            if (tenant == null) {
                log.warn("租户不存在: {}", tenantId);
                return;
            }

            // 获取当前余额
            AccountWallet wallet = accountWalletService.lambdaQuery()
                    .eq(AccountWallet::getTenantId, tenantId)
                    .one();
            long balance = wallet != null && wallet.getTokenBalance() != null
                    ? wallet.getTokenBalance() : 0L;

            // 构建消息数据
            Map<String, TemplateData> data = new HashMap<>();
            data.put("thing1", new TemplateData(tenant.getName()));
            data.put("thing2", new TemplateData(balance + " Tokens"));
            data.put("thing3", new TemplateData(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))));

            // 发送消息（简化版，不获取 openId）
            sendSubscribeMessage(userId, BALANCE_LOW_TEMPLATE_ID, "/pages/recharge/index", data);

            log.info("发送余额不足提醒: userId={}, tenantId={}, balance={}", userId, tenantId, balance);
        } catch (Exception e) {
            log.error("发送余额不足提醒失败", e);
        }
    }

    @Override
    public void sendRechargeSuccessNotification(Long userId, Long tenantId, Long tokenGrant, String orderNo) {
        try {
            // 获取租户信息
            Tenant tenant = tenantService.getById(tenantId);
            if (tenant == null) {
                log.warn("租户不存在: {}", tenantId);
                return;
            }

            // 构建消息数据
            Map<String, TemplateData> data = new HashMap<>();
            data.put("thing1", new TemplateData(tenant.getName()));
            data.put("thing2", new TemplateData(tokenGrant + " Tokens"));
            data.put("thing3", new TemplateData(orderNo));

            // 发送消息
            sendSubscribeMessage(userId, RECHARGE_SUCCESS_TEMPLATE_ID, "/pages/account/index", data);

            log.info("发送充值成功通知: userId={}, tenantId={}, tokenGrant={}, orderNo={}",
                    userId, tenantId, tokenGrant, orderNo);
        } catch (Exception e) {
            log.error("发送充值成功通知失败", e);
        }
    }

    @Override
    public String generateMiniappCode(Long tenantId, String pluginCode, String page) {
        // Mock 模式下返回固定码
        if (mockEnabled) {
            return "mock_code_" + tenantId;
        }

        // 实际调用微信接口生成小程序码
        // TODO: 实现真实的 getwxacodeunlimit 接口调用
        return "code_" + tenantId + "_" + pluginCode;
    }

    @Override
    public void recordShare(Long userId, Long tenantId, String pluginCode, String resultType, String formId) {
        try {
            // TODO: 记录分享行为到数据库
            // - 创建 share_log 表
            // - 记录：userId, tenantId, pluginCode, resultType, formId, createdAt

            log.info("记录分享行为: userId={}, tenantId={}, pluginCode={}, resultType={}, formId={}",
                    userId, tenantId, pluginCode, resultType, formId);
        } catch (Exception e) {
            log.error("记录分享行为失败", e);
        }
    }

    /**
     * 发送订阅消息
     */
    private void sendSubscribeMessage(Long userId, String templateId, String page,
                                          Map<String, TemplateData> data) {
        if (mockEnabled) {
            log.info("Mock 模式：发送订阅消息, templateId={}", templateId);
            return;
        }

        try {
            String touser = "user_" + userId; // 简化版，实际应该从数据库查询

            // 构建请求
            Map<String, Object> request = new HashMap<>();
            request.put("touser", touser);
            request.put("template_id", templateId);
            request.put("page", page);
            request.put("data", data);

            String url = String.format("%s?access_token=%s", SEND_MESSAGE_URL, accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // 发送请求
            restTemplate.postForObject(url, entity, String.class);

            log.info("发送订阅消息成功: templateId={}", templateId);
        } catch (Exception e) {
            log.error("发送订阅消息失败", e);
        }
    }

    /**
     * 模板数据
     */
    private static class TemplateData {
        private final String value;

        public TemplateData(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // 模板 ID 常量
    private static final String BALANCE_LOW_TEMPLATE_ID = "balance_low_notify";
    private static final String RECHARGE_SUCCESS_TEMPLATE_ID = "recharge_success_notify";
}
