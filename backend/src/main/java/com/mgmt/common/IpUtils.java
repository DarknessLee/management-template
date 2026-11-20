package com.mgmt.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取：优先取代理头，便于部署在 Nginx 后仍能记录真实来源。
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
