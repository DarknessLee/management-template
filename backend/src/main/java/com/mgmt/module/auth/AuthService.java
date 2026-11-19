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
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import com.mgmt.security.AuthInterceptor;
import com.mgmt.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PermissionService permissionService;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        // 用户不存在和密码错误返回同一提示，避免被枚举出有效用户名
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(400, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(400, "账号已被禁用，请联系管理员");
        }

        LoginLog log = new LoginLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);

        String token = jwtUtil.generate(user.getId(), log.getId());
        redisTemplate.opsForValue().set(
                AuthInterceptor.TOKEN_KEY_PREFIX + token,
                String.valueOf(user.getId()),
                Duration.ofMinutes(jwtUtil.getExpireMinutes()));
        // 同时记录到用户会话集合，便于改密码 / 强制下线时一次性清理所有 token
        String sessionKey = AuthInterceptor.USER_TOKENS_KEY_PREFIX + user.getId();
        redisTemplate.opsForSet().add(sessionKey, token);
        redisTemplate.expire(sessionKey, Duration.ofMinutes(jwtUtil.getExpireMinutes()));

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

    @Transactional
    public void logout(String token) {
        // 先删白名单，token 立即失效；再回填退出时间用于统计在线时长
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
