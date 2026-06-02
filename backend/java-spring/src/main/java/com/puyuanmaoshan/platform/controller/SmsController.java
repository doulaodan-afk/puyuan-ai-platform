package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sms")
public class SmsController {
    private static final Logger logger = LoggerFactory.getLogger(SmsController.class);

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    /**
     * 发送登录验证码
     */
    @PostMapping("/send-login-code")
    public ApiResponse<Void> sendLoginCode(@RequestParam String mobile) {
        logger.info("Sending login SMS code to: {}", mobile);
        smsService.sendLoginCode(mobile);
        return ApiResponse.ok(null, "req-sms-login");
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-register-code")
    public ApiResponse<Void> sendRegisterCode(@RequestParam String mobile) {
        logger.info("Sending register SMS code to: {}", mobile);
        smsService.sendRegisterCode(mobile);
        return ApiResponse.ok(null, "req-sms-register");
    }
}
