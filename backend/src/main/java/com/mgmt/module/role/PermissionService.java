package com.mgmt.module.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.module.role.entity.SysRolePermission;
import com.mgmt.module.role.mapper.SysRolePermissionMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限查询：用户 → 角色 → 权限。
 * 目前每次校验都查库（两条 SQL），量小可接受；
 * 后续可以在角色变更时清 Redis 缓存，避免每次请求都回源。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserMapper userMapper;
    private final SysRolePermissionMapper permissionMapper;

    public List<String> permissionsOf(Long userId) {
        if (userId == null) {
            return List.of();
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return List.of();
        }
        return permissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, user.getRoleId()))
                .stream()
                .map(SysRolePermission::getPermission)
                .toList();
    }

    public boolean hasPermission(Long userId, String permission) {
        return permissionsOf(userId).contains(permission);
    }
}
