package com.mgmt.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mgmt.common.BizException;
import com.mgmt.common.UserContext;
import com.mgmt.module.auth.dto.LoginRequest;
import com.mgmt.module.auth.dto.LoginResponse;
import com.mgmt.module.auth.dto.UserInfoVO;
import com.mgmt.module.auth.entity.LoginLog;
import com.mgmt.module.auth.mapper.LoginLogMapper;
import com.mgmt.module.role.PermissionService;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.setting.SettingService;
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

    private static final String FAIL_KEY_PREFIX = "mgmt:auth:login-fail:";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PermissionService permissionService;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaService captchaService;
    private final SettingService settingService;

    @Value("${app.login.max-fail:5}")
    private int maxFail;

    @Value("${app.login.fail-window-minutes:15}")
    private int failWindowMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip) {
        // 先验验证码：拦掉脚本化的无限次尝试，再谈账号锁定
        captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode());

        Map<String, Object> security = settingService.security();
        boolean lockOnFail = boolValue(security, "lockOnFail", true);
        int maxFailure = intValue(security, "maxFailCount", maxFail);

        String failKey = FAIL_KEY_PREFIX + request.getUsername();
        int fails = currentFails(failKey);
        if (lockOnFail && fails >= maxFailure) {
            throw new BizException(400, "失败次数过多，请 " + remainingMinutes(failKey) + " 分钟后再试");
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        // 用户不存在和密码错误返回同一提示，避免被枚举出有效用户名
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            if (!lockOnFail) {
                throw new BizException(400, "用户名或密码错误");
            }
            int remaining = recordFailure(failKey, fails, maxFailure);
            if (remaining <= 0) {
                throw new BizException(400, "失败次数过多，请 " + failWindowMinutes + " 分钟后再试");
            }
            throw new BizException(400, "用户名或密码错误，还可尝试 " + remaining + " 次");
        }
        // 登录成功清空计数，避免历史失败影响下一次
        redisTemplate.delete(failKey);
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(400, "账号已被禁用，请联系管理员");
        }

        LoginLog log = new LoginLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);

        String token = jwtUtil.generate(user.getId(), log.getId());
        // 会话有效期取「设置中的登录超时」与 JWT 有效期中的较小值
        int loginTimeout = intValue(security, "loginTimeout", (int) jwtUtil.getExpireMinutes());
        long ttlMinutes = loginTimeout > 0
                ? Math.min(jwtUtil.getExpireMinutes(), loginTimeout)
                : jwtUtil.getExpireMinutes();
        Duration ttl = Duration.ofMinutes(Math.max(ttlMinutes, 1));
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

    public UserInfoVO userInfo() {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BizException(401, "登录已过期，请重新登录");
        }

        SysRole role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(role == null ? "" : role.getCode());
        vo.setPermissions(permissionService.permissionsOf(user.getId()));
        vo.setAvatar(user.getAvatar() == null ? "" : user.getAvatar());
        return vo;
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
    private int recordFailure(String key, int currentFails, int maxFailure) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(failWindowMinutes));
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

    private long remainingMinutes(String key) {
        Long seconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (seconds == null || seconds < 0) {
            return failWindowMinutes;
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
