package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.service.WxLoginService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信登录控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
public class WxLoginController {

    private final WxLoginService wxLoginService;

    public WxLoginController(WxLoginService wxLoginService) {
        this.wxLoginService = wxLoginService;
    }

    /**
     * 微信授权登录
     */
    @PostMapping("/wx_login")
    public ApiResponse<ApiModels.LoginResponse> wxLogin(
            @Valid @RequestBody ApiModels.WxLoginRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        String resolvedRequestId = RequestContextUtil.resolveRequestId(requestId, "req-wx-login");

        // 调用微信登录服务
        ApiModels.LoginResponse data = wxLoginService.handleWxLogin(
                request.code(),
                request.getUserInfo()
        );

        return ApiResponse.ok(data, resolvedRequestId);
    }
}
