package com.mgmt.module.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mgmt.module.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 近 7 天操作日志按模块聚合并限制数量，用于仪表盘分布图。
     */
    @Select("""
            SELECT module AS name, COUNT(*) AS cnt
            FROM sys_operation_log
            WHERE create_time >= #{start}
            GROUP BY module
            ORDER BY cnt DESC
            LIMIT 6
            """)
    List<Map<String, Object>> countByModule(@Param("start") LocalDateTime start);

    /**
     * 分批删除过期日志。带上 LIMIT 后，InnoDB 命中 create_time 索引扫到足够行数就停止，
     * 不会一次锁住整张表；没有 ORDER BY 也能工作，排序反而会让索引利用变差。
     */
    @Delete("DELETE FROM sys_operation_log WHERE create_time < #{before} LIMIT #{batchSize}")
    int deleteBefore(@Param("before") LocalDateTime before, @Param("batchSize") int batchSize);
}
