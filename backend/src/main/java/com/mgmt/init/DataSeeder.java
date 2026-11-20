package com.mgmt.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.module.permission.entity.SysPermission;
import com.mgmt.module.permission.mapper.SysPermissionMapper;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.entity.SysRolePermission;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.role.mapper.SysRolePermissionMapper;
import com.mgmt.module.setting.SettingService;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 种子数据：表为空时才写入，重启不会重复插入。
 * 密码必须经过 BCrypt 加密，所以放在 Java 里而不是 SQL 脚本里。
 * 测试账号：admin / 123456、user / 123456
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper permissionMapper;
    private final SysPermissionMapper permissionDefinitionMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SettingService settingService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 权限点目录先于角色初始化，角色授权的可选范围依赖它
        seedPermissions();
        seedRoles();
        seedUsers();
        // 角色可能早已初始化过，这里幂等补权限，避免老库缺新增的权限标识
        ensurePermission("admin", "logs:view");
        settingService.initDefaultsIfEmpty();
    }

    private void seedPermissions() {
        ensurePermissionDefinition("users:view", "查看用户", "用户管理");
        ensurePermissionDefinition("users:add", "新增用户", "用户管理");
        ensurePermissionDefinition("users:edit", "编辑用户", "用户管理");
        ensurePermissionDefinition("users:delete", "删除用户", "用户管理");
        ensurePermissionDefinition("roles:view", "查看角色", "角色管理");
        ensurePermissionDefinition("roles:add", "新增角色", "角色管理");
        ensurePermissionDefinition("roles:edit", "编辑角色", "角色管理");
        ensurePermissionDefinition("roles:delete", "删除角色", "角色管理");
        ensurePermissionDefinition("settings:view", "查看设置", "系统设置");
        ensurePermissionDefinition("settings:edit", "修改设置", "系统设置");
        ensurePermissionDefinition("logs:view", "查看日志", "操作日志");
    }

    private void seedRoles() {
        if (roleMapper.selectCount(null) > 0) {
            return;
        }

        Map<String, List<String>> rolePermissions = new LinkedHashMap<>();
        rolePermissions.put("admin|管理员|系统管理员，拥有所有权限", List.of(
                "users:view", "users:add", "users:edit", "users:delete",
                "roles:view", "roles:add", "roles:edit", "roles:delete",
                "settings:view", "settings:edit", "logs:view"));
        rolePermissions.put("editor|编辑|内容编辑，拥有部分权限", List.of(
                "users:view", "users:add", "users:edit"));
        rolePermissions.put("viewer|查看者|仅拥有查看权限", List.of(
                "users:view", "roles:view"));

        rolePermissions.forEach((meta, permissions) -> {
            String[] parts = meta.split("\\|");
            SysRole role = new SysRole();
            role.setCode(parts[0]);
            role.setName(parts[1]);
            role.setDescription(parts[2]);
            roleMapper.insert(role);
            permissions.forEach(p -> permissionMapper.insert(new SysRolePermission(role.getId(), p)));
        });
        log.info("初始化角色与权限完成");
    }

    private void seedUsers() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }

        Map<String, Long> roleIds = new LinkedHashMap<>();
        roleMapper.selectList(null).forEach(role -> roleIds.put(role.getCode(), role.getId()));

        createUser("admin", "管理员", "admin@example.com", "13800138000", roleIds.get("admin"), 1);
        createUser("user1", "张三", "user1@example.com", "13800138001", roleIds.get("editor"), 1);
        createUser("user2", "李四", "user2@example.com", "13800138002", roleIds.get("viewer"), 0);
        createUser("user3", "王五", "user3@example.com", "13800138003", roleIds.get("editor"), 1);
        createUser("user4", "赵六", "user4@example.com", "13800138004", roleIds.get("viewer"), 1);
        createUser("user", "访客用户", "user@example.com", "13800138005", roleIds.get("viewer"), 1);
        log.info("初始化测试用户完成，默认密码 123456");
    }

    private void ensurePermissionDefinition(String code, String name, String module) {
        long exists = permissionDefinitionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, code));
        if (exists > 0) {
            return;
        }
        SysPermission permission = new SysPermission();
        permission.setCode(code);
        permission.setName(name);
        permission.setModule(module);
        permissionDefinitionMapper.insert(permission);
        log.info("初始化权限点 {}", code);
    }

    private void ensurePermission(String roleCode, String permission) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, roleCode));
        if (role == null) {
            return;
        }
        long exists = permissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, role.getId())
                .eq(SysRolePermission::getPermission, permission));
        if (exists == 0) {
            permissionMapper.insert(new SysRolePermission(role.getId(), permission));
            log.info("为角色 {} 补充权限 {}", roleCode, permission);
        }
    }

    private void createUser(String username, String realName, String email, String phone, Long roleId, int status) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setRealName(realName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoleId(roleId);
        user.setStatus(status);
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.insert(user);
    }
}
