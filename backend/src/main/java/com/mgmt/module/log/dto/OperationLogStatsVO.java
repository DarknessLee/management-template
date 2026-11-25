package com.mgmt.module.log.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志今日统计：操作总数、失败率与耗时最长的模块 Top3。
 */
@Data
public class OperationLogStatsVO {

    private long todayTotal;
    private long todaySuccess;
    private long todayFailed;
    /** 失败率百分比，保留 1 位小数 */
    private double failureRate;

    private List<SlowModule> slowModules = new ArrayList<>();

    @Data
    public static class SlowModule {
        private String module;
        /** 平均耗时（毫秒） */
        private double avgDurationMs;
        private long count;

        public SlowModule(String module, double avgDurationMs, long count) {
            this.module = module;
            this.avgDurationMs = avgDurationMs;
            this.count = count;
        }
    }
}
