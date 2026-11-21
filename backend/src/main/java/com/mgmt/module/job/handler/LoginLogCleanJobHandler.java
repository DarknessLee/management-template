package com.mgmt.module.job.handler;

import com.mgmt.module.auth.LoginLogService;
import com.mgmt.module.job.JobHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 清理超过保留天数的登录日志。
 */
@Component
@RequiredArgsConstructor
public class LoginLogCleanJobHandler implements JobHandler {

    private final LoginLogService loginLogService;

    @Value("${app.login-log.retention-days:90}")
    private int retentionDays;

    @Override
    public String key() {
        return "login-log-clean";
    }

    @Override
    public String name() {
        return "登录日志清理";
    }

    @Override
    public String description() {
        return "删除保留天数之前的登录日志，保留天数读取 app.login-log.retention-days";
    }

    @Override
    public String execute() {
        LocalDateTime before = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        int deleted = loginLogService.deleteBefore(before, 1000);
        return "删除 " + deleted + " 条，保留 " + retentionDays + " 天";
    }
}
