package com.mgmt.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mgmt.module.auth.entity.LoginLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("SELECT COUNT(*) FROM login_log WHERE DATE(login_time) = CURDATE()")
    long countTodayLogin();

    /**
     * 近 7 天已正常退出会话的平均在线分钟数，没有数据时返回 null。
     */
    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MINUTE, login_time, logout_time))
            FROM login_log
            WHERE logout_time IS NOT NULL AND login_time >= #{start}
            """)
    Double avgLoginMinutes(@Param("start") LocalDateTime start);
}
