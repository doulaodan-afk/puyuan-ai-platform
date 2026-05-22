package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 订阅消息相关 DTO
 */
@Data
public class SubscribeMessageDtos {

    /**
     * 发送订阅消息请求
     */
    @Data
    public static class SendSubscribeMessageRequest {
        @NotBlank
        @JsonProperty("touser")
        private String touser;

        @NotBlank
        @JsonProperty("template_id")
        private String templateId;

        @JsonProperty("page")
        private String page;

        @JsonProperty("data")
        private Map<String, TemplateData> data;

        @Data
        public static class TemplateData {
            @JsonProperty("value")
            private String value;
        }
    }

    /**
     * 余额不足订阅消息模板 ID
     */
    public static final String BALANCE_LOW_TEMPLATE_ID = "balance_low_notify";

    /**
     * 充值到账订阅消息模板 ID
     */
    public static final String RECHARGE_SUCCESS_TEMPLATE_ID = "recharge_success_notify";

    /**
     * 插件调用结果分享数据
     */
    @Data
    public static class SharePluginResultRequest {
        @NotBlank
        @JsonProperty("plugin_code")
        private String pluginCode;

        @NotBlank
        @JsonProperty("result_type")
        private String resultType; // image, text, script

        @JsonProperty("result_data")
        private Object resultData;

        @NotBlank
        @JsonProperty("result_url")
        private String resultUrl;

        @JsonProperty("form_id")
        private String formId; // 用于统计分析
    }
}
