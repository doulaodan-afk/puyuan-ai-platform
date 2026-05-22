package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信登录相关 DTO
 */
@Data
public class WxLoginDtos {

    /**
     * 微信 jscode2session 响应
     */
    @Data
    public static class JsCode2SessionResponse {
        @JsonProperty("openid")
        private String openId;

        @JsonProperty("session_key")
        private String sessionKey;

        @JsonProperty("unionid")
        private String unionId;

        @JsonProperty("errcode")
        private Integer errCode;

        @JsonProperty("errmsg")
        private String errMsg;
    }
}
