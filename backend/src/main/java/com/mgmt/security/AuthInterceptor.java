package com.mgmt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmt.common.Result;
import com.mgmt.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录拦截器：校验 JWT 签名 + Redis 白名单。
 * 用 HandlerInterceptor 而不是 Spring Security 过滤器链，
 * 是为了让"一次请求怎么被拦下来"这件事完全可见、可调试。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String TOKEN_KEY_PREFIX = "mgmt:auth:token:";
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
            UserContext.setUserId(Long.valueOf(claims.getSubject()));
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
}
