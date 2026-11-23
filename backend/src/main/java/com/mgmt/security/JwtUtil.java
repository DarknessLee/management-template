package com.mgmt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 只负责"签名防篡改"，不负责"是否仍然有效"。
 * 有效性交给 Redis 白名单：退出登录 / 强制下线只需删 key，无需等 JWT 自然过期。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-minutes}")
    private long expireMinutes;

    /** 勾选「记住我」时的会话有效期（天） */
    @Value("${app.jwt.remember-days:7}")
    private long rememberDays;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, Long loginLogId, boolean remember) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireMillis(remember));
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("loginLogId", loginLogId)
                // 续期时据此判断新的会话应该沿用哪种有效期
                .claim("rm", remember)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private long expireMillis(boolean remember) {
        long minutes = remember ? Math.max(rememberDays, 1) * 24 * 60 : expireMinutes;
        return minutes * 60 * 1000;
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpireMinutes() {
        return expireMinutes;
    }

    public long getRememberDays() {
        return rememberDays;
    }
}
