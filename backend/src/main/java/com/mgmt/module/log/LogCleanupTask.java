package com.mgmt.module.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志清理任务。
 * 只做"何时执行、保留多久"，删除细节交给 Service，方便单独测试和复用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupTask {

    private final OperationLogService logService;

    @Value("${app.log.retention-days:30}")
    private int retentionDays;

    @Value("${app.log.cleanup-batch-size:1000}")
    private int batchSize;

    @Scheduled(cron = "${app.log.cleanup-cron:0 0 3 * * ?}")
    public void cleanup() {
        // 以"保留天数前的零点"为界，保留最近 N 个完整自然日
        LocalDateTime before = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        try {
            int deleted = logService.deleteBefore(before, batchSize);
            log.info("操作日志清理完成：删除 {} 条，保留 {} 天", deleted, retentionDays);
        } catch (Exception e) {
            // 定时任务抛异常不会影响下一次触发，但必须留下错误日志
            log.error("操作日志清理失败", e);
        }
    }
}
