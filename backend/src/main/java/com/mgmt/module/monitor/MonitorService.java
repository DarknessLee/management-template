package com.mgmt.module.monitor;

import com.mgmt.module.monitor.dto.MonitorInfo;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 基于 JDK 自带的管理接口采集运行信息，不引入额外依赖。
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private static final String CAPTCHA_PREFIX = "mgmt:auth:captcha:";
    private static final String LOGIN_FAIL_PREFIX = "mgmt:auth:login-fail:";
    private static final int SCAN_BATCH = 200;

    private final StringRedisTemplate redisTemplate;
    private final DataSource dataSource;

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

        fillRedis(info);
        fillDbPool(info);
        return info;
    }

    /**
     * 清理业务缓存，不影响登录会话。
     *
     * @param type captcha / loginFail / 其他值按全部处理
     */
    public long clearCache(String type) {
        if ("captcha".equalsIgnoreCase(type)) {
            return deleteByPrefix(CAPTCHA_PREFIX);
        }
        if ("loginFail".equalsIgnoreCase(type)) {
            return deleteByPrefix(LOGIN_FAIL_PREFIX);
        }
        return deleteByPrefix(CAPTCHA_PREFIX) + deleteByPrefix(LOGIN_FAIL_PREFIX);
    }

    private void fillRedis(MonitorInfo info) {
        try {
            Properties memory = redisInfo("memory");
            Properties server = redisInfo("server");
            Properties clients = redisInfo("clients");
            Properties keyspace = redisInfo("keyspace");

            info.setRedisAvailable(true);
            info.setRedisVersion(server.getProperty("redis_version", ""));
            info.setRedisUsedMemory(parseLong(memory.getProperty("used_memory")));
            info.setRedisMaxMemory(parseLong(memory.getProperty("maxmemory")));
            info.setRedisClients(parseLong(clients.getProperty("connected_clients")));
            info.setRedisKeys(keyspaceKeys(keyspace));
            info.setCaptchaKeys(countKeys(CAPTCHA_PREFIX));
            info.setLoginFailKeys(countKeys(LOGIN_FAIL_PREFIX));
        } catch (Exception e) {
            // Redis 不可用时不影响服务器指标展示
            info.setRedisAvailable(false);
        }
    }

    private void fillDbPool(MonitorInfo info) {
        if (!(dataSource instanceof HikariDataSource hikari)) {
            return;
        }
        info.setDbPoolAvailable(true);
        info.setDbPoolMax(hikari.getMaximumPoolSize());
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        if (pool == null) {
            return;
        }
        info.setDbPoolActive(pool.getActiveConnections());
        info.setDbPoolIdle(pool.getIdleConnections());
        info.setDbPoolTotal(pool.getTotalConnections());
        info.setDbPoolWaiting(pool.getThreadsAwaitingConnection());
    }

    private Properties redisInfo(String section) {
        Properties properties = redisTemplate.execute((RedisCallback<Properties>) connection ->
                connection.serverCommands().info(section));
        return properties == null ? new Properties() : properties;
    }

    private long keyspaceKeys(Properties keyspace) {
        for (String name : keyspace.stringPropertyNames()) {
            if (!name.startsWith("db")) {
                continue;
            }
            for (String part : keyspace.getProperty(name, "").split(",")) {
                if (part.startsWith("keys=")) {
                    return parseLong(part.substring("keys=".length()));
                }
            }
        }
        return 0;
    }

    private long countKeys(String prefix) {
        long count = 0;
        try (Cursor<String> cursor = scan(prefix)) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }

    private long deleteByPrefix(String prefix) {
        long deleted = 0;
        List<String> batch = new ArrayList<>();
        try (Cursor<String> cursor = scan(prefix)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= SCAN_BATCH) {
                    deleted += deleteBatch(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                deleted += deleteBatch(batch);
            }
        }
        return deleted;
    }

    private long deleteBatch(List<String> batch) {
        Long removed = redisTemplate.delete(batch);
        return removed == null ? 0 : removed;
    }

    private Cursor<String> scan(String prefix) {
        return redisTemplate.scan(ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(SCAN_BATCH)
                .build());
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
