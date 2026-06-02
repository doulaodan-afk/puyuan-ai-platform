package com.puyuanmaoshan.platform.util;

import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RequestContextUtil {
    private static final DateTimeFormatter REQUEST_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private RequestContextUtil() {
    }

    public static long parseTenantId(String tenantIdHeader) {
        try {
            return Long.parseLong(tenantIdHeader);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "invalid tenant id");
        }
    }

    public static String resolveRequestId(String requestId, String fallbackPrefix) {
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return fallbackPrefix + "-" + LocalDateTime.now().format(REQUEST_TS_FORMATTER);
    }

    // # MEMORY: Parse userId from token (format: "token-{userId}-{tenantId}")
    public static long parseUserIdFromToken(String authHeader) {
        try {
            if (authHeader == null || authHeader.isBlank()) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "missing authorization header");
            }
            // Remove "Bearer " prefix
            String token = authHeader.replace("Bearer ", "");
            // Token format: "token-{userId}-{tenantId}"
            String[] parts = token.split("-");
            if (parts.length < 2) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "invalid token format");
            }
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "invalid user id in token");
        }
    }
}
