package com.mgmt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmt.common.Result;
import com.mgmt.common.UserContext;
import com.mgmt.module.role.PermissionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录 + 权限拦截器：校验 JWT 签名、Redis 白名单，再校验 @RequirePermission。
 * 用 HandlerInterceptor 而不是 Spring Security 过滤器链，
 * 是为了让"一次请求怎么被拦下来"这件事完全可见、可调试。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String TOKEN_KEY_PREFIX = "mgmt:auth:token:";
    /** 用户 -> 该用户所有有效 token 的集合，用于批量失效 */
    public static final String USER_TOKENS_KEY_PREFIX = "mgmt:auth:user-tokens:";
    public static final String ONLINE_KEY_PREFIX = "mgmt:auth:online:";
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return unauthorized(response);
        }

        String token = header.substring(BEARER.length());
        try {
            Claims claims = jwtUtil.parse(token);
            // 白名单不存在说明已退出登录或已被强制下线
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_KEY_PREFIX + token))) {
                return unauthorized(response);
            }
            Long userId = Long.valueOf(claims.getSubject());

            // 先校验权限再写 ThreadLocal：preHandle 返回 false 时 afterCompletion 不会执行，
            // 提前写入会导致线程复用时残留上一个用户的信息
            RequirePermission required = handler instanceof HandlerMethod handlerMethod
                    ? handlerMethod.getMethodAnnotation(RequirePermission.class)
                    : null;
            if (required != null && !permissionService.hasPermission(userId, required.value())) {
                return forbidden(response);
            }

            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            return unauthorized(response);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, "登录已过期，请重新登录")));
        return false;
    }

    /** 已登录但没有该操作权限：返回 403，页面不应跳登录页 */
    private boolean forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(403, "没有操作权限")));
        return false;
    }
}
