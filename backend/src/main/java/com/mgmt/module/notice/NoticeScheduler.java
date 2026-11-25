package com.mgmt.module.notice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 公告定时任务：每 30 秒检查一次，到点自动上线、到期自动下线。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeScheduler {

    private final NoticeService noticeService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void applySchedule() {
        try {
            int published = noticeService.applySchedule();
            if (published > 0) {
                log.info("定时公告自动发布 {} 条", published);
            }
        } catch (Exception e) {
            log.error("公告定时任务执行失败", e);
        }
    }
}
