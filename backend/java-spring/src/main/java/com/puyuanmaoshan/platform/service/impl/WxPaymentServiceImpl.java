package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.RechargeOrder;
import com.puyuanmaoshan.platform.dto.WxPaymentDtos;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.RechargeOrderService;
import com.puyuanmaoshan.platform.service.WxPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 微信支付服务实现
 */
@Slf4j
@Service
public class WxPaymentServiceImpl implements WxPaymentService {

    private final RechargeOrderService rechargeOrderService;
    private final AccountWalletService accountWalletService;

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.payment.mchid:}")
    private String mchid;

    @Value("${wx.payment.api-v3-key:}")
    private String apiV3Key;

    @Value("${wx.payment.notify-url:}")
    private String notifyUrl;

    @Value("${wx.payment.mock-enabled:false}")
    private boolean mockEnabled;

    public WxPaymentServiceImpl(
            RechargeOrderService rechargeOrderService,
            AccountWalletService accountWalletService) {
        this.rechargeOrderService = rechargeOrderService;
        this.accountWalletService = accountWalletService;
    }

    @Override
    public WxPaymentDtos.MiniappPaymentParams createPrepay(
            Long tenantId,
            String orderNo,
            Long amount,
            String packageName) {

        // Mock 模式下直接返回模拟支付参数
        if (mockEnabled) {
            return createMockPrepayParams(orderNo);
        }

        try {
            // 生产环境实现微信支付
            // TODO: 实现真实的微信统一下单接口调用
            log.info("创建微信预支付订单: tenantId={}, orderNo={}, amount={}", tenantId, orderNo, amount);

            WxPaymentDtos.MiniappPaymentParams params = new WxPaymentDtos.MiniappPaymentParams();
            params.setAppId(appId);
            params.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
            params.setNonceStr(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            params.setPackageValue("prepay_id=" + orderNo);
            params.setSignType("RSA");
            params.setPaySign(generateMockSign(orderNo));
            params.setPackageId("prepay_id_" + orderNo);

            return params;
        } catch (Exception e) {
            log.error("创建微信预支付订单失败", e);
            throw new RuntimeException("创建支付订单失败");
        }
    }

    /**
     * 创建 Mock 支付参数（用于开发测试）
     */
    private WxPaymentDtos.MiniappPaymentParams createMockPrepayParams(String orderNo) {
        WxPaymentDtos.MiniappPaymentParams params = new WxPaymentDtos.MiniappPaymentParams();
        params.setAppId("mock_appid");
        params.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        params.setNonceStr("mock_nonce_" + orderNo);
        params.setPackageValue("prepay_id=mock_" + orderNo);
        params.setSignType("MD5");
        params.setPaySign("mock_sign_" + orderNo);
        params.setPackageId("mock_" + orderNo);

        log.info("创建 Mock 支付参数: orderNo={}", orderNo);
        return params;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentNotify(WxPaymentDtos.WxPayNotifyData notifyData) {
        try {
            String orderNo = notifyData.getOutTradeNo();
            String tradeState = notifyData.getTradeState();

            log.info("收到微信支付回调: orderNo={}, tradeState={}", orderNo, tradeState);

            // 查询订单
            RechargeOrder order = rechargeOrderService.lambdaQuery()
                    .eq(RechargeOrder::getOrderNo, orderNo)
                    .one();

            if (order == null) {
                log.warn("订单不存在: {}", orderNo);
                return false;
            }

            // 检查订单状态
            if ("SUCCESS".equals(order.getPayStatus())) {
                log.info("订单已支付，跳过处理: {}", orderNo);
                return true;
            }

            // 处理支付成功
            if ("SUCCESS".equals(tradeState)) {
                // 更新订单状态
                order.setPayStatus("SUCCESS");
                order.setPayChannel("wechat");
                // 注意：RechargeOrder 可能没有 pay_txn_id 字段，根据实际情况调整
                order.setUpdatedAt(LocalDateTime.now());
                rechargeOrderService.updateById(order);

                // 增加用户 Token 余额
                AccountWallet wallet = accountWalletService.lambdaQuery()
                        .eq(AccountWallet::getTenantId, order.getTenantId())
                        .one();

                if (wallet != null) {
                    Long newBalance = (wallet.getTokenBalance() != null ? wallet.getTokenBalance() : 0L)
                            + order.getTokenGrant();
                    wallet.setTokenBalance(newBalance);
                    wallet.setUpdatedAt(LocalDateTime.now());
                    accountWalletService.updateById(wallet);

                    log.info("充值成功: tenantId={}, orderNo={}, tokenGrant={}, newBalance={}",
                            order.getTenantId(), orderNo, order.getTokenGrant(), newBalance);
                }

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return false;
        }
    }

    @Override
    public String queryOrderStatus(String orderNo) {
        // Mock 模式下直接返回成功
        if (mockEnabled) {
            log.info("Mock 查询订单状态: orderNo={}", orderNo);
            return "SUCCESS";
        }

        try {
            // 实际调用微信查询订单接口
            log.info("查询订单状态: orderNo={}", orderNo);
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("查询订单状态失败: orderNo={}", orderNo, e);
            return "ERROR";
        }
    }

    /**
     * 生成 Mock 签名（简化版，实际需要使用微信官方签名算法）
     */
    private String generateMockSign(String orderNo) {
        try {
            String data = String.format("appId=%s&nonceStr=%s&package=%s&signType=%s&timeStamp=%s&key=%s",
                    "mock_appid",
                    "mock_nonce_" + orderNo,
                    "prepay_id=mock_" + orderNo,
                    "MD5",
                    String.valueOf(System.currentTimeMillis() / 1000),
                    "mock_key"
            );

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            log.error("生成签名失败", e);
            return "";
        }
    }
}
