package com.puyuanmaoshan.platform.filter;

import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.TenantMemberService;
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
 * 租户上下文过滤器
 * 验证用户是否在指定租户中有权限，并将租户信息存入 RequestContextUtil
 */
@Component
@Order(2) // 在认证过滤器之后执行
public class TenantContextFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(TenantContextFilter.class);

    private final TenantMemberService tenantMemberService;

    // 不需要租户验证的路径
    private static final String[] SKIP_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/verify",
            "/api/v1/auth/register",
            "/api/v1/tenant/user/tenants",
            "/api/tenant/switch",
            "/api/supplier/register",
            "/api/v1/sms/",
            "/api/admin/",
            "/api/v1/admin/",
            "/api/v1/merchant/auth/login",
            "/api/v1/user/",
            "/health",
            "/actuator"
    };

    public TenantContextFilter(TenantMemberService tenantMemberService) {
        this.tenantMemberService = tenantMemberService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 平台管理员路径直接跳过，不需要租户验证
        if (requestPath.startsWith("/api/v1/admin/") || requestPath.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 跳过不需要租户验证的路径
        if (shouldSkip(requestPath)) {
            // 对于跳过路径，尝试设置用户上下文（从 token 中）
            String userIdHeader = request.getHeader("X-User-Id");
            String authHeader = request.getHeader("Authorization");
            Long userId = null;

            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    userId = RequestContextUtil.parseUserId(userIdHeader);
                } catch (Exception ignored) {}
            } else if (authHeader != null && !authHeader.isBlank()) {
                try {
                    userId = RequestContextUtil.parseUserIdFromToken(authHeader);
                } catch (Exception ignored) {}
            }

            RequestContextUtil.setContext(new RequestContextUtil.RequestContext(userId, getClientIp(request)));
            try {
                filterChain.doFilter(request, response);
            } finally {
                RequestContextUtil.clearContext();
            }
            return;
        }

        try {
            // 获取用户ID和租户ID
            String userIdHeader = request.getHeader("X-User-Id");
            String tenantIdHeader = request.getHeader("X-Tenant-Id");

            // 如果没有 X-User-Id，尝试从 Authorization header 中解析
            if (userIdHeader == null || userIdHeader.isBlank()) {
                String authHeader = request.getHeader("Authorization");
                try {
                    long userIdFromToken = RequestContextUtil.parseUserIdFromToken(authHeader);
                    userIdHeader = String.valueOf(userIdFromToken);
                } catch (AppException e) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "missing X-User-Id header (path=" + requestPath + ")");
                }
            }

            if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "missing X-Tenant-Id header");
            }

            long userId = RequestContextUtil.parseUserId(userIdHeader);
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);

            // 验证用户是否在该租户中有权限
            var tenantUser = tenantMemberService.getUserRoleInTenant(userId, tenantId);

            if (tenantUser == null) {
                logger.warn("用户 {} 尝试访问租户 {}，但无权限", userId, tenantId);
                throw new AppException(ErrorCode.FORBIDDEN, "您不在该工作室中或已被移除");
            }

            if (!tenantUser.isActive()) {
                logger.warn("用户 {} 在租户 {} 中的状态非活跃", userId, tenantId);
                throw new AppException(ErrorCode.FORBIDDEN, "您在该工作室中的账号已被停用");
            }

            // 将租户信息存入请求属性，供后续使用
            request.setAttribute("X-Tenant-Id", tenantId);
            request.setAttribute("X-User-Id", userId);
            request.setAttribute("X-User-Role", tenantUser.getRole());

            // 设置请求上下文（用于审计日志）
            RequestContextUtil.setContext(new RequestContextUtil.RequestContext(userId, getClientIp(request)));

            try {
                logger.debug("租户上下文验证通过: userId={}, tenantId={}, role={}",
                        userId, tenantId, tenantUser.getRole());
                filterChain.doFilter(request, response);
            } finally {
                // 清除请求上下文
                RequestContextUtil.clearContext();
            }

        } catch (AppException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("租户上下文验证失败", e);
            handleException(response,
                    new AppException(ErrorCode.INTERNAL_ERROR, "租户验证失败: " + e.getMessage()));
        }
    }

    private boolean shouldSkip(String path) {
        for (String skipPath : SKIP_PATHS) {
            if (path.startsWith(skipPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端真实 IP 地址
     * 支持通过代理（如 Nginx）转发的情况
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多个 IP（通过代理），取第一个
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
            logger.error("写响应失败", ioException);
        }
    }
}
