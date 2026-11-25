package com.mgmt.module.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.UserContext;
import com.mgmt.module.auth.mapper.LoginLogMapper;
import com.mgmt.module.dashboard.dto.DashboardVO;
import com.mgmt.module.dept.mapper.SysDeptMapper;
import com.mgmt.module.file.mapper.SysFileMapper;
import com.mgmt.module.job.entity.SysJob;
import com.mgmt.module.job.mapper.SysJobMapper;
import com.mgmt.module.log.entity.SysOperationLog;
import com.mgmt.module.log.mapper.SysOperationLogMapper;
import com.mgmt.module.notice.entity.SysNotice;
import com.mgmt.module.notice.entity.SysNoticeRead;
import com.mgmt.module.notice.mapper.SysNoticeMapper;
import com.mgmt.module.notice.mapper.SysNoticeReadMapper;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_DAYS = 7;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final SysFileMapper fileMapper;
    private final SysJobMapper jobMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysNoticeReadMapper noticeReadMapper;
    private final LoginLogMapper loginLogMapper;
    private final SysOperationLogMapper operationLogMapper;

    public DashboardVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(TREND_DAYS - 1L).atStartOfDay();
        LocalDateTime todayStart = today.atStartOfDay();

        DashboardVO vo = new DashboardVO();
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setEnabledUsers(userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)));
        vo.setDisabledUsers(Math.max(0, vo.getTotalUsers() - vo.getEnabledUsers()));
        vo.setTotalRoles(roleMapper.selectCount(null));
        vo.setTotalDepts(deptMapper.selectCount(null));
        vo.setTotalFiles(fileMapper.selectCount(null));
        vo.setPublishedNotices(noticeMapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)));
        vo.setUnreadNotices(unreadNotices(UserContext.getUserId()));
        vo.setTotalJobs(jobMapper.selectCount(null));
        vo.setEnabledJobs(jobMapper.selectCount(new LambdaQueryWrapper<SysJob>()
                .eq(SysJob::getStatus, 1)));
        vo.setTodayLogin(loginLogMapper.countTodayLogin());
        vo.setAvgLoginTime(formatMinutes(loginLogMapper.avgLoginMinutes(start)));

        vo.setTodayOperations(operationLogMapper.selectCount(new LambdaQueryWrapper<SysOperationLog>()
                .ge(SysOperationLog::getCreateTime, todayStart)));
        vo.setFailedOperations(operationLogMapper.selectCount(new LambdaQueryWrapper<SysOperationLog>()
                .ge(SysOperationLog::getCreateTime, start)
                .eq(SysOperationLog::getSuccess, 0)));

        Map<LocalDate, Long> loginByDay = loginLogMapper.countLoginByDay(start).stream()
                .collect(Collectors.toMap(
                        row -> toLocalDate(row.get("day")),
                        row -> ((Number) row.get("cnt")).longValue()));
        // 补齐没有登录的日期，保证趋势图横轴稳定
        List<DashboardVO.TrendPoint> trend = IntStream.range(0, TREND_DAYS)
                .mapToObj(offset -> {
                    LocalDate date = today.minusDays(TREND_DAYS - 1L - offset);
                    return new DashboardVO.TrendPoint(
                            date.format(DAY_LABEL),
                            loginByDay.getOrDefault(date, 0L));
                })
                .toList();
        vo.setLoginTrend(trend);

        Map<LocalDate, long[]> operationByDay = operationLogMapper.countOperationByDay(start).stream()
                .collect(Collectors.toMap(
                        row -> toLocalDate(row.get("day")),
                        row -> {
                            long total = ((Number) row.get("total")).longValue();
                            long success = ((Number) row.get("successCnt")).longValue();
                            return new long[]{success, total - success};
                        }));
        // 与登录趋势保持一致，补齐近 7 天没有操作的日期
        vo.setOperationTrend(IntStream.range(0, TREND_DAYS)
                .mapToObj(offset -> {
                    LocalDate date = today.minusDays(TREND_DAYS - 1L - offset);
                    long[] counts = operationByDay.getOrDefault(date, new long[]{0L, 0L});
                    return new DashboardVO.OperationTrendPoint(date.format(DAY_LABEL), counts[0], counts[1]);
                })
                .toList());

        vo.setOperationModules(operationLogMapper.countByModule(start).stream()
                .map(row -> new DashboardVO.ModuleCount(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("cnt")).longValue()))
                .toList());

        vo.setRoleDistribution(userMapper.countUsersByRole().stream()
                .map(row -> new DashboardVO.RoleCount(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("value")).longValue()))
                .toList());

        vo.setRecentOperations(operationLogMapper.selectList(new LambdaQueryWrapper<SysOperationLog>()
                        .orderByDesc(SysOperationLog::getCreateTime)
                        .orderByDesc(SysOperationLog::getId)
                        .last("LIMIT 5"))
                .stream()
                .map(log -> {
                    DashboardVO.RecentOperation item = new DashboardVO.RecentOperation();
                    item.setUsername(log.getUsername());
                    item.setModule(log.getModule());
                    item.setAction(log.getAction());
                    item.setSuccess(log.getSuccess());
                    item.setDurationMs(log.getDurationMs());
                    item.setCreateTime(log.getCreateTime());
                    return item;
                })
                .toList());

        vo.setLatestNotices(noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, 1)
                        .orderByDesc(SysNotice::getPinned)
                        .orderByDesc(SysNotice::getPublishTime)
                        .orderByDesc(SysNotice::getId)
                        .last("LIMIT 5"))
                .stream()
                .map(notice -> {
                    DashboardVO.NoticeBrief item = new DashboardVO.NoticeBrief();
                    item.setId(notice.getId());
                    item.setTitle(notice.getTitle());
                    item.setContent(notice.getContent());
                    item.setPinned(notice.getPinned());
                    item.setPublishTime(notice.getPublishTime());
                    return item;
                })
                .toList());

        return vo;
    }

    private long unreadNotices(Long userId) {
        List<Long> readIds = noticeReadMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                        .eq(SysNoticeRead::getUserId, userId))
                .stream()
                .map(SysNoticeRead::getNoticeId)
                .toList();
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1);
        if (!readIds.isEmpty()) {
            wrapper.notIn(SysNotice::getId, readIds);
        }
        return noticeMapper.selectCount(wrapper);
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
