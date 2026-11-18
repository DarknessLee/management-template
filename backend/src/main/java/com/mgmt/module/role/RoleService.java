package com.mgmt.module.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.BizException;
import com.mgmt.module.role.dto.RoleRequest;
import com.mgmt.module.role.dto.RoleVO;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.entity.SysRolePermission;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.role.mapper.SysRolePermissionMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper permissionMapper;
    private final SysUserMapper userMapper;

    public List<RoleVO> list() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        Map<Long, List<String>> permissionMap = permissionMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysRolePermission::getRoleId,
                        Collectors.mapping(SysRolePermission::getPermission, Collectors.toList())));

        return roles.stream()
                .map(role -> toVO(role, permissionMap.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    @Transactional
    public RoleVO create(RoleRequest request) {
        requireNameAndCode(request);
        ensureCodeAvailable(request.getCode(), null);

        SysRole role = new SysRole();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        roleMapper.insert(role);

        savePermissions(role.getId(), request.getPermissions());
        return toVO(role, request.getPermissions() == null ? List.of() : request.getPermissions());
    }

    @Transactional
    public RoleVO update(Long id, RoleRequest request) {
        SysRole role = requireRole(id);
        requireNameAndCode(request);
        ensureCodeAvailable(request.getCode(), id);

        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        roleMapper.updateById(role);

        // 权限采用"先删后插"，比逐条 diff 简单，也避免脏数据
        permissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        savePermissions(id, request.getPermissions());

        return toVO(role, request.getPermissions() == null ? List.of() : request.getPermissions());
    }

    @Transactional
    public void delete(Long id) {
        requireRole(id);
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, id));
        if (userCount > 0) {
            throw new BizException("该角色下还有 " + userCount + " 个用户，请先调整用户角色");
        }
        roleMapper.deleteById(id);
        permissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
    }

    private void requireNameAndCode(RoleRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("请输入角色名称");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new BizException("请输入角色标识");
        }
        if (!request.getCode().matches("^[a-z]+$")) {
            throw new BizException("角色标识只能包含小写字母");
        }
    }

    private void ensureCodeAvailable(String code, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException("角色标识已存在");
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(404, "角色不存在");
        }
        return role;
    }

    private void savePermissions(Long roleId, List<String> permissions) {
        if (permissions == null) {
            return;
        }
        permissions.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(permission -> permissionMapper.insert(new SysRolePermission(roleId, permission)));
    }

    private RoleVO toVO(SysRole role, List<String> permissions) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setDescription(role.getDescription());
        vo.setPermissions(permissions);
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
