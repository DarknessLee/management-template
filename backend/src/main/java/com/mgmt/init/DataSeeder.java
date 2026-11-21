package com.mgmt.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.module.dept.entity.SysDept;
import com.mgmt.module.dept.mapper.SysDeptMapper;
import com.mgmt.module.dict.entity.SysDictItem;
import com.mgmt.module.dict.entity.SysDictType;
import com.mgmt.module.dict.mapper.DictItemMapper;
import com.mgmt.module.dict.mapper.DictTypeMapper;
import com.mgmt.module.job.JobScheduler;
import com.mgmt.module.job.entity.SysJob;
import com.mgmt.module.job.mapper.SysJobMapper;
import com.mgmt.module.permission.entity.SysPermission;
import com.mgmt.module.permission.mapper.SysPermissionMapper;
import com.mgmt.module.menu.entity.SysMenu;
import com.mgmt.module.menu.mapper.MenuMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final SysDeptMapper deptMapper;
    private final SysJobMapper jobMapper;
    private final JobScheduler jobScheduler;
    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final MenuMapper menuMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SettingService settingService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 建表脚本是 CREATE TABLE IF NOT EXISTS，老库不会自动改表结构，只能在这里补
        ensureMenuStructure();
        ensureRoleDataScope();
        ensureNoticePinned();
        // 权限点目录先于角色初始化，角色授权的可选范围依赖它
        seedPermissions();
        seedDicts();
        seedMenus();
        migrateMenuGroups();
        ensureFileMenu();
        seedDepts();
        seedJobs();
        seedRoles();
        seedUsers();
        // 角色可能早已初始化过，这里幂等补权限，避免老库缺新增的权限标识
        ensurePermission("admin", "logs:view");
        ensurePermission("admin", "notice:view");
        ensurePermission("admin", "notice:edit");
        ensurePermission("admin", "dict:view");
        ensurePermission("admin", "dict:edit");
        ensurePermission("admin", "monitor:view");
        ensurePermission("admin", "online:view");
        ensurePermission("admin", "logs:clean");
        ensurePermission("admin", "dept:view");
        ensurePermission("admin", "dept:edit");
        ensurePermission("admin", "job:view");
        ensurePermission("admin", "job:edit");
        ensurePermission("admin", "menu:view");
        ensurePermission("admin", "menu:edit");
        ensurePermission("admin", "file:view");
        ensurePermission("admin", "file:edit");
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
        ensurePermissionDefinition("logs:clean", "清理日志", "操作日志");
        ensurePermissionDefinition("notice:view", "查看公告", "通知公告");
        ensurePermissionDefinition("notice:edit", "管理公告", "通知公告");
        ensurePermissionDefinition("dict:view", "查看字典", "数据字典");
        ensurePermissionDefinition("dict:edit", "管理字典", "数据字典");
        ensurePermissionDefinition("monitor:view", "查看监控", "系统监控");
        ensurePermissionDefinition("online:view", "查看在线用户", "在线用户");
        ensurePermissionDefinition("dept:view", "查看部门", "部门管理");
        ensurePermissionDefinition("dept:edit", "维护部门", "部门管理");
        ensurePermissionDefinition("job:view", "查看任务", "定时任务");
        ensurePermissionDefinition("job:edit", "维护任务", "定时任务");
        ensurePermissionDefinition("menu:view", "查看菜单", "菜单管理");
        ensurePermissionDefinition("menu:edit", "维护菜单", "菜单管理");
        ensurePermissionDefinition("file:view", "查看文件", "文件管理");
        ensurePermissionDefinition("file:edit", "管理文件", "文件管理");
    }

    /**
     * 菜单只在首次初始化时写入，之后由菜单管理页维护，避免删除后重启又被补回。
     * 仪表盘保持顶级，其余页面按业务归到目录下，侧边栏两级展示。
     */
    private void seedMenus() {
        if (menuMapper.selectCount(null) > 0) {
            return;
        }
        createMenu(0L, "/dashboard", "仪表盘", "views/dashboard/index.vue", "Odometer", null, 1);

        Long systemId = createMenu(0L, "/system", "系统管理", null, "Setting", null, 2).getId();
        createMenu(systemId, "/users", "用户管理", "views/users/index.vue", "User", "users:view", 1);
        createMenu(systemId, "/roles", "角色管理", "views/roles/index.vue", "UserFilled", "roles:view", 2);
        createMenu(systemId, "/permissions", "权限点管理", "views/permissions/index.vue", "Key", "roles:view", 3);
        createMenu(systemId, "/depts", "部门管理", "views/depts/index.vue", "OfficeBuilding", "dept:view", 4);
        createMenu(systemId, "/menus", "菜单管理", "views/menus/index.vue", "Menu", "menu:view", 5);
        createMenu(systemId, "/dicts", "数据字典", "views/dicts/index.vue", "Collection", "dict:view", 6);
        createMenu(systemId, "/settings", "系统设置", "views/settings/index.vue", "Operation", "settings:view", 7);
        createMenu(systemId, "/jobs", "定时任务", "views/jobs/index.vue", "Timer", "job:view", 8);
        createMenu(systemId, "/files", "文件管理", "views/files/index.vue", "FolderOpened", "file:view", 9);

        Long auditId = createMenu(0L, "/audit", "日志审计", null, "Tickets", null, 3).getId();
        createMenu(auditId, "/logs", "操作日志", "views/logs/index.vue", "Document", "logs:view", 1);
        createMenu(auditId, "/login-logs", "登录日志", "views/loginLogs/index.vue", "Clock", "logs:view", 2);

        Long opsId = createMenu(0L, "/ops", "运行监控", null, "DataLine", null, 4).getId();
        createMenu(opsId, "/monitor", "系统监控", "views/monitor/index.vue", "Monitor", "monitor:view", 1);
        createMenu(opsId, "/online-users", "在线用户", "views/onlineUsers/index.vue", "Connection", "online:view", 2);

        Long contentId = createMenu(0L, "/content", "内容管理", null, "ChatDotRound", null, 5).getId();
        createMenu(contentId, "/notices", "通知公告", "views/notices/index.vue", "Message", "notice:view", 1);

        log.info("初始化菜单完成");
    }

    /**
     * 老库菜单是平铺的，这里一次性补上分组目录并把页面菜单挂进去。
     * 只要分组目录已存在就认为迁移过，避免覆盖后续在管理页做的调整。
     */
    private void migrateMenuGroups() {
        long exists = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getPath, "/system", "/audit", "/ops", "/content"));
        if (exists > 0) {
            return;
        }
        Long systemId = createMenu(0L, "/system", "系统管理", null, "Setting", null, 2).getId();
        Long auditId = createMenu(0L, "/audit", "日志审计", null, "Tickets", null, 3).getId();
        Long opsId = createMenu(0L, "/ops", "运行监控", null, "DataLine", null, 4).getId();
        Long contentId = createMenu(0L, "/content", "内容管理", null, "ChatDotRound", null, 5).getId();

        moveToGroup("/users", systemId, 1);
        moveToGroup("/roles", systemId, 2);
        moveToGroup("/permissions", systemId, 3);
        moveToGroup("/depts", systemId, 4);
        moveToGroup("/menus", systemId, 5);
        moveToGroup("/dicts", systemId, 6);
        moveToGroup("/settings", systemId, 7);
        moveToGroup("/jobs", systemId, 8);
        moveToGroup("/files", systemId, 9);

        moveToGroup("/logs", auditId, 1);
        moveToGroup("/login-logs", auditId, 2);

        moveToGroup("/monitor", opsId, 1);
        moveToGroup("/online-users", opsId, 2);

        moveToGroup("/notices", contentId, 1);
        log.info("菜单分组迁移完成");
    }

    private void moveToGroup(String path, Long parentId, int sort) {
        SysMenu menu = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, path));
        if (menu == null) {
            return;
        }
        menu.setParentId(parentId);
        menu.setSort(sort);
        menuMapper.updateById(menu);
    }

    /**
     * 文件管理是后加的功能，老库菜单表已初始化过，seedMenus 不会再次执行，
     * 这里按路径幂等补一条，保证升级后菜单直接可见。
     */
    private void ensureFileMenu() {
        long exists = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, "/files"));
        if (exists > 0) {
            return;
        }
        SysMenu group = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, "/system"));
        Long parentId = group == null ? 0L : group.getId();
        createMenu(parentId, "/files", "文件管理", "views/files/index.vue", "FolderOpened", "file:view", 9);
        log.info("补充文件管理菜单");
    }

    private void ensureMenuStructure() {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_menu' AND column_name = 'parent_id'",
                Integer.class);
        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE sys_menu ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 "
                    + "COMMENT '上级菜单ID，0为顶级' AFTER id, ADD KEY idx_parent_id (parent_id)");
            log.info("菜单表补充 parent_id 字段");
        }
        // 目录没有组件路径，老库的 component 是 NOT NULL，需要放开约束
        Integer notNullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_menu' "
                        + "AND column_name = 'component' AND IS_NULLABLE = 'NO'",
                Integer.class);
        if (notNullCount != null && notNullCount > 0) {
            jdbcTemplate.execute("ALTER TABLE sys_menu MODIFY COLUMN component VARCHAR(100) DEFAULT NULL "
                    + "COMMENT '前端视图路径，如 views/users/index.vue；目录为空'");
            log.info("菜单表放开 component 非空限制");
        }
    }

    private void ensureRoleDataScope() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_role' AND column_name = 'data_scope'",
                Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN data_scope TINYINT NOT NULL DEFAULT 1 "
                + "COMMENT '数据范围：1全部 2本部门 3本部门及以下 4仅本人' AFTER description");
        log.info("角色表补充 data_scope 字段");
    }

    private void ensureNoticePinned() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_notice' AND column_name = 'pinned'",
                Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE sys_notice ADD COLUMN pinned TINYINT NOT NULL DEFAULT 0 "
                + "COMMENT '是否置顶：1是 0否' AFTER status");
        log.info("公告表补充 pinned 字段");
    }

    private SysMenu createMenu(Long parentId, String path, String title, String component, String icon,
                               String permission, int sort) {
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setPath(path);
        menu.setTitle(title);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setPermission(permission);
        menu.setSort(sort);
        menu.setVisible(1);
        menuMapper.insert(menu);
        return menu;
    }

    /**
     * 业务下拉框使用的字典：只在编码不存在时插入，避免覆盖管理员后续维护的内容。
     */
    private void seedDicts() {
        ensureDictType("user_status", "用户状态",
                new String[]{"启用", "1"}, new String[]{"禁用", "0"});
        ensureDictType("notice_status", "公告状态",
                new String[]{"已发布", "1"}, new String[]{"已下线", "0"});
        ensureDictType("job_status", "任务状态",
                new String[]{"启用", "1"}, new String[]{"停用", "0"});
    }

    private void ensureDictType(String code, String name, String[]... items) {
        long exists = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getCode, code));
        if (exists > 0) {
            return;
        }
        SysDictType type = new SysDictType();
        type.setCode(code);
        type.setName(name);
        dictTypeMapper.insert(type);

        int sort = 1;
        for (String[] item : items) {
            SysDictItem dictItem = new SysDictItem();
            dictItem.setTypeId(type.getId());
            dictItem.setLabel(item[0]);
            dictItem.setValue(item[1]);
            dictItem.setSort(sort++);
            dictItemMapper.insert(dictItem);
        }
        log.info("初始化字典 {}", code);
    }

    private void seedJobs() {
        if (jobMapper.selectCount(null) > 0) {
            return;
        }
        createJob("操作日志清理", "log-clean", "0 0 3 * * ?", 1,
                "每天 03:00 清理超过保留天数的操作日志");
        createJob("登录日志清理", "login-log-clean", "0 30 3 * * ?", 1,
                "每天 03:30 清理超过保留天数的登录日志");
        log.info("初始化定时任务完成");
    }

    private void createJob(String name, String handlerKey, String cron, int status, String remark) {
        SysJob job = new SysJob();
        job.setName(name);
        job.setHandlerKey(handlerKey);
        job.setCronExpression(cron);
        job.setStatus(status);
        job.setRemark(remark);
        jobMapper.insert(job);
        // 调度器在种子数据之前启动，新建的任务需要手动注册
        jobScheduler.reschedule(job);
    }

    private void seedDepts() {
        if (deptMapper.selectCount(null) > 0) {
            return;
        }
        SysDept tech = createDept(0L, "技术部", "张伟", "13800000001", 1);
        createDept(tech.getId(), "研发组", "李强", "13800000002", 1);
        createDept(tech.getId(), "测试组", "王敏", "13800000003", 2);
        log.info("初始化部门完成");
    }

    private SysDept createDept(Long parentId, String name, String leader, String phone, int sort) {
        SysDept dept = new SysDept();
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setLeader(leader);
        dept.setPhone(phone);
        dept.setSort(sort);
        deptMapper.insert(dept);
        return dept;
    }

    private void seedRoles() {
        if (roleMapper.selectCount(null) > 0) {
            return;
        }

        Map<String, List<String>> rolePermissions = new LinkedHashMap<>();
        rolePermissions.put("admin|管理员|系统管理员，拥有所有权限", List.of(
                "users:view", "users:add", "users:edit", "users:delete",
                "roles:view", "roles:add", "roles:edit", "roles:delete",
                "settings:view", "settings:edit", "logs:view", "logs:clean",
                "notice:view", "notice:edit",
                "dict:view", "dict:edit",
                "monitor:view", "online:view",
                "dept:view", "dept:edit",
                "job:view", "job:edit",
                "menu:view", "menu:edit",
                "file:view", "file:edit"));
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
            role.setDataScope(switch (parts[0]) {
                case "admin" -> 1;
                case "editor" -> 2;
                default -> 4;
            });
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
