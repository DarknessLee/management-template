package com.mgmt.module.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.UserContext;
import com.mgmt.module.dept.entity.SysDept;
import com.mgmt.module.dept.mapper.SysDeptMapper;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据权限：按当前登录用户所属角色的数据范围，决定能看到哪些用户数据。
 * 数据范围取值见 sys_role.data_scope：1全部 2本部门 3本部门及以下 4仅本人。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_DEPT = 2;
    public static final int SCOPE_DEPT_AND_CHILD = 3;
    public static final int SCOPE_SELF = 4;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public DataScope current() {
        Long userId = UserContext.getUserId();
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return new DataScope(SCOPE_SELF, null, 0L, List.of());
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        int scope = role == null || role.getDataScope() == null ? SCOPE_SELF : role.getDataScope();
        Long deptId = user.getDeptId() == null ? 0L : user.getDeptId();
        List<Long> deptIds = switch (scope) {
            case SCOPE_ALL -> List.of();
            case SCOPE_DEPT -> List.of(deptId);
            case SCOPE_DEPT_AND_CHILD -> deptAndChildren(deptId);
            default -> List.of();
        };
        return new DataScope(scope, user.getId(), deptId, deptIds);
    }

    /**
     * 当前用户可见的用户ID；返回 null 表示不限制（全部数据），空集合表示没有任何可见用户。
     * 日志按 user_id 过滤时使用。
     */
    public List<Long> visibleUserIds() {
        DataScope scope = current();
        if (scope.all()) {
            return null;
        }
        if (scope.selfOnly()) {
            return scope.userId() == null ? List.of() : List.of(scope.userId());
        }
        if (scope.deptIds().isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .in(SysUser::getDeptId, scope.deptIds()))
                .stream()
                .map(SysUser::getId)
                .toList();
    }

    private List<Long> deptAndChildren(Long deptId) {
        if (deptId == null || deptId == 0L) {
            // 未分配部门的用户，"本部门及以下"只代表无部门这一档，不能把顶级部门全捞进来
            return List.of(0L);
        }
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (SysDept dept : deptMapper.selectList(null)) {
            long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(dept.getId());
        }
        List<Long> result = new ArrayList<>();
        collectDept(deptId, childrenMap, result, 0);
        return result;
    }

    private void collectDept(Long deptId, Map<Long, List<Long>> childrenMap, List<Long> result, int depth) {
        if (depth > 32) {
            return;
        }
        result.add(deptId);
        for (Long childId : childrenMap.getOrDefault(deptId, List.of())) {
            collectDept(childId, childrenMap, result, depth + 1);
        }
    }

    public record DataScope(int scope, Long userId, Long deptId, List<Long> deptIds) {

        public boolean all() {
            return scope == SCOPE_ALL;
        }

        public boolean selfOnly() {
            return scope == SCOPE_SELF;
        }

        public boolean deptLimited() {
            return scope == SCOPE_DEPT || scope == SCOPE_DEPT_AND_CHILD;
        }

        public boolean canSeeUser(Long targetUserId, Long targetDeptId) {
            if (all()) {
                return true;
            }
            if (selfOnly()) {
                return Objects.equals(userId, targetUserId);
            }
            long dept = targetDeptId == null ? 0L : targetDeptId;
            return deptIds.contains(dept);
        }
    }
}
