package com.mgmt.module.dashboard;

import com.mgmt.module.auth.mapper.LoginLogMapper;
import com.mgmt.module.dashboard.dto.DashboardVO;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_DAYS = 7;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final LoginLogMapper loginLogMapper;

    public DashboardVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(TREND_DAYS - 1).atStartOfDay();

        DashboardVO vo = new DashboardVO();
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setTotalRoles(roleMapper.selectCount(null));
        vo.setTodayLogin(loginLogMapper.countTodayLogin());
        vo.setAvgLoginTime(formatMinutes(loginLogMapper.avgLoginMinutes(start)));

        Map<LocalDate, Long> newUserByDay = userMapper.countNewUsersByDay(start).stream()
                .collect(Collectors.toMap(
                        row -> toLocalDate(row.get("day")),
                        row -> ((Number) row.get("cnt")).longValue()));

        // 数据库只返回有注册的日期，这里补齐 7 天，保证前端柱状图宽度稳定
        List<Long> trend = IntStream.range(0, TREND_DAYS)
                .mapToObj(offset -> newUserByDay.getOrDefault(today.minusDays(TREND_DAYS - 1L - offset), 0L))
                .toList();
        vo.setUserTrend(trend);

        vo.setRoleDistribution(userMapper.countUsersByRole().stream()
                .map(row -> new DashboardVO.RoleCount(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("value")).longValue()))
                .toList());
        return vo;
    }

    private String formatMinutes(Double minutes) {
        if (minutes == null || minutes <= 0) {
            return "0分钟";
        }
        long total = Math.round(minutes);
        long hours = total / 60;
        long rest = total % 60;
        return hours > 0 ? hours + "小时" + rest + "分" : rest + "分钟";
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
