package com.scaffold.admin.util;

import com.scaffold.admin.service.impl.AdminUserServiceImpl.AdminUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类：统一获取当前登录用户信息
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前登录用户详情，未认证时返回 null
     */
    public static AdminUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }

    /**
     * 获取当前登录用户ID，未认证时返回 null
     */
    public static Long getCurrentUserId() {
        AdminUserDetails user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 判断当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isSuperuser() {
        AdminUserDetails user = getCurrentUser();
        return user != null && Integer.valueOf(1).equals(user.getIsSuperuser());
    }
}
