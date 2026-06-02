package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信支付相关 DTO
 */
@Data
public class WxPaymentDtos {

    /**
     * 微信支付预下单请求
     */
    @Data
    public static class WxPrepayRequest {
        @JsonProperty("package_id")
        private String packageId;

        @JsonProperty("amount")
        private Amount amount;

        @JsonProperty("payer")
        private Payer payer;

        @JsonProperty("attach")
        private String attach;

        @JsonProperty("detail")
        private Detail detail;

        @JsonProperty("description")
        private String description;

        @JsonProperty("out_trade_no")
        private String outTradeNo;

        @JsonProperty("time_expire")
        private String timeExpire;

        @JsonProperty("notify_url")
        private String notifyUrl;

        @Data
        public static class Amount {
            @JsonProperty("total")
            private Long total; // 单位：分

            @JsonProperty("currency")
            private String currency;

            public Amount(Long total, String currency) {
                this.total = total;
                this.currency = currency;
            }
        }

        @Data
        public static class Payer {
            @JsonProperty("openid")
            private String openid;
        }

        @Data
        public static class Detail {
            @JsonProperty("goods_detail")
            private GoodsDetail[] goodsDetail;

            @Data
            public static class GoodsDetail {
                @JsonProperty("merchant_goods_id")
                private String merchantGoodsId;

                @JsonProperty("goods_name")
                private String goodsName;

                @JsonProperty("quantity")
                private Long quantity;

                @JsonProperty("unit_price")
                private Long unitPrice; // 单位：分

                public GoodsDetail(String merchantGoodsId, String goodsName, Long quantity, Long unitPrice) {
                    this.merchantGoodsId = merchantGoodsId;
                    this.goodsName = goodsName;
                    this.quantity = quantity;
                    this.unitPrice = unitPrice;
                }
            }
        }
    }

    /**
     * 微信支付预下单响应
     */
    @Data
    public static class WxPrepayResponse {
        @JsonProperty("prepay_id")
        private String prepayId;
    }

    /**
     * 小程序支付参数
     */
    @Data
    public static class MiniappPaymentParams {
        @JsonProperty("appId")
        private String appId;

        @JsonProperty("timeStamp")
        private String timeStamp;

        @JsonProperty("nonceStr")
        private String nonceStr;

        @JsonProperty("package")
        private String packageValue;

        @JsonProperty("signType")
        private String signType;

        @JsonProperty("paySign")
        private String paySign;

        @JsonProperty("packageId")
        private String packageId;
    }

    /**
     * 微信支付回调通知
     */
    @Data
    public static class WxPayNotifyRequest {
        @JsonProperty("id")
        private String id;

        @JsonProperty("event_type")
        private String eventType;

        @JsonProperty("resource")
        private Resource resource;

        @JsonProperty("create_time")
        private String createTime;

        @Data
        public static class Resource {
            @JsonProperty("algorithm")
            private String algorithm;

            @JsonProperty("ciphertext")
            private String ciphertext;

            @JsonProperty("nonce")
            private String nonce;

            @JsonProperty("associated_data")
            private String associatedData;
        }
    }

    /**
     * 微信支付回调解密后数据
     */
    @Data
    public static class WxPayNotifyData {
        @JsonProperty("mchid")
        private String mchid;

        @JsonProperty("appid")
        private String appid;

        @JsonProperty("out_trade_no")
        private String outTradeNo;

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("trade_type")
        private String tradeType;

        @JsonProperty("trade_state")
        private String tradeState;

        @JsonProperty("trade_state_desc")
        private String tradeStateDesc;

        @JsonProperty("bank_type")
        private String bankType;

        @JsonProperty("attach")
        private String attach;

        @JsonProperty("success_time")
        private String successTime;

        @JsonProperty("payer")
        private Payer payer;

        @JsonProperty("amount")
        private NotifyAmount amount;

        @Data
        public static class Payer {
            @JsonProperty("openid")
            private String openid;
        }

        @Data
        public static class NotifyAmount {
            @JsonProperty("total")
            private Long total;

            @JsonProperty("payer_total")
            private Long payerTotal;

            @JsonProperty("currency")
            private String currency;

            @JsonProperty("payer_currency")
            private String payerCurrency;
        }
    }
}
