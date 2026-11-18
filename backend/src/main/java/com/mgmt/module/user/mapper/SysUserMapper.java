package com.mgmt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mgmt.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 近 N 天每日新增用户数（仪表盘趋势图）。
     * 只返回有数据的日期，缺失日期在 Service 里补 0。
     */
    @Select("""
            SELECT DATE(create_time) AS day, COUNT(*) AS cnt
            FROM sys_user
            WHERE deleted = 0 AND create_time >= #{start}
            GROUP BY DATE(create_time)
            """)
    List<Map<String, Object>> countNewUsersByDay(@Param("start") LocalDateTime start);

    /**
     * 各角色下的用户数量（仪表盘饼图）。
     * 用 LEFT JOIN 保证没有用户的角色也能出现在分布里。
     */
    @Select("""
            SELECT r.name AS name, COUNT(u.id) AS `value`
            FROM sys_role r
            LEFT JOIN sys_user u ON u.role_id = r.id AND u.deleted = 0
            WHERE r.deleted = 0
            GROUP BY r.id, r.name
            ORDER BY r.id
            """)
    List<Map<String, Object>> countUsersByRole();
}
