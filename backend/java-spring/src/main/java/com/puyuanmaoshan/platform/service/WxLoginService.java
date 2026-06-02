package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.ApiModels;

/**
 * 微信登录服务
 */
public interface WxLoginService {

    /**
     * 处理微信登录
     *
     * @param code 微信登录凭证
     * @param userInfo 用户信息（可选）
     * @return 登录响应
     */
    ApiModels.LoginResponse handleWxLogin(String code, ApiModels.WxLoginRequest.WxUserInfo userInfo);

    /**
     * 调用微信 jscode2session 接口获取 openId
     *
     * @param code 微信登录凭证
     * @return openId
     */
    String getOpenIdByCode(String code);
}
