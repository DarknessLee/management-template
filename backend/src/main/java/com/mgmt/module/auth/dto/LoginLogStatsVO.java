package com.mgmt.module.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志今日统计：成功/失败次数、失败率与失败原因 Top3。
 */
@Data
public class LoginLogStatsVO {

    private long todayTotal;
    private long todaySuccess;
    private long todayFailed;
    /** 失败率百分比，保留 1 位小数 */
    private double failureRate;

    private List<FailReason> topFailReasons = new ArrayList<>();

    @Data
    public static class FailReason {
        private String reason;
        private long count;

        public FailReason(String reason, long count) {
            this.reason = reason;
            this.count = count;
        }
    }
}
