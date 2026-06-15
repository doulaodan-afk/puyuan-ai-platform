package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.TenantDtos;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.entity.TenantUser;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.PluginService;
import com.puyuanmaoshan.platform.service.PricingConfigService;
import com.puyuanmaoshan.platform.service.SmsService;
import com.puyuanmaoshan.platform.service.TenantMemberService;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import com.puyuanmaoshan.platform.service.TenantProfileService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.service.TenantStorageService;
import com.puyuanmaoshan.platform.service.UserAccountService;
import com.puyuanmaoshan.platform.mapper.TenantUserMapper;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final TenantStorageService tenantStorageService;
    private final SmsService smsService;
    private final PricingConfigService pricingConfigService;
    private final TenantProfileService tenantProfileService;
    private final TenantUserMapper tenantUserMapper;

    public AuthController(UserAccountService userAccountService, TenantService tenantService,
                          TenantMemberService tenantMemberService, AccountWalletService accountWalletService,
                          PluginService pluginService, TenantPluginService tenantPluginService,
                          TenantStorageService tenantStorageService,
                          SmsService smsService,
                          PricingConfigService pricingConfigService,
                          TenantProfileService tenantProfileService,
                          TenantUserMapper tenantUserMapper) {
        this.userAccountService = userAccountService;
        this.tenantService = tenantService;
        this.tenantMemberService = tenantMemberService;
        this.accountWalletService = accountWalletService;
        this.pluginService = pluginService;
        this.tenantPluginService = tenantPluginService;
        this.tenantStorageService = tenantStorageService;
        this.smsService = smsService;
        this.pricingConfigService = pricingConfigService;
        this.tenantProfileService = tenantProfileService;
        this.tenantUserMapper = tenantUserMapper;
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

        logger.debug("Login user: id={}, mobile={}, roleCode={}, tenantId={}", user.getId(), user.getMobile(), user.getRoleCode(), user.getTenantId());

        // ====== 修复：被邀请用户可能在 user_account.tenant_id 为 0 的情况下登录 ======
        // 如果用户的 primary tenantId 无效（0 或租户不存在），则从 tenant_user 表中查找其活跃租户
        Long effectiveTenantId = (user.getTenantId() != null && user.getTenantId() > 0)
                ? user.getTenantId() : 0L;
        boolean validPrimaryTenant = effectiveTenantId > 0 && tenantService.getById(effectiveTenantId) != null;

        if (!validPrimaryTenant) {
            logger.info("User {} has no valid primary tenant (tenantId={}), looking up tenant_user...",
                    user.getId(), user.getTenantId());
            List<TenantUser> activeTenants = tenantUserMapper.selectActiveTenantsByUserId(user.getId());
            if (activeTenants != null && !activeTenants.isEmpty()) {
                // 选最早加入的活跃租户
                TenantUser earliest = activeTenants.get(0);
                for (int i = 1; i < activeTenants.size(); i++) {
                    TenantUser current = activeTenants.get(i);
                    if (current.getCreatedAt() != null &&
                        (earliest.getCreatedAt() == null ||
                         current.getCreatedAt().isBefore(earliest.getCreatedAt()))) {
                        earliest = current;
                    }
                }
                effectiveTenantId = earliest.getTenantId();
                logger.info("Using earliest active tenant {} for user {}", effectiveTenantId, user.getId());
            }
        }

        Tenant tenant = tenantService.getById(effectiveTenantId);
        logger.info("Tenant lookup result: id={}, name={}", tenant != null ? tenant.getId() : null, tenant != null ? tenant.getName() : null);
        if (tenant == null) {
            logger.error("No valid tenant found for user {} (original tenantId={})", user.getId(), user.getTenantId());
            throw new AppException(ErrorCode.NOT_FOUND, "您尚未加入任何工作室，请联系管理员邀请您加入");
        }

        String accessToken = "token-" + user.getId() + "-" + tenant.getId();
        List<TenantDtos.UserTenant> userTenants = tenantMemberService.getUserTenants(user.getId());
        ApiModels.LoginResponse data = new ApiModels.LoginResponse(
                accessToken,
                7200,
                user.getId(),
                tenant.getId(),
                user.getRoleCode(),
                userTenants
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

        // 从定价配置读取新用户注册赠送 Token 数量（默认 10）
        long registerBonusToken = 10L;
        try {
            registerBonusToken = pricingConfigService.getRegisterBonusToken();
        } catch (Exception e) {
            logger.warn("读取注册赠送Token配置失败，使用默认值10: {}", e.getMessage());
        }
        logger.info("新用户注册赠送 Token: {}", registerBonusToken);

        AccountWallet wallet = AccountWallet.builder()
                .tenantId(tenantId)
                .tokenBalance(registerBonusToken)
                .cashBalance(BigDecimal.ZERO)
                .frozenToken(0L)
                .status(1)
                .updatedAt(now)
                .build();
        accountWalletService.save(wallet);

        enableDefaultPlugins(tenantId, now);

        // 新租户注册后，自动分配免费存储空间
        tenantStorageService.autoAssignBucketForNewTenant(tenantId, tenant.getTenantCode());

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

        // 修复：使用 tenant_user 表校验用户是否属于该租户，而非 user_account.tenant_id
        // 原因：被邀请用户可能 user_account.tenant_id = 0，但在 tenant_user 中有有效记录
        TenantUser tenantUser = tenantUserMapper.selectByUserIdAndTenantId(userId, parsedTenantId);
        if (tenantUser == null || !tenantUser.isActive()) {
            logger.warn("User {} is not an active member of tenant {}", userId, parsedTenantId);
            throw new AppException(ErrorCode.FORBIDDEN, "user not in this tenant");
        }

        ApiModels.ProfileResponse data = new ApiModels.ProfileResponse(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getLogoUrl(),
                user.getId(),
                tenantUser.getRole()  // 使用 tenant_user 中的角色，而非 user_account.role_code
        );
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-profile"));
    }

    @PutMapping("/tenant/profile")
    public ApiResponse<TenantDtos.TenantProfileDetailResponse> updateTenantProfile(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody TenantDtos.UpdateTenantProfileRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        tenantProfileService.updateTenantProfile(parsedTenantId, request);
        TenantDtos.TenantProfileDetailResponse updated = tenantProfileService.getTenantProfile(parsedTenantId);
        logger.info("Tenant {} updated profile", parsedTenantId);
        return ApiResponse.ok(updated, RequestContextUtil.resolveRequestId(requestId, "req-tenant-update"));
    }

    @PostMapping(value = "/tenant/upload-logo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TenantDtos.UploadLogoResponse> uploadLogo(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);

        if (file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "文件不能为空");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "文件大小不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "仅支持图片文件");
        }

        try {
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "logo.png";
            String url = tenantProfileService.uploadLogo(parsedTenantId, bytes, fileName);
            logger.info("Tenant {} uploaded logo: {}", parsedTenantId, url);
            return ApiResponse.ok(new TenantDtos.UploadLogoResponse(url), RequestContextUtil.resolveRequestId(requestId, "req-tenant-logo"));
        } catch (Exception e) {
            logger.error("Failed to upload logo for tenant {}", parsedTenantId, e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Logo上传失败: " + e.getMessage());
        }
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

