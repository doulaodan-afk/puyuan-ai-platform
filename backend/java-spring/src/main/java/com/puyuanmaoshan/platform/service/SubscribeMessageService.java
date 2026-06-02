package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.SubscribeMessageDtos;

/**
 * 订阅消息服务
 */
public interface SubscribeMessageService {

    /**
     * 发送余额不足提醒
     */
    void sendBalanceLowNotification(Long userId, Long tenantId);

    /**
     * 发送充值到账通知
     */
    void sendRechargeSuccessNotification(Long userId, Long tenantId, Long tokenGrant, String orderNo);

    /**
     * 获取小程序码（用于分享）
     */
    String generateMiniappCode(Long tenantId, String pluginCode, String page);

    /**
     * 记录分享行为
     */
    void recordShare(Long userId, Long tenantId, String pluginCode, String resultType, String formId);
}
