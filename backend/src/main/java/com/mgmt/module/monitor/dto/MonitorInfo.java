package com.mgmt.module.monitor.dto;

import lombok.Data;

/**
 * 系统监控快照，内存与磁盘单位均为字节，CPU 为百分比。
 */
@Data
public class MonitorInfo {

    /** JVM 已用堆内存 */
    private long heapUsed;

    /** JVM 最大可用堆内存 */
    private long heapMax;

    /** 系统 CPU 使用率（0-100），无法获取时为 -1 */
    private double cpuLoad;

    /** 系统 1 分钟平均负载，Windows 下可能为 -1 */
    private double systemLoadAverage;

    /** 应用所在磁盘总容量 */
    private long diskTotal;

    /** 应用所在磁盘可用容量 */
    private long diskFree;

    /** 运行时长（毫秒） */
    private long uptime;

    /** 当前线程数 */
    private int threadCount;

    private int availableProcessors;

    private String javaVersion;

    private String osName;

    private String osArch;

    private String hostName;
}
