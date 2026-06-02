package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.SubscribeMessageService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.service.UserAccountService;
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
 *
 * 支持两种模式：
 * 1. Mock 模式 (wx.subscribe.mock-enabled=true)：用于本地开发和测试
 * 2. 真实模式 (wx.subscribe.mock-enabled=false)：调用微信接口发送订阅消息
 */
@Slf4j
@Service
public class SubscribeMessageServiceImpl implements SubscribeMessageService {

    private final AccountWalletService accountWalletService;
    private final TenantService tenantService;
    private final UserAccountService userAccountService;
    private final RestTemplate restTemplate;

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.miniapp.secret:}")
    private String appSecret;

    @Value("${wx.subscribe.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${wx.subscribe.balance-low-template-id:balance_low_notify}")
    private String balanceLowTemplateId;

    @Value("${wx.subscribe.recharge-success-template-id:recharge_success_notify}")
    private String rechargeSuccessTemplateId;

    private static final String SEND_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String MINIAPP_CODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";

    /** 缓存的 access_token 和过期时间 */
    private String cachedAccessToken;
    private long accessTokenExpireAt;

    public SubscribeMessageServiceImpl(
            AccountWalletService accountWalletService,
            TenantService tenantService,
            UserAccountService userAccountService,
            RestTemplate restTemplate) {
        this.accountWalletService = accountWalletService;
        this.tenantService = tenantService;
        this.userAccountService = userAccountService;
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

            // 发送消息
            sendSubscribeMessage(userId, balanceLowTemplateId, "/pages/recharge/index", data);

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
            sendSubscribeMessage(userId, rechargeSuccessTemplateId, "/pages/account/index", data);

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

        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.warn("获取 access_token 失败，返回 mock 码");
                return "mock_code_" + tenantId;
            }

            // 构建请求参数
            Map<String, Object> request = new HashMap<>();
            request.put("scene", tenantId + "_" + pluginCode);
            request.put("page", page);
            request.put("width", 280);
            request.put("auto_color", false);
            Map<String, String> lineColor = new HashMap<>();
            lineColor.put("r", "102");
            lineColor.put("g", "126");
            lineColor.put("b", "234");
            request.put("line_color", lineColor);

            String url = MINIAPP_CODE_URL + "?access_token=" + accessToken;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            byte[] response = restTemplate.postForObject(url, entity, byte[].class);

            if (response != null) {
                // 实际场景中应将图片上传到 OSS 并返回 URL
                // 此处简化为返回 base64 或 mock URL
                log.info("生成小程序码成功: tenantId={}, pluginCode={}, size={}", tenantId, pluginCode, response.length);
                return "miniapp_code_" + tenantId + "_" + pluginCode;
            }

            return "mock_code_" + tenantId;
        } catch (Exception e) {
            log.error("生成小程序码失败: tenantId={}, pluginCode={}", tenantId, pluginCode, e);
            return "mock_code_" + tenantId;
        }
    }

    @Override
    public void recordShare(Long userId, Long tenantId, String pluginCode, String resultType, String formId) {
        try {
            // TODO: 创建 share_log 表并记录分享行为
            // 当前仅记录日志，待后续实现数据库持久化
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
            log.info("Mock 模式：发送订阅消息, userId={}, templateId={}", userId, templateId);
            return;
        }

        try {
            // 从数据库查询真实的 openId
            String openId = getOpenIdByUserId(userId);
            if (openId == null || openId.isEmpty()) {
                log.warn("未找到用户 {} 的微信 openId，跳过发送订阅消息", userId);
                return;
            }

            // 获取 access_token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.warn("获取 access_token 失败，跳过发送订阅消息");
                return;
            }

            // 构建请求
            Map<String, Object> request = new HashMap<>();
            request.put("touser", openId);
            request.put("template_id", templateId);
            request.put("page", page);
            request.put("data", data);

            String url = String.format("%s?access_token=%s", SEND_MESSAGE_URL, accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String response = restTemplate.postForObject(url, entity, String.class);
            log.info("发送订阅消息结果: userId={}, templateId={}, response={}", userId, templateId, response);
        } catch (Exception e) {
            log.error("发送订阅消息失败: userId={}, templateId={}", userId, templateId, e);
        }
    }

    /**
     * 获取微信 access_token（带缓存）
     * access_token 有效期 2 小时（7200秒），提前 5 分钟刷新
     */
    private String getAccessToken() {
        // 检查缓存的 token 是否仍然有效
        if (cachedAccessToken != null && System.currentTimeMillis() < accessTokenExpireAt) {
            return cachedAccessToken;
        }

        try {
            String url = String.format("%s?grant_type=client_credential&appid=%s&secret=%s",
                    ACCESS_TOKEN_URL, appId, appSecret);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                return null;
            }

            // 解析响应
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(response);

            if (json.has("access_token")) {
                cachedAccessToken = json.get("access_token").asText();
                int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 7200;
                // 提前 5 分钟过期，避免边界情况
                accessTokenExpireAt = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

                log.info("获取微信 access_token 成功, expiresIn={}秒", expiresIn);
                return cachedAccessToken;
            } else {
                int errCode = json.has("errcode") ? json.get("errcode").asInt() : -1;
                String errMsg = json.has("errmsg") ? json.get("errmsg").asText() : "unknown";
                log.error("获取微信 access_token 失败: errcode={}, errmsg={}", errCode, errMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("获取微信 access_token 异常", e);
            return null;
        }
    }

    /**
     * 通过 userId 从数据库查询真实的微信 openId
     */
    private String getOpenIdByUserId(Long userId) {
        try {
            UserAccount user = userAccountService.getById(userId);
            if (user != null && user.getWechatOpenid() != null && !user.getWechatOpenid().isEmpty()) {
                return user.getWechatOpenid();
            }
        } catch (Exception e) {
            log.error("查询用户 openId 失败: userId={}", userId, e);
        }
        return null;
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
}