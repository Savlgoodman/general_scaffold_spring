package com.scaffold.admin.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址提取工具
 */
public final class IpUtils {

    private IpUtils() {}

    /**
     * 从请求中提取客户端真实 IP
     * 优先级：X-Forwarded-For > Proxy-Client-IP > WL-Proxy-Client-IP > getRemoteAddr()
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isBlank(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank() || "unknown".equalsIgnoreCase(str);
    }
}
