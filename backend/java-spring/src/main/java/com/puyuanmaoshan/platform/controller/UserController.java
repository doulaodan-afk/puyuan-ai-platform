package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.UserDtos;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.UserProfileService;
import com.puyuanmaoshan.platform.service.UserSecurityService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserProfileService userProfileService;
    private final UserSecurityService userSecurityService;

    public UserController(UserProfileService userProfileService,
                        UserSecurityService userSecurityService) {
        this.userProfileService = userProfileService;
        this.userSecurityService = userSecurityService;
    }

    /**
     * 获取用户个人信息
     */
    @GetMapping("/profile")
    public ApiResponse<UserDtos.UserProfileResponse> getProfile(
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        UserDtos.UserProfileResponse profile = userProfileService.getUserProfile(userId);
        return ApiResponse.ok(profile, requestId);
    }

    /**
     * 更新用户个人信息
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @Valid @RequestBody UserDtos.UpdateProfileRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        userProfileService.updateProfile(userId, request);
        logger.info("User {} updated profile", userId);
        return ApiResponse.ok(null, requestId);
    }

    /**
     * 绑定/更换手机号
     */
    @PostMapping("/bind/phone")
    public ApiResponse<Void> bindPhone(
            @Valid @RequestBody UserDtos.BindPhoneRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        userProfileService.bindPhone(userId, request);
        logger.info("User {} bound phone {}", userId, request.phone());
        return ApiResponse.ok(null, requestId);
    }

    /**
     * 绑定微信
     */
    @PostMapping("/bind/wechat")
    public ApiResponse<Void> bindWechat(
            @Valid @RequestBody UserDtos.BindWechatRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        userProfileService.bindWechat(userId, request);
        logger.info("User {} bound WeChat", userId);
        return ApiResponse.ok(null, requestId);
    }

    /**
     * 解绑微信
     */
    @PostMapping("/unbind/wechat")
    public ApiResponse<Void> unbindWechat(
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        userProfileService.unbindWechat(userId);
        logger.info("User {} unbound WeChat", userId);
        return ApiResponse.ok(null, requestId);
    }

    /**
     * 上传头像
     */
    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserDtos.UploadAvatarResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();

        // Validate file
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "File is empty");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "File size exceeds 5MB limit");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/octet-stream"))) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Only image files are allowed");
        }

        try {
            byte[] bytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String fileName = originalFilename != null ? originalFilename : "avatar.jpg";

            String url = userProfileService.uploadAvatar(userId, bytes, fileName);
            logger.info("User {} uploaded avatar: {}", userId, url);

            return ApiResponse.ok(new UserDtos.UploadAvatarResponse(url), requestId);
        } catch (Exception e) {
            logger.error("Failed to upload avatar for user {}", userId, e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to upload avatar: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody UserDtos.ChangePasswordRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        userSecurityService.changePassword(userId, request);
        logger.info("User {} changed password", userId);
        return ApiResponse.ok(null, requestId);
    }

    /**
     * 获取账号安全信息
     */
    @GetMapping("/security")
    public ApiResponse<UserDtos.AccountSecurityResponse> getAccountSecurity(
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        UserDtos.AccountSecurityResponse security = userSecurityService.getAccountSecurity(userId);
        return ApiResponse.ok(security, requestId);
    }

    /**
     * 更新安全设置
     */
    @PutMapping("/security")
    public ApiResponse<Void> updateSecuritySettings(
            @RequestBody UserDtos.SecuritySettingsRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        userSecurityService.updateSecuritySettings(userId, request);
        logger.info("User {} updated security settings", userId);
        return ApiResponse.ok(null, requestId);
    }

    /**
     * 获取登录日志
     */
    @GetMapping("/login-logs")
    public ApiResponse<UserDtos.LoginLogListResponse> getLoginLogs(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestHeader("X-Request-Id") String requestId) {
        Long userId = RequestContextUtil.getCurrentUserId();
        UserDtos.LoginLogListResponse logs = userSecurityService.getLoginLogs(userId, page, pageSize);
        return ApiResponse.ok(logs, requestId);
    }
}