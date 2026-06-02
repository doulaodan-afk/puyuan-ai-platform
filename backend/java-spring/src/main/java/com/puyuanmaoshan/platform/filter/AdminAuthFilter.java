package com.puyuanmaoshan.platform.filter;

import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 平台管理员认证过滤器
 * 处理 /api/v1/admin/* 路径的平台管理员请求
 */
@Component
@Order(3) // 在 TenantContextFilter 之后执行
public class AdminAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 只处理管理端 API
        if (!requestPath.startsWith("/api/v1/admin/") && !requestPath.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = request.getHeader("Authorization");
            Long userId = null;

            if (authHeader != null && !authHeader.isBlank()) {
                try {
                    userId = RequestContextUtil.parseUserIdFromToken(authHeader);
                } catch (AppException e) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "invalid token");
                }
            }

            // 设置请求上下文
            RequestContextUtil.setContext(new RequestContextUtil.RequestContext(userId, getClientIp(request)));

            try {
                logger.debug("Admin API access: path={}, userId={}", requestPath, userId);
                filterChain.doFilter(request, response);
            } finally {
                RequestContextUtil.clearContext();
            }

        } catch (AppException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Admin auth filter error", e);
            handleException(response,
                    new AppException(ErrorCode.INTERNAL_ERROR, "admin auth failed: " + e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void handleException(HttpServletResponse response, AppException e) {
        try {
            response.setStatus(e.getErrorCode().httpStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            String body = String.format(
                    "{\"code\":%d,\"message\":\"%s\",\"data\":null,\"request_id\":null}",
                    e.getErrorCode().code(),
                    e.getMessage()
            );
            response.getWriter().write(body);
        } catch (IOException ioException) {
            logger.error("write response failed", ioException);
        }
    }
}