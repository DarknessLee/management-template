package com.mgmt.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.PageResult;
import com.mgmt.module.auth.entity.LoginLog;
import com.mgmt.module.auth.mapper.LoginLogMapper;
import com.mgmt.module.role.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;
    private final DataScopeService dataScopeService;

    public PageResult<LoginLog> page(long page, long size, String username,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .ge(startTime != null, LoginLog::getLoginTime, startTime)
                .le(endTime != null, LoginLog::getLoginTime, endTime)
                .orderByDesc(LoginLog::getLoginTime)
                .orderByDesc(LoginLog::getId);

        // 与操作日志一致，按当前用户角色的数据范围过滤
        List<Long> visibleUserIds = dataScopeService.visibleUserIds();
        if (visibleUserIds != null) {
            if (visibleUserIds.isEmpty()) {
                wrapper.eq(LoginLog::getId, -1L);
            } else {
                wrapper.in(LoginLog::getUserId, visibleUserIds);
            }
        }

        IPage<LoginLog> result = loginLogMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result, result.getRecords());
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
