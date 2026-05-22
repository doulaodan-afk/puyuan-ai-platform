package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.puyuanmaoshan.platform.enums.ErrorCode;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        @JsonProperty("request_id") String requestId
) {
    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.defaultMessage(), data, requestId);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, String requestId) {
        return new ApiResponse<>(errorCode.code(), message, null, requestId);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data, String requestId) {
        return new ApiResponse<>(code, message, data, requestId);
    }
}
