package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.WxPaymentDtos;

/**
 * 微信支付服务
 */
public interface WxPaymentService {

    /**
     * 创建微信支付预下单
     *
     * @param tenantId 租户 ID
     * @param orderNo 订单号
     * @param amount 金额（元）
     * @param packageName 套餐名称
     * @return 小程序支付参数
     */
    WxPaymentDtos.MiniappPaymentParams createPrepay(
            Long tenantId,
            String orderNo,
            Long amount,
            String packageName
    );

    /**
     * 处理微信支付回调
     *
     * @param notifyData 支付回调数据
     * @return 处理结果
     */
    boolean handlePaymentNotify(WxPaymentDtos.WxPayNotifyData notifyData);

    /**
     * 查询订单状态（用于异步通知失败时的补偿）
     *
     * @param orderNo 订单号
     * @return 支付状态
     */
    String queryOrderStatus(String orderNo);
}
