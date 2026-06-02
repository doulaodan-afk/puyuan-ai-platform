package com.puyuanmaoshan.platform.service.impl;

import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.entity.RechargeOrder;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.dto.WxPaymentDtos;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import com.puyuanmaoshan.platform.service.RechargeOrderService;
import com.puyuanmaoshan.platform.service.UserAccountService;
import com.puyuanmaoshan.platform.service.WxPaymentService;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.cipher.Signer;
import com.wechat.pay.java.core.cipher.SignatureResult;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 微信支付服务实现
 *
 * 支持两种模式：
 * 1. Mock 模式 (wx.payment.mock-enabled=true)：用于本地开发和测试，不调用微信接口
 * 2. 真实模式 (wx.payment.mock-enabled=false)：使用 wechatpay-java SDK 调用微信支付 V3 API
 */
@Slf4j
@Service
public class WxPaymentServiceImpl implements WxPaymentService {

    private final RechargeOrderService rechargeOrderService;
    private final AccountWalletService accountWalletService;
    private final UserAccountService userAccountService;

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.payment.mchid:}")
    private String mchid;

    @Value("${wx.payment.api-v3-key:}")
    private String apiV3Key;

    @Value("${wx.payment.notify-url:}")
    private String notifyUrl;

    @Value("${wx.payment.private-key-path:}")
    private String privateKeyPath;

    @Value("${wx.payment.merchant-serial-number:}")
    private String merchantSerialNumber;

    @Value("${wx.payment.mock-enabled:true}")
    private boolean mockEnabled;

    /** SDK 客户端，在 mockEnabled=false 时初始化 */
    private JsapiService jsapiService;
    private NotificationParser notificationParser;
    private Config config;

    public WxPaymentServiceImpl(
            RechargeOrderService rechargeOrderService,
            AccountWalletService accountWalletService,
            UserAccountService userAccountService) {
        this.rechargeOrderService = rechargeOrderService;
        this.accountWalletService = accountWalletService;
        this.userAccountService = userAccountService;
    }

    /**
     * 初始化微信支付 SDK 客户端
     */
    @PostConstruct
    public void init() {
        if (mockEnabled) {
            log.info("微信支付 Mock 模式已启用，跳过 SDK 初始化");
            return;
        }

        try {
            if (appId.isEmpty() || mchid.isEmpty() || apiV3Key.isEmpty()
                    || privateKeyPath.isEmpty() || merchantSerialNumber.isEmpty()) {
                log.warn("微信支付配置不完整，将降级为 Mock 模式。请检查 wx.payment.* 配置项");
                mockEnabled = true;
                return;
            }

            // 读取商户私钥内容（支持 classpath 路径和文件绝对路径）
            String privateKeyContent = loadPrivateKeyContent(privateKeyPath);

            // 使用 RSA 自动证书配置，SDK 会自动下载和管理微信平台证书
            config = new RSAAutoCertificateConfig.Builder()
                    .merchantId(mchid)
                    .privateKey(privateKeyContent)
                    .merchantSerialNumber(merchantSerialNumber)
                    .apiV3Key(apiV3Key)
                    .build();

            jsapiService = new JsapiService.Builder().config(config).build();

            // RSAAutoCertificateConfig 本身实现了 NotificationConfig 接口
            notificationParser = new NotificationParser((RSAAutoCertificateConfig) config);

            log.info("微信支付 SDK 初始化成功: appId={}, mchid={}", appId, mchid);
        } catch (Exception e) {
            log.error("微信支付 SDK 初始化失败，将降级为 Mock 模式", e);
            mockEnabled = true;
        }
    }

    /**
     * 读取商户私钥内容
     * 支持 classpath 路径（如 wxpay-cert/apiclient_key.pem）和文件绝对路径（如 /app/config/apiclient_key.pem）
     */
    private String loadPrivateKeyContent(String path) throws Exception {
        if (path.startsWith("/") || path.startsWith("C:") || path.startsWith("D:") || path.contains(":")) {
            // 绝对文件路径（生产环境）
            java.nio.file.Path filePath = java.nio.file.Paths.get(path);
            return java.nio.file.Files.readString(filePath);
        } else {
            // classpath 路径（开发环境）
            ClassPathResource resource = new ClassPathResource(path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
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
            // 获取用户 openId（通过 tenantId 找到用户的 wechat_openid）
            String openId = getOpenIdByTenantId(tenantId);
            if (openId == null || openId.isEmpty()) {
                log.warn("未找到租户 {} 对应的微信 openId，降级为 Mock 模式", tenantId);
                return createMockPrepayParams(orderNo);
            }

            // 构建统一下单请求
            PrepayRequest request = new PrepayRequest();
            Amount orderAmount = new Amount();
            orderAmount.setTotal(amount.intValue()); // 单位：分
            orderAmount.setCurrency("CNY");
            request.setAmount(orderAmount);
            request.setAppid(appId);
            request.setMchid(mchid);
            request.setDescription(packageName != null ? packageName : "濮院毛衫AI平台-充值");
            request.setNotifyUrl(notifyUrl);
            request.setOutTradeNo(orderNo);

            // 设置支付者 openId
            com.wechat.pay.java.service.payments.jsapi.model.Payer payer = new com.wechat.pay.java.service.payments.jsapi.model.Payer();
            payer.setOpenid(openId);
            request.setPayer(payer);

            log.info("调用微信统一下单: orderNo={}, amount={}分, openId={}", orderNo, amount, openId);

            // 调用微信统一下单 API
            PrepayResponse response = jsapiService.prepay(request);
            String prepayId = response.getPrepayId();

            log.info("微信统一下单成功: orderNo={}, prepayId={}", orderNo, prepayId);

            // 生成小程序支付调起参数并签名
            return buildMiniappPaymentParams(prepayId);

        } catch (Exception e) {
            log.error("创建微信预支付订单失败: orderNo={}", orderNo, e);
            throw new RuntimeException("创建支付订单失败: " + e.getMessage());
        }
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
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(mchid);
            request.setOutTradeNo(orderNo);

            Transaction transaction = jsapiService.queryOrderByOutTradeNo(request);
            String tradeState = transaction.getTradeState().name();

            log.info("查询订单状态成功: orderNo={}, tradeState={}", orderNo, tradeState);
            return tradeState;
        } catch (Exception e) {
            log.error("查询订单状态失败: orderNo={}", orderNo, e);
            return "ERROR";
        }
    }

    /**
     * 解密微信支付回调通知
     *
     * @param notifyRequest 微信回调通知原始请求体
     * @param wechatpaySerialNumber 微信平台证书序列号（从 HTTP Header Wechatpay-Serial 获取）
     * @param wechatpaySignature 微信签名（从 HTTP Header Wechatpay-Signature 获取）
     * @param wechatpayTimestamp 微信时间戳（从 HTTP Header Wechatpay-Timestamp 获取）
     * @param wechatpayNonce 微信随机串（从 HTTP Header Wechatpay-Nonce 获取）
     * @param rawBody HTTP 请求原始 body
     * @return 解密后的通知数据
     */
    public WxPaymentDtos.WxPayNotifyData decryptNotify(
            WxPaymentDtos.WxPayNotifyRequest notifyRequest,
            String wechatpaySerialNumber,
            String wechatpaySignature,
            String wechatpayTimestamp,
            String wechatpayNonce,
            String rawBody) {

        if (mockEnabled) {
            WxPaymentDtos.WxPayNotifyData data = new WxPaymentDtos.WxPayNotifyData();
            data.setOutTradeNo("mock_" + System.currentTimeMillis());
            data.setTradeState("SUCCESS");
            data.setTransactionId("mock_txn_" + System.currentTimeMillis());
            return data;
        }

        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(wechatpaySerialNumber)
                    .nonce(wechatpayNonce)
                    .timestamp(wechatpayTimestamp)
                    .signature(wechatpaySignature)
                    .body(rawBody)
                    .build();

            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);

            WxPaymentDtos.WxPayNotifyData data = new WxPaymentDtos.WxPayNotifyData();
            data.setMchid(transaction.getMchid());
            data.setAppid(transaction.getAppid());
            data.setOutTradeNo(transaction.getOutTradeNo());
            data.setTransactionId(transaction.getTransactionId());
            data.setTradeState(transaction.getTradeState().name());
            data.setTradeType(transaction.getTradeType() != null ? transaction.getTradeType().name() : null);
            data.setTradeStateDesc(transaction.getTradeStateDesc());
            data.setSuccessTime(transaction.getSuccessTime());

            if (transaction.getPayer() != null) {
                WxPaymentDtos.WxPayNotifyData.Payer payer = new WxPaymentDtos.WxPayNotifyData.Payer();
                payer.setOpenid(transaction.getPayer().getOpenid());
                data.setPayer(payer);
            }

            if (transaction.getAmount() != null) {
                WxPaymentDtos.WxPayNotifyData.NotifyAmount amount = new WxPaymentDtos.WxPayNotifyData.NotifyAmount();
                amount.setTotal(transaction.getAmount().getTotal() != null ? transaction.getAmount().getTotal().longValue() : null);
                amount.setPayerTotal(transaction.getAmount().getPayerTotal() != null ? transaction.getAmount().getPayerTotal().longValue() : null);
                amount.setCurrency(transaction.getAmount().getCurrency());
                amount.setPayerCurrency(transaction.getAmount().getPayerCurrency());
                data.setAmount(amount);
            }

            return data;
        } catch (Exception e) {
            log.error("解密微信支付回调通知失败", e);
            throw new RuntimeException("解密支付通知失败: " + e.getMessage());
        }
    }

    /**
     * 构建小程序支付调起参数
     * 使用 SDK 的 Signer 进行 RSA 签名
     */
    private WxPaymentDtos.MiniappPaymentParams buildMiniappPaymentParams(String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        // 构造签名串（微信支付 V3 规定格式）
        String signMessage = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + "prepay_id=" + prepayId + "\n";

        // 使用 SDK 的签名器签名（sign 方法接受 String）
        String paySign;
        try {
            Signer signer = config.createSigner();
            SignatureResult signatureResult = signer.sign(signMessage);
            paySign = signatureResult.getSign();
        } catch (Exception e) {
            log.error("生成支付签名失败，降级为 Mock 签名", e);
            paySign = generateMockSign(prepayId);
        }

        WxPaymentDtos.MiniappPaymentParams params = new WxPaymentDtos.MiniappPaymentParams();
        params.setAppId(appId);
        params.setTimeStamp(timeStamp);
        params.setNonceStr(nonceStr);
        params.setPackageValue("prepay_id=" + prepayId);
        params.setSignType("RSA");
        params.setPaySign(paySign);
        params.setPackageId(prepayId);

        return params;
    }

    /**
     * 通过 tenantId 获取用户的微信 openId
     */
    private String getOpenIdByTenantId(Long tenantId) {
        try {
            UserAccount user = userAccountService.lambdaQuery()
                    .eq(UserAccount::getTenantId, tenantId)
                    .eq(UserAccount::getStatus, 1)
                    .orderByAsc(UserAccount::getId)
                    .last("LIMIT 1")
                    .one();

            if (user != null && user.getWechatOpenid() != null && !user.getWechatOpenid().isEmpty()) {
                return user.getWechatOpenid();
            }
        } catch (Exception e) {
            log.error("查询用户 openId 失败: tenantId={}", tenantId, e);
        }
        return null;
    }

    /**
     * 创建 Mock 支付参数（用于开发测试）
     */
    private WxPaymentDtos.MiniappPaymentParams createMockPrepayParams(String orderNo) {
        WxPaymentDtos.MiniappPaymentParams params = new WxPaymentDtos.MiniappPaymentParams();
        params.setAppId(appId.isEmpty() ? "mock_appid" : appId);
        params.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        params.setNonceStr("mock_nonce_" + orderNo);
        params.setPackageValue("prepay_id=mock_" + orderNo);
        params.setSignType("MD5");
        params.setPaySign("mock_sign_" + orderNo);
        params.setPackageId("mock_" + orderNo);

        log.info("创建 Mock 支付参数: orderNo={}", orderNo);
        return params;
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