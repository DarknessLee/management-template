package com.mgmt.module.monitor;

import com.mgmt.module.monitor.dto.MonitorInfo;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

/**
 * 基于 JDK 自带的管理接口采集运行信息，不引入额外依赖。
 */
@Service
public class MonitorService {

    public MonitorInfo info() {
        MonitorInfo info = new MonitorInfo();

        Runtime runtime = Runtime.getRuntime();
        info.setHeapUsed(runtime.totalMemory() - runtime.freeMemory());
        info.setHeapMax(runtime.maxMemory());
        info.setAvailableProcessors(runtime.availableProcessors());
        info.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());
        info.setThreadCount(ManagementFactory.getThreadMXBean().getThreadCount());

        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        info.setSystemLoadAverage(osBean.getSystemLoadAverage());
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double load = sunOsBean.getCpuLoad();
            // 首次采样或平台不支持时可能返回负数
            info.setCpuLoad(load < 0 ? -1 : Math.round(load * 10000) / 100.0);
        } else {
            info.setCpuLoad(-1);
        }

        File disk = new File(".").getAbsoluteFile();
        info.setDiskTotal(disk.getTotalSpace());
        info.setDiskFree(disk.getUsableSpace());

        info.setJavaVersion(System.getProperty("java.version"));
        info.setOsName(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        try {
            info.setHostName(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            info.setHostName("-");
        }
        return info;
    }
}
