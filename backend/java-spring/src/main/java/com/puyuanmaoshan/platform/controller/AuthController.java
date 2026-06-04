package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.SmsService;
import com.puyuanmaoshan.platform.service.TenantMemberService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.service.UserAccountService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserAccountService userAccountService;
    private final TenantService tenantService;
    private final TenantMemberService tenantMemberService;
    private final AccountWalletService accountWalletService;
    private final PluginService pluginService;
    private final TenantPluginService tenantPluginService;
    private final SmsService smsService;

    public AuthController(UserAccountService userAccountService, TenantService tenantService,
                          TenantMemberService tenantMemberService, AccountWalletService accountWalletService,
                          PluginService pluginService, TenantPluginService tenantPluginService,
                          SmsService smsService) {
        this.userAccountService = userAccountService;
        this.tenantService = tenantService;
        this.tenantMemberService = tenantMemberService;
        this.accountWalletService = accountWalletService;
        this.pluginService = pluginService;
        this.tenantPluginService = tenantPluginService;
        this.smsService = smsService;
    }

    @PostMapping("/auth/login")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ApiModels.LoginResponse> login(@Valid @RequestBody ApiModels.LoginRequest request,
                                                      @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        logger.info("========== [Login] START ==========");
        logger.info("Request: mobile={}, verifyCode={}", request.mobile(), request.verifyCode());

        if (!smsService.verifySmsCode(request.mobile(), request.verifyCode(), "login")) {
            logger.error("SMS code verification failed for mobile: {}", request.mobile());
            throw new AppException(ErrorCode.VALIDATION_ERROR, "invalid verification code");
        }

        UserAccount user = firstByMobile(request.mobile());
        logger.info("User lookup result: {}", user != null ? user.getId() : "null");

        if (user == null) {
            logger.info("Auto-registering new user for mobile: {}", request.mobile());
            user = createSmsUser(request.mobile());
        }

        logger.debug("Login user: id={}, mobile={}, roleCode={}", user.getId(), user.getMobile(), user.getRoleCode());

        Tenant tenant = tenantService.getById(user.getTenantId());
        logger.info("Tenant lookup result: id={}, name={}", tenant != null ? tenant.getId() : null, tenant != null ? tenant.getName() : null);
        if (tenant == null) {
            logger.error("Tenant not found for id: {}", user.getTenantId());
            throw new AppException(ErrorCode.NOT_FOUND, "tenant not found");
        }

        String accessToken = "token-" + user.getId() + "-" + tenant.getId();
        ApiModels.LoginResponse data = new ApiModels.LoginResponse(
                accessToken,
                7200,
                user.getId(),
                tenant.getId(),
                user.getRoleCode()
        );
        logger.info("Login SUCCESS: userId={}, tenantId={}, accessToken={}", user.getId(), tenant.getId(), accessToken);
        logger.info("========== [Login] END ==========");
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-login"));
    }

    private UserAccount createSmsUser(String mobile) {
        LocalDateTime now = LocalDateTime.now();

        Tenant tenant = Tenant.builder()
                .tenantCode(generateTenantCode())
                .name("新工作室")
                .status(1)
                .level("free")
                .tenantType("individual")
                .createdAt(now)
                .updatedAt(now)
                .build();
        tenantService.save(tenant);
        Long tenantId = tenant.getId();

        UserAccount user = UserAccount.builder()
                .tenantId(tenantId)
                .mobile(mobile)
                .nickname("用户" + mobile.substring(mobile.length() - 4))
                .roleCode("boss")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userAccountService.save(user);
        Long userId = user.getId();

        tenantMemberService.saveTenantUser(tenantId, userId, "boss", userId);

        AccountWallet wallet = AccountWallet.builder()
                .tenantId(tenantId)
                .tokenBalance(1000L)
                .cashBalance(BigDecimal.ZERO)
                .frozenToken(0L)
                .status(1)
                .updatedAt(now)
                .build();
        accountWalletService.save(wallet);

        enableDefaultPlugins(tenantId, now);

        logger.info("创建短信登录用户成功: userId={}, tenantId={}, mobile={}", userId, tenantId, mobile);
        return user;
    }

    private void enableDefaultPlugins(Long tenantId, LocalDateTime now) {
        String[] defaultPlugins = {"ai_image_gen", "ai_script_gen", "ai_translate"};
        for (String pluginId : defaultPlugins) {
            Plugin plugin = pluginService.lambdaQuery()
                    .eq(Plugin::getPluginId, pluginId)
                    .eq(Plugin::getStatus, 1)
                    .one();
            if (plugin != null) {
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

    private String generateTenantCode() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @GetMapping("/tenant/profile")
    public ApiResponse<ApiModels.ProfileResponse> profile(@RequestHeader("X-Tenant-Id") String tenantId,
                                                          @RequestHeader("Authorization") String authHeader,
                                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        Tenant tenant = tenantService.getById(parsedTenantId);
        if (tenant == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "tenant not found");
        }

        // # MEMORY: Extract userId from token (format: "token-{userId}-{tenantId}")
        long userId = RequestContextUtil.parseUserIdFromToken(authHeader);

        UserAccount user = userAccountService.getById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "user not found");
        }
        if (!user.getTenantId().equals(parsedTenantId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "user not in this tenant");
        }

        ApiModels.ProfileResponse data = new ApiModels.ProfileResponse(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getName(),
                tenant.getStatus(),
                user.getId(),
                user.getRoleCode()
        );
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-profile"));
    }

    private UserAccount firstByMobile(String mobile) {
        List<UserAccount> users = userAccountService.lambdaQuery()
                .eq(UserAccount::getMobile, mobile)
                .eq(UserAccount::getStatus, 1)
                .orderByAsc(UserAccount::getId)
                .list();
        return users.isEmpty() ? null : users.get(0);
    }
}

