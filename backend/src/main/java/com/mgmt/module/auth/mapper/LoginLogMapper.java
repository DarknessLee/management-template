package com.mgmt.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mgmt.module.auth.entity.LoginLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("SELECT COUNT(*) FROM login_log WHERE DATE(login_time) = CURDATE()")
    long countTodayLogin();

    /**
     * 近 7 天每日登录次数，用于仪表盘趋势图。
     */
    @Select("""
            SELECT DATE(login_time) AS day, COUNT(*) AS cnt
            FROM login_log
            WHERE login_time >= #{start}
            GROUP BY DATE(login_time)
            ORDER BY day
            """)
    List<Map<String, Object>> countLoginByDay(@Param("start") LocalDateTime start);

    /**
     * 近 7 天已正常退出会话的平均在线分钟数，没有数据时返回 null。
     */
    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MINUTE, login_time, logout_time))
            FROM login_log
            WHERE logout_time IS NOT NULL AND login_time >= #{start}
            """)
    Double avgLoginMinutes(@Param("start") LocalDateTime start);

    /**
     * 分批删除过期登录日志，走 idx_login_time 索引，扫够 batchSize 行即停止。
     */
    @Delete("DELETE FROM login_log WHERE login_time < #{before} LIMIT #{batchSize}")
    int deleteBefore(@Param("before") LocalDateTime before, @Param("batchSize") int batchSize);
}
