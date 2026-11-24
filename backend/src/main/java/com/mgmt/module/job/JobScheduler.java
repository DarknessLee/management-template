package com.mgmt.module.job;

import com.mgmt.common.I18n;
import com.mgmt.common.BizException;
import com.mgmt.module.job.entity.SysJob;
import com.mgmt.module.message.MessageService;
import com.mgmt.module.job.mapper.SysJobMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * 定时任务调度：启动时加载启用状态的任务，增删改后重新注册。
 * 处理器只能来自代码中注册的 JobHandler，数据库仅保存调度配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobScheduler {

    private final SysJobMapper jobMapper;
    private final List<JobHandler> jobHandlers;
    private final JobLogService jobLogService;
    private final MessageService messageService;

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private Map<String, JobHandler> handlerMap = Map.of();

    @PostConstruct
    public void init() {
        handlerMap = jobHandlers.stream()
                .collect(Collectors.toMap(JobHandler::key, handler -> handler));

        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("mgmt-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();

        for (SysJob job : jobMapper.selectList(null)) {
            if (isEnabled(job)) {
                try {
                    schedule(job);
                } catch (Exception e) {
                    log.error("任务 {} 启动失败", job.getName(), e);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        futures.values().forEach(future -> future.cancel(true));
        futures.clear();
        scheduler.shutdown();
    }

    public Map<String, JobHandler> handlers() {
        return handlerMap;
    }

    public void reschedule(SysJob job) {
        cancel(job.getId());
        if (isEnabled(job)) {
            schedule(job);
        }
    }

    public void cancel(Long jobId) {
        ScheduledFuture<?> future = futures.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 手动执行：同步执行并返回结果，方便页面立即看到执行情况。
     */
    public String runNow(SysJob job) {
        JobHandler handler = handlerMap.get(job.getHandlerKey());
        if (handler == null) {
            throw new BizException(I18n.get("job.handlerMissing", job.getHandlerKey()));
        }
        long start = System.currentTimeMillis();
        try {
            String result = normalize(handler.execute());
            saveLastRun(job.getId(), result);
            jobLogService.record(job, JobLogService.TRIGGER_MANUAL, true,
                    System.currentTimeMillis() - start, result);
            return result;
        } catch (Exception e) {
            String result = "执行失败：" + e.getMessage();
            saveLastRun(job.getId(), result);
            jobLogService.record(job, JobLogService.TRIGGER_MANUAL, false,
                    System.currentTimeMillis() - start, result);
            log.error("任务 {} 执行失败", job.getName(), e);
            throw new BizException(result);
        }
    }

    private void schedule(SysJob job) {
        CronTrigger trigger = new CronTrigger(job.getCronExpression());
        ScheduledFuture<?> future = scheduler.schedule(() -> runScheduled(job.getId()), trigger);
        if (future != null) {
            futures.put(job.getId(), future);
        }
    }

    private void runScheduled(Long jobId) {
        // 重新读取，保证状态、cron 被停用或修改后立即生效
        SysJob job = jobMapper.selectById(jobId);
        if (job == null || !isEnabled(job)) {
            cancel(jobId);
            return;
        }
        JobHandler handler = handlerMap.get(job.getHandlerKey());
        if (handler == null) {
            log.warn("任务 {} 的处理器 {} 不存在，跳过执行", job.getName(), job.getHandlerKey());
            return;
        }
        long start = System.currentTimeMillis();
        try {
            String result = normalize(handler.execute());
            long duration = System.currentTimeMillis() - start;
            saveLastRun(jobId, result);
            jobLogService.record(job, JobLogService.TRIGGER_SCHEDULED, true, duration, result);
            log.info("任务 {} 执行完成：{}", job.getName(), result);
        } catch (Exception e) {
            String result = "执行失败：" + e.getMessage();
            saveLastRun(jobId, result);
            jobLogService.record(job, JobLogService.TRIGGER_SCHEDULED, false,
                    System.currentTimeMillis() - start, result);
            log.error("任务 {} 执行失败", job.getName(), e);
            // 定时执行无人值守，失败时通知拥有任务查看权限的用户
            messageService.sendToPermission("job:view", MessageService.TYPE_JOB,
                    I18n.get("message.jobFailTitle"),
                    I18n.get("message.jobFailContent", job.getName(),
                            e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()),
                    "/jobs");
        }
    }

    private void saveLastRun(Long jobId, String result) {
        SysJob update = new SysJob();
        update.setId(jobId);
        update.setLastRunTime(LocalDateTime.now());
        update.setLastResult(result.length() > 500 ? result.substring(0, 500) : result);
        jobMapper.updateById(update);
    }

    private String normalize(String result) {
        return StringUtils.hasText(result) ? result : "执行成功";
    }

    private boolean isEnabled(SysJob job) {
        return job.getStatus() != null && job.getStatus() == 1;
    }
}
