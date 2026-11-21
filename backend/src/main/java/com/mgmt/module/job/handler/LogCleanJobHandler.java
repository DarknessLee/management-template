package com.mgmt.module.job.handler;

import com.mgmt.module.job.JobHandler;
import com.mgmt.module.log.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 清理超过保留天数的操作日志。
 */
@Component
@RequiredArgsConstructor
public class LogCleanJobHandler implements JobHandler {

    private final OperationLogService logService;

    @Value("${app.log.retention-days:30}")
    private int retentionDays;

    @Override
    public String key() {
        return "log-clean";
    }

    @Override
    public String name() {
        return "操作日志清理";
    }

    @Override
    public String description() {
        return "删除保留天数之前的操作日志，保留天数读取 app.log.retention-days";
    }

    @Override
    public String execute() {
        LocalDateTime before = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        int deleted = logService.deleteBefore(before, 1000);
        return "删除 " + deleted + " 条，保留 " + retentionDays + " 天";
    }
}
