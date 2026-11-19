package com.mgmt.module.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 登录日志清理任务。
 * 与操作日志使用独立的保留天数和执行时间：认证事件用于安全审计，保留期更长。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogCleanupTask {

    private final LoginLogService loginLogService;

    @Value("${app.log.login-retention-days:180}")
    private int retentionDays;

    @Value("${app.log.login-cleanup-batch-size:1000}")
    private int batchSize;

    @Scheduled(cron = "${app.log.login-cleanup-cron:0 30 3 * * ?}")
    public void cleanup() {
        LocalDateTime before = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        try {
            int deleted = loginLogService.deleteBefore(before, batchSize);
            log.info("登录日志清理完成：删除 {} 条，保留 {} 天", deleted, retentionDays);
        } catch (Exception e) {
            log.error("登录日志清理失败", e);
        }
    }
}
