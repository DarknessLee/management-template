package com.mgmt.module.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DashboardVO {

    private long totalUsers;
    private long enabledUsers;
    private long disabledUsers;
    private long totalRoles;
    private long totalDepts;
    private long totalFiles;
    private long publishedNotices;
    private long unreadNotices;
    private long totalJobs;
    private long enabledJobs;
    private long todayLogin;
    private long todayOperations;
    private long failedOperations;

    /** 形如 "2小时30分" */
    private String avgLoginTime;

    /** 近 7 天登录次数，按时间正序 */
    private List<TrendPoint> loginTrend = new ArrayList<>();

    /** 近 7 天操作次数按模块分布 */
    private List<ModuleCount> operationModules = new ArrayList<>();

    private List<RecentOperation> recentOperations = new ArrayList<>();

    private List<NoticeBrief> latestNotices = new ArrayList<>();

    @Data
    public static class TrendPoint {
        private String label;
        private long value;

        public TrendPoint(String label, long value) {
            this.label = label;
            this.value = value;
        }
    }

    @Data
    public static class ModuleCount {
        private String name;
        private long value;

        public ModuleCount(String name, long value) {
            this.name = name;
            this.value = value;
        }
    }

    @Data
    public static class RecentOperation {
        private String username;
        private String module;
        private String action;
        private Integer success;
        private Long durationMs;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
    }

    @Data
    public static class NoticeBrief {
        private Long id;
        private String title;
        private String content;
        private Integer pinned;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime publishTime;
    }
}
