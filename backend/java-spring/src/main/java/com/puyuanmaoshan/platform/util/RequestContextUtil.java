package com.puyuanmaoshan.platform.util;

import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RequestContextUtil {
    private static final DateTimeFormatter REQUEST_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextUtil() {
    }

    public static void setContext(RequestContext context) {
        CONTEXT.set(context);
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    public static Long getUserId() {
        RequestContext context = CONTEXT.get();
        return context == null ? null : context.userId();
    }

    public static Long getCurrentUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new com.puyuanmaoshan.platform.exception.AppException(
                    com.puyuanmaoshan.platform.enums.ErrorCode.UNAUTHORIZED, "User not authenticated");
        }
        return userId;
    }

    public static String getRemoteAddr() {
        RequestContext context = CONTEXT.get();
        return context == null ? null : context.remoteAddr();
    }

    public static long parseUserId(String userIdHeader) {
        try {
            return Long.parseLong(userIdHeader);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "invalid user id");
        }
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

    public record RequestContext(Long userId, String remoteAddr) {}
}
