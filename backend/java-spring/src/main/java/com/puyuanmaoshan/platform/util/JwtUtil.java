package com.puyuanmaoshan.platform.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Date;

/**
 * JWT Token 工具类
 *
 * Token 格式兼容性说明：
 * - 新格式：JWT Token（含 userId、tenantId、role 签名信息）
 * - 旧格式："token-{userId}" 或 "token-{userId}-{tenantId}"（向后兼容，逐步淘汰）
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.security.jwt-secret:puyuanmaoshan-default-jwt-secret-change-in-production}")
    private String jwtSecret;

    @Value("${app.security.jwt-expire-hours:168}")
    private int jwtExpireHours;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(jwtSecret);
        this.verifier = JWT.require(algorithm).withIssuer("puyuanmaoshan").build();
        log.info("JwtUtil 初始化完成, token 有效期: {}小时", jwtExpireHours);
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param role     用户角色
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId, Long tenantId, String role) {
        long now = System.currentTimeMillis();
        Date expiresAt = new Date(now + (long) jwtExpireHours * 3600 * 1000);

        return JWT.create()
                .withIssuer("puyuanmaoshan")
                .withSubject(String.valueOf(userId))
                .withClaim("userId", userId)
                .withClaim("tenantId", tenantId)
                .withClaim("role", role != null ? role : "user")
                .withIssuedAt(new Date(now))
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    /**
     * 验证并解析 JWT Token
     *
     * @param token JWT Token 字符串
     * @return 解析后的 Token 信息，验证失败返回 null
     */
    public TokenInfo verifyToken(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return new TokenInfo(
                    jwt.getClaim("userId").asLong(),
                    jwt.getClaim("tenantId").asLong(),
                    jwt.getClaim("role").asString()
            );
        } catch (JWTVerificationException e) {
            log.debug("JWT 验证失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 Token（兼容旧格式和新 JWT 格式）
     *
     * @param token 请求中的 token 字符串
     * @return TokenInfo，旧格式也会包装为 TokenInfo 返回
     */
    public TokenInfo parseToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        // 尝试 JWT 格式解析
        if (token.contains(".")) {
            TokenInfo info = verifyToken(token);
            if (info != null) {
                return info;
            }
        }

        // 兼容旧格式: "token-{userId}" 或 "token-{userId}-{tenantId}"
        if (token.startsWith("token-")) {
            String[] parts = token.substring(6).split("-");
            try {
                Long userId = Long.parseLong(parts[0]);
                Long tenantId = parts.length > 1 ? Long.parseLong(parts[1]) : null;
                return new TokenInfo(userId, tenantId, null);
            } catch (NumberFormatException e) {
                log.warn("解析旧格式 token 失败: {}", token);
                return null;
            }
        }

        return null;
    }

    /**
     * Token 解析结果
     */
    public static class TokenInfo {
        private final Long userId;
        private final Long tenantId;
        private final String role;

        public TokenInfo(Long userId, Long tenantId, String role) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public String getRole() {
            return role;
        }
    }
}