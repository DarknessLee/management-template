package com.mgmt.module.dashboard.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DashboardVO {

    private long totalUsers;
    private long totalRoles;
    private long todayLogin;

    /** 形如 "2小时30分" */
    private String avgLoginTime;

    /** 近 7 天每日新增用户数，按时间正序 */
    private List<Long> userTrend = new ArrayList<>();

    private List<RoleCount> roleDistribution = new ArrayList<>();

    @Data
    public static class RoleCount {
        private String name;
        private long value;

        public RoleCount(String name, long value) {
            this.name = name;
            this.value = value;
        }
    }
}
