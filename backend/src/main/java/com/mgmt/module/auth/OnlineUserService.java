package com.mgmt.module.auth;

import com.mgmt.module.auth.dto.OnlineUserVO;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import com.mgmt.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线用户：从 Redis 会话中读取登录时间与 IP，按用户聚合展示。
 */
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public List<OnlineUserVO> list() {
        Map<Long, OnlineUserVO> onlineMap = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(AuthInterceptor.ONLINE_KEY_PREFIX + "*")
                .count(200)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String token = key.substring(AuthInterceptor.ONLINE_KEY_PREFIX.length());
                // token 已失效时顺手清理残留的会话记录
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(AuthInterceptor.TOKEN_KEY_PREFIX + token))) {
                    redisTemplate.delete(key);
                    continue;
                }

                Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
                Long userId = parseLong(session.get("userId"));
                if (userId == null) {
                    continue;
                }

                LocalDateTime loginTime = toTime(parseLong(session.get("loginTime")));
                String ip = Objects.toString(session.get("ip"), "");
                OnlineUserVO vo = onlineMap.get(userId);
                if (vo == null) {
                    vo = new OnlineUserVO();
                    vo.setUserId(userId);
                    vo.setLoginTime(loginTime);
                    vo.setIp(ip);
                    vo.setSessionCount(1);
                    onlineMap.put(userId, vo);
                } else {
                    vo.setSessionCount(vo.getSessionCount() + 1);
                    // 多会话时展示最近一次登录的时间与 IP
                    if (loginTime != null && (vo.getLoginTime() == null || loginTime.isAfter(vo.getLoginTime()))) {
                        vo.setLoginTime(loginTime);
                        vo.setIp(ip);
                    }
                }
            }
        }

        if (onlineMap.isEmpty()) {
            return List.of();
        }

        List<SysUser> users = userMapper.selectBatchIds(onlineMap.keySet());
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));
        Set<Long> roleIds = users.stream()
                .map(SysUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> roleCodes = roleIds.isEmpty()
                ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .collect(Collectors.toMap(SysRole::getId, SysRole::getCode));

        List<OnlineUserVO> result = new ArrayList<>();
        for (OnlineUserVO vo : onlineMap.values()) {
            SysUser user = userMap.get(vo.getUserId());
            if (user == null) {
                continue;
            }
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setRole(roleCodes.getOrDefault(user.getRoleId(), ""));
            result.add(vo);
        }
        result.sort(Comparator.comparing(OnlineUserVO::getLoginTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime toTime(Long millis) {
        if (millis == null || millis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}
