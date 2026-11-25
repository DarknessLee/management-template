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

    /** Redis 是否可连接 */
    private boolean redisAvailable;

    private String redisVersion;

    /** Redis 已用内存（字节） */
    private long redisUsedMemory;

    /** Redis 最大内存限制（字节），0 表示未限制 */
    private long redisMaxMemory;

    /** 当前库键总数 */
    private long redisKeys;

    /** 客户端连接数 */
    private long redisClients;

    /** 验证码缓存键数量 */
    private long captchaKeys;

    /** 登录失败计数键数量（清理后可解除临时锁定） */
    private long loginFailKeys;

    /** 数据库连接池是否可用 */
    private boolean dbPoolAvailable;

    private int dbPoolActive;

    private int dbPoolIdle;

    private int dbPoolTotal;

    private int dbPoolMax;

    /** 等待获取连接的线程数 */
    private int dbPoolWaiting;
}
