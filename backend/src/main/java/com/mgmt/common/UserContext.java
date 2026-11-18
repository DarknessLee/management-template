package com.mgmt.common;

/**
 * 当前请求登录用户上下文。
 * 拦截器校验 token 后写入，Controller/Service 直接读取，请求结束由拦截器清理，
 * 避免把 userId 一层层当参数传递。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
