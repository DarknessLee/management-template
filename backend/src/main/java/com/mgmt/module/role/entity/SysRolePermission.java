package com.mgmt.module.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色-权限关联表，联合主键，没有自增 id。
 * 只做批量插入和按角色删除，用不到 updateById / deleteById。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_permission")
public class SysRolePermission {

    private Long roleId;

    private String permission;
}
