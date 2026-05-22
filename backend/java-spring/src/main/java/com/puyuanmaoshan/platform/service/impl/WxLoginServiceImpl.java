package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.WxLoginDtos;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.entity.TenantUser;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.TenantMemberService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.service.UserAccountService;
import com.puyuanmaoshan.platform.service.WxLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 微信登录服务实现
 */
@Slf4j
@Service
public class WxLoginServiceImpl implements WxLoginService {

    private final UserAccountService userAccountService;
    private final TenantService tenantService;
    private final TenantMemberService tenantMemberService;
    private final AccountWalletService accountWalletService;
    private final PluginService pluginService;
    private final TenantPluginService tenantPluginService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.miniapp.secret:}")
    private String appSecret;

    private static final String JS_CODE_2_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    public WxLoginServiceImpl(
            UserAccountService userAccountService,
            TenantService tenantService,
            TenantMemberService tenantMemberService,
            AccountWalletService accountWalletService,
            PluginService pluginService,
            TenantPluginService tenantPluginService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.userAccountService = userAccountService;
        this.tenantService = tenantService;
        this.tenantMemberService = tenantMemberService;
        this.accountWalletService = accountWalletService;
        this.pluginService = pluginService;
        this.tenantPluginService = tenantPluginService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiModels.LoginResponse handleWxLogin(String code, ApiModels.WxLoginRequest.WxUserInfo userInfo) {
        // 1. 获取 openId
        String openId = getOpenIdByCode(code);
        if (openId == null) {
            throw new RuntimeException("获取微信 openId 失败");
        }

        // 2. 通过 openId 查找用户
        String wxMobileKey = "wx_" + openId;
        UserAccount user = userAccountService.lambdaQuery()
                .eq(UserAccount::getMobile, wxMobileKey)
                .eq(UserAccount::getStatus, 1)
                .one();

        Long userId;
        Long tenantId;
        String role;

        // 3. 如果用户不存在，创建新用户和租户
        if (user == null) {
            user = createWxUser(openId, userInfo);
            userId = user.getId();
            tenantId = user.getTenantId();
            role = "boss"; // 新用户默认为老板角色
        } else {
            userId = user.getId();
            tenantId = user.getTenantId();
            role = user.getRoleCode();
        }

        // 4. 生成访问令牌
        String accessToken = "token-" + userId;

        // 5. 返回登录响应
        return new ApiModels.LoginResponse(
                accessToken,
                7200, // 2小时
                userId,
                tenantId,
                role
        );
    }

    @Override
    public String getOpenIdByCode(String code) {
        try {
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    JS_CODE_2_SESSION_URL, appId, appSecret, code);

            String response = restTemplate.getForObject(url, String.class);
            WxLoginDtos.JsCode2SessionResponse sessionData = objectMapper.readValue(
                    response, WxLoginDtos.JsCode2SessionResponse.class);

            if (sessionData.getErrCode() != null && sessionData.getErrCode() != 0) {
                log.error("微信 jscode2session 失败: code={}, errcode={}, errmsg={}",
                        code, sessionData.getErrCode(), sessionData.getErrMsg());
                return null;
            }

            return sessionData.getOpenId();
        } catch (Exception e) {
            log.error("调用微信 jscode2session 接口失败", e);
            return null;
        }
    }

    /**
     * 创建微信用户
     */
    private UserAccount createWxUser(String openId, ApiModels.WxLoginRequest.WxUserInfo userInfo) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 创建租户
        Tenant tenant = Tenant.builder()
                .tenantCode(generateTenantCode())
                .name(userInfo != null && userInfo.getNickName() != null
                        ? userInfo.getNickName() + "的工作室" : "新工作室")
                .status(1)
                .level("free") // 免费版
                .tenantType("individual") // 个人
                .createdAt(now)
                .updatedAt(now)
                .build();

        tenantService.save(tenant);
        Long tenantId = tenant.getId();

        // 2. 创建用户账户
        String nickname = userInfo != null && userInfo.getNickName() != null
                ? userInfo.getNickName() : "用户" + openId.substring(openId.length() - 4);

        UserAccount user = UserAccount.builder()
                .tenantId(tenantId)
                .mobile("wx_" + openId)
                .nickname(nickname)
                .roleCode("boss")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userAccountService.save(user);
        Long userId = user.getId();

        // 3. 创建租户用户关联
        TenantUser tenantUser = TenantUser.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role("boss")
                .status("active")
                .invitedBy(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        tenantMemberService.saveTenantUser(tenantId, userId, "boss", userId);

        // 4. 创建账户钱包，分配默认免费配额
        AccountWallet wallet = AccountWallet.builder()
                .tenantId(tenantId)
                .tokenBalance(1000L) // 默认 1000 Token
                .cashBalance(java.math.BigDecimal.ZERO)
                .frozenToken(0L)
                .status(1)
                .updatedAt(now)
                .build();

        accountWalletService.save(wallet);

        // 5. 启用默认插件（AI 图片生成、AI 脚本生成、AI 跨境翻译）
        enableDefaultPlugins(tenantId, now);

        log.info("创建微信用户成功: userId={}, tenantId={}, openId={}", userId, tenantId, openId);

        return user;
    }

    /**
     * 启用默认插件
     */
    private void enableDefaultPlugins(Long tenantId, LocalDateTime now) {
        String[] defaultPlugins = {"ai_image_gen", "ai_script_gen", "ai_translate"};

        for (String pluginId : defaultPlugins) {
            // 检查插件是否存在
            Plugin plugin = pluginService.lambdaQuery()
                    .eq(Plugin::getPluginId, pluginId)
                    .eq(Plugin::getStatus, 1)
                    .one();

            if (plugin != null) {
                // 创建租户插件关联
                TenantPlugin tenantPlugin = TenantPlugin.builder()
                        .tenantId(tenantId)
                        .pluginId(pluginId)
                        .enabled(1)
                        .configJson("{}")
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                tenantPluginService.save(tenantPlugin);
            }
        }
    }

    /**
     * 生成租户编码
     */
    private String generateTenantCode() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
