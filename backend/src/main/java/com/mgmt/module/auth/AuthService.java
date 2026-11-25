package com.mgmt.module.auth;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mgmt.common.BizException;
import com.mgmt.common.UserContext;
import com.mgmt.module.auth.dto.LoginRequest;
import com.mgmt.module.auth.dto.LoginResponse;
import com.mgmt.module.auth.dto.UserInfoVO;
import com.mgmt.module.auth.entity.LoginLog;
import com.mgmt.module.auth.mapper.LoginLogMapper;
import com.mgmt.module.message.MessageService;
import com.mgmt.module.message.MessageSocketRegistry;
import com.mgmt.module.role.PermissionService;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.setting.SettingService;
import com.mgmt.module.user.PasswordPolicyService;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import com.mgmt.security.AuthInterceptor;
import com.mgmt.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String FAIL_KEY_PREFIX = "mgmt:auth:login-fail:";
    /** 剩余有效期不足该值时自动续期 */
    private static final long RENEW_BEFORE_SECONDS = 600;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PermissionService permissionService;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaService captchaService;
    private final SettingService settingService;
    private final PasswordPolicyService passwordPolicyService;
    private final MessageService messageService;
    private final MessageSocketRegistry messageSocketRegistry;
    private final LoginAuditService loginAuditService;

    @Value("${app.login.max-fail:5}")
    private int maxFail;

    @Value("${app.login.fail-window-minutes:15}")
    private int failWindowMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip) {
        // 先验验证码：拦掉脚本化的无限次尝试，再谈账号锁定
        try {
            captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode());
        } catch (BizException e) {
            loginAuditService.recordFailure(request.getUsername(), null, e.getMessage());
            throw e;
        }

        Map<String, Object> security = settingService.security();
        boolean lockOnFail = boolValue(security, "lockOnFail", true);
        int maxFailure = intValue(security, "maxFailCount", maxFail);
        int lockMinutes = intValue(security, "lockMinutes", failWindowMinutes);

        String failKey = FAIL_KEY_PREFIX + request.getUsername();
        int fails = currentFails(failKey);
        if (lockOnFail && fails >= maxFailure) {
            String message = I18n.get("auth.tooManyFailures", remainingMinutes(failKey, lockMinutes));
            loginAuditService.recordFailure(request.getUsername(), null, message);
            throw new BizException(400, message);
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        // 用户不存在和密码错误返回同一提示，避免被枚举出有效用户名
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            Long userId = user == null ? null : user.getId();
            if (!lockOnFail) {
                String message = I18n.get("auth.badCredentials");
                loginAuditService.recordFailure(request.getUsername(), userId, message);
                throw new BizException(400, message);
            }
            int remaining = recordFailure(failKey, fails, maxFailure, lockMinutes);
            String message = remaining <= 0
                    ? I18n.get("auth.tooManyFailures", lockMinutes)
                    : I18n.get("auth.badCredentialsRemaining", remaining);
            // 日志里记录稳定的失败原因，避免“剩余次数”差异导致统计被拆散
            loginAuditService.recordFailure(request.getUsername(), userId, I18n.get("auth.badCredentials"));
            throw new BizException(400, message);
        }
        // 登录成功清空计数，避免历史失败影响下一次
        redisTemplate.delete(failKey);
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            String message = I18n.get("auth.accountDisabled");
            loginAuditService.recordFailure(request.getUsername(), user.getId(), message);
            throw new BizException(400, message);
        }

        LoginLog log = new LoginLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setLoginTime(LocalDateTime.now());
        log.setSuccess(1);
        loginLogMapper.insert(log);

        String token = jwtUtil.generate(user.getId(), log.getId(), Boolean.TRUE.equals(request.getRememberMe()));
        // 会话有效期取「设置中的登录超时」与 JWT 有效期中的较小值；记住我时改用 remember-days
        int loginTimeout = intValue(security, "loginTimeout", (int) jwtUtil.getExpireMinutes());
        Duration ttl = sessionTtl(Boolean.TRUE.equals(request.getRememberMe()), loginTimeout);
        redisTemplate.opsForValue().set(
                AuthInterceptor.TOKEN_KEY_PREFIX + token,
                String.valueOf(user.getId()),
                ttl);
        // 同时记录到用户会话集合，便于改密码 / 强制下线时一次性清理所有 token
        String sessionKey = AuthInterceptor.USER_TOKENS_KEY_PREFIX + user.getId();
        redisTemplate.opsForSet().add(sessionKey, token);
        redisTemplate.expire(sessionKey, ttl);

        // 会话详情供在线用户列表使用：登录时间 + 来源 IP
        Map<String, String> online = new HashMap<>();
        online.put("userId", String.valueOf(user.getId()));
        online.put("loginTime", String.valueOf(System.currentTimeMillis()));
        online.put("ip", ip == null ? "" : ip);
        String onlineKey = AuthInterceptor.ONLINE_KEY_PREFIX + token;
        redisTemplate.opsForHash().putAll(onlineKey, online);
        redisTemplate.expire(onlineKey, ttl);

        return new LoginResponse(token);
    }

    /**
     * 临期自动续期：剩余有效期不足 {@link #RENEW_BEFORE_SECONDS} 时签发新 token，
     * 并把 Redis 中的白名单、用户会话集合、在线信息交接过去；无需续期时返回 null。
     */
    public String renewIfNeeded(String token, Long userId) {
        Long remaining = redisTemplate.getExpire(AuthInterceptor.TOKEN_KEY_PREFIX + token, TimeUnit.SECONDS);
        if (remaining == null || remaining < 0 || remaining > RENEW_BEFORE_SECONDS) {
            return null;
        }
        Claims claims = jwtUtil.parse(token);
        boolean remember = Boolean.TRUE.equals(claims.get("rm", Boolean.class));
        Object loginLogId = claims.get("loginLogId");
        String newToken = jwtUtil.generate(userId,
                loginLogId == null ? null : Long.valueOf(loginLogId.toString()), remember);

        int loginTimeout = intValue(settingService.security(), "loginTimeout", (int) jwtUtil.getExpireMinutes());
        Duration ttl = sessionTtl(remember, loginTimeout);

        redisTemplate.opsForValue().set(AuthInterceptor.TOKEN_KEY_PREFIX + newToken, String.valueOf(userId), ttl);
        String sessionKey = AuthInterceptor.USER_TOKENS_KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(sessionKey, newToken);
        redisTemplate.expire(sessionKey, ttl);

        // 登录时间与来源 IP 原样交接，避免续期后在线用户列表丢失会话
        String oldOnlineKey = AuthInterceptor.ONLINE_KEY_PREFIX + token;
        Map<Object, Object> online = redisTemplate.opsForHash().entries(oldOnlineKey);
        if (!online.isEmpty()) {
            redisTemplate.opsForHash().putAll(AuthInterceptor.ONLINE_KEY_PREFIX + newToken, online);
            redisTemplate.expire(AuthInterceptor.ONLINE_KEY_PREFIX + newToken, ttl);
        }
        redisTemplate.delete(oldOnlineKey);

        // 旧 token 立即失效，并从该用户的会话集合中移除
        redisTemplate.delete(AuthInterceptor.TOKEN_KEY_PREFIX + token);
        redisTemplate.opsForSet().remove(sessionKey, token);
        return newToken;
    }

    private Duration sessionTtl(boolean remember, int loginTimeout) {
        if (remember) {
            return Duration.ofDays(Math.max(jwtUtil.getRememberDays(), 1));
        }
        long ttlMinutes = loginTimeout > 0
                ? Math.min(jwtUtil.getExpireMinutes(), loginTimeout)
                : jwtUtil.getExpireMinutes();
        return Duration.ofMinutes(Math.max(ttlMinutes, 1));
    }

    public UserInfoVO userInfo() {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BizException(401, I18n.get("common.unauthorized"));
        }

        SysRole role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(role == null ? "" : role.getCode());
        vo.setPermissions(permissionService.permissionsOf(user.getId()));
        vo.setAvatar(user.getAvatar() == null ? "" : user.getAvatar());
        vo.setMustChangePassword(Integer.valueOf(1).equals(user.getMustChangePassword())
                || passwordPolicyService.isExpired(user.getPasswordUpdateTime()));
        return vo;
    }

    /**
     * 强制下线：清理全部会话后留下一条站内消息，并关闭该用户已建立的消息连接。
     * 与修改密码共用 {@link #invalidateAllSessions(Long)}，但不发消息。
     */
    public int kickout(Long userId) {
        int count = invalidateAllSessions(userId);
        messageService.send(userId, MessageService.TYPE_KICK,
                I18n.get("message.kickTitle"),
                I18n.get("message.kickContent"),
                null);
        messageSocketRegistry.close(userId);
        return count;
    }

    /**
     * 一次性失效某个用户的全部会话，供"强制下线"和"修改密码"复用。
     * 返回被清理的 token 数量，便于前端提示。
     */
    public int invalidateAllSessions(Long userId) {
        String sessionKey = AuthInterceptor.USER_TOKENS_KEY_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(sessionKey);
        if (tokens == null || tokens.isEmpty()) {
            redisTemplate.delete(sessionKey);
            return 0;
        }
        tokens.forEach(token -> {
            redisTemplate.delete(AuthInterceptor.TOKEN_KEY_PREFIX + token);
            redisTemplate.delete(AuthInterceptor.ONLINE_KEY_PREFIX + token);
        });
        redisTemplate.delete(sessionKey);
        return tokens.size();
    }

    private int currentFails(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     * 失败计数写在 Redis：过期即自动解锁，无需定时任务清理。
     * 只有第一次失败时设置过期时间，后续失败只做自增，避免每次失败都把窗口往后推。
     */
    private int recordFailure(String key, int currentFails, int maxFailure, int lockMinutes) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(lockMinutes));
        }
        int total = count == null ? currentFails + 1 : count.intValue();
        return Math.max(maxFailure - total, 0);
    }

    private int intValue(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private boolean boolValue(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private long remainingMinutes(String key, int fallbackMinutes) {
        Long seconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (seconds == null || seconds < 0) {
            return fallbackMinutes;
        }
        return Math.max((seconds + 59) / 60, 1);
    }

    @Transactional
    public void logout(String token) {
        // 先删白名单与会话记录，token 立即失效；再回填退出时间用于统计在线时长
        redisTemplate.delete(AuthInterceptor.ONLINE_KEY_PREFIX + token);
        redisTemplate.delete(AuthInterceptor.TOKEN_KEY_PREFIX + token);
        try {
            Claims claims = jwtUtil.parse(token);
            Object loginLogId = claims.get("loginLogId");
            if (loginLogId != null) {
                loginLogMapper.update(null, new LambdaUpdateWrapper<LoginLog>()
                        .eq(LoginLog::getId, Long.valueOf(loginLogId.toString()))
                        .isNull(LoginLog::getLogoutTime)
                        .set(LoginLog::getLogoutTime, LocalDateTime.now()));
            }
            Long userId = Long.valueOf(claims.getSubject());
            redisTemplate.opsForSet().remove(AuthInterceptor.USER_TOKENS_KEY_PREFIX + userId, token);
        } catch (Exception ignored) {
            // token 已失效时没必要让退出接口失败
        }
    }
}
