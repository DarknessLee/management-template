package com.mgmt.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.PageResult;
import com.mgmt.module.auth.dto.LoginLogStatsVO;
import com.mgmt.module.auth.dto.LoginSecurityVO;
import com.mgmt.module.auth.entity.LoginLog;
import com.mgmt.module.auth.mapper.LoginLogMapper;
import com.mgmt.module.role.DataScopeService;
import com.mgmt.module.setting.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;
    private final DataScopeService dataScopeService;
    private final StringRedisTemplate redisTemplate;
    private final SettingService settingService;

    public PageResult<LoginLog> page(long page, long size, String username, Integer success,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        IPage<LoginLog> result = loginLogMapper.selectPage(new Page<>(page, size),
                searchWrapper(username, success, startTime, endTime)
                        .orderByDesc(LoginLog::getLoginTime)
                        .orderByDesc(LoginLog::getId));
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 导出与列表共用筛选条件和数据范围，避免导出越权数据。
     */
    public List<LoginLog> listForExport(String username, Integer success,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogMapper.selectList(searchWrapper(username, success, startTime, endTime)
                .orderByDesc(LoginLog::getLoginTime)
                .orderByDesc(LoginLog::getId));
    }

    /**
     * 当前被临时锁定的账号：直接读取 Redis 失败计数，剩余时间即计数键的 TTL。
     */
    public LoginSecurityVO security() {
        LoginSecurityVO vo = new LoginSecurityVO();
        vo.setMaxFailCount(intValue(settingService.security().get("maxFailCount"), 5));
        vo.setLockMinutes(intValue(settingService.security().get("lockMinutes"), 15));

        List<LoginSecurityVO.LockedAccount> accounts = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(AuthService.FAIL_KEY_PREFIX + "*")
                .count(200)
                .build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                int failures = intValue(redisTemplate.opsForValue().get(key), 0);
                if (failures < vo.getMaxFailCount()) {
                    continue;
                }
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                LoginSecurityVO.LockedAccount account = new LoginSecurityVO.LockedAccount();
                account.setUsername(key.substring(AuthService.FAIL_KEY_PREFIX.length()));
                account.setFailures(failures);
                account.setRemainingSeconds(ttl == null || ttl < 0 ? 0 : ttl);
                accounts.add(account);
            }
        }
        accounts.sort(Comparator.comparingLong(LoginSecurityVO.LockedAccount::getRemainingSeconds));
        vo.setLockedAccounts(accounts);
        return vo;
    }

    /**
     * 解除指定账号的临时锁定，返回删除的计数键数量。
     */
    public long unlock(String username) {
        if (!StringUtils.hasText(username)) {
            return 0;
        }
        Boolean deleted = redisTemplate.delete(AuthService.FAIL_KEY_PREFIX + username);
        return Boolean.TRUE.equals(deleted) ? 1 : 0;
    }

    /**
     * 今日登录统计：成功/失败次数、失败率以及失败原因 Top3。
     */
    public LoginLogStatsVO stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        LoginLogStatsVO vo = new LoginLogStatsVO();
        vo.setTodaySuccess(countWithScope(1, start, end));
        vo.setTodayFailed(countWithScope(0, start, end));
        vo.setTodayTotal(vo.getTodaySuccess() + vo.getTodayFailed());
        if (vo.getTodayTotal() > 0) {
            double rate = vo.getTodayFailed() * 100.0 / vo.getTodayTotal();
            vo.setFailureRate(Math.round(rate * 10) / 10.0);
        }

        QueryWrapper<LoginLog> wrapper = rangeWrapper(0, start, end);
        if (wrapper != null) {
            wrapper.select("fail_reason AS reason", "COUNT(*) AS cnt")
                    .isNotNull("fail_reason")
                    .ne("fail_reason", "")
                    .groupBy("fail_reason")
                    .orderByDesc("cnt")
                    .last("LIMIT 3");
            vo.setTopFailReasons(loginLogMapper.selectMaps(wrapper).stream()
                    .map(row -> new LoginLogStatsVO.FailReason(
                            String.valueOf(row.get("reason")),
                            ((Number) row.get("cnt")).longValue()))
                    .toList());
        }
        return vo;
    }

    private long countWithScope(int success, LocalDateTime start, LocalDateTime end) {
        QueryWrapper<LoginLog> wrapper = rangeWrapper(success, start, end);
        return wrapper == null ? 0 : loginLogMapper.selectCount(wrapper);
    }

    /**
     * 按成功标识和时间范围构造查询，并套用当前角色的数据范围；
     * 返回 null 表示当前角色看不到任何数据。
     */
    private QueryWrapper<LoginLog> rangeWrapper(int success, LocalDateTime start, LocalDateTime end) {
        QueryWrapper<LoginLog> wrapper = new QueryWrapper<>();
        wrapper.eq("success", success)
                .ge("login_time", start)
                .lt("login_time", end);
        List<Long> visibleUserIds = dataScopeService.visibleUserIds();
        if (visibleUserIds != null) {
            if (visibleUserIds.isEmpty()) {
                return null;
            }
            wrapper.in("user_id", visibleUserIds);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<LoginLog> searchWrapper(String username, Integer success,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .eq(success != null, LoginLog::getSuccess, success)
                .ge(startTime != null, LoginLog::getLoginTime, startTime)
                .le(endTime != null, LoginLog::getLoginTime, endTime);

        // 与操作日志一致，按当前用户角色的数据范围过滤
        List<Long> visibleUserIds = dataScopeService.visibleUserIds();
        if (visibleUserIds != null) {
            if (visibleUserIds.isEmpty()) {
                wrapper.eq(LoginLog::getId, -1L);
            } else {
                wrapper.in(LoginLog::getUserId, visibleUserIds);
            }
        }
        return wrapper;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 分批删除指定时间之前的登录日志。
     * 登录日志只增不改，删除时没有业务逻辑需要联动，所以这里只做分批控制。
     */
    public int deleteBefore(LocalDateTime before, int batchSize) {
        int total = 0;
        int rounds = 0;
        int deleted;
        do {
            deleted = loginLogMapper.deleteBefore(before, batchSize);
            total += deleted;
            rounds++;
        } while (deleted == batchSize && rounds < 100);
        return total;
    }
}
