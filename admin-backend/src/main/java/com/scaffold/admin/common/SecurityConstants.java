package com.scaffold.admin.common;

/**
 * 安全相关常量：白名单路径等
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    /**
     * 不需要认证的公开路径（Spring Security permitAll + 权限过滤器跳过）
     */
    public static final String[] PUBLIC_PATHS = {
        "/health",
        "/api/admin/auth/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api-docs/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/openapi.json",
        "/error",
        "/api/admin/system-config/public"
    };
}
