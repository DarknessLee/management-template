package com.mgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 日志专用线程池：
     * - 用固定大小的池而不是默认的 SimpleAsyncTaskExecutor，避免高并发时无限创建线程；
     * - 队列满时使用 CallerRunsPolicy，由请求线程自己写库——宁可慢一点，也不能丢审计日志。
     */
    @Bean("operationLogExecutor")
    public ThreadPoolTaskExecutor operationLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("op-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
