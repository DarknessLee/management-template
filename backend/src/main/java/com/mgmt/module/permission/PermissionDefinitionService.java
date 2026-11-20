package com.mgmt.module.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.BizException;
import com.mgmt.module.permission.dto.PermissionRequest;
import com.mgmt.module.permission.entity.SysPermission;
import com.mgmt.module.permission.mapper.SysPermissionMapper;
import com.mgmt.module.role.entity.SysRolePermission;
import com.mgmt.module.role.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 权限点维护：角色授权页面从这里取全部可选权限，管理员也可以增删改权限点。
 */
@Service
@RequiredArgsConstructor
public class PermissionDefinitionService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public List<SysPermission> list() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getId));
    }

    public SysPermission create(PermissionRequest request) {
        validate(request);
        ensureCodeAvailable(request.getCode());

        SysPermission permission = new SysPermission();
        permission.setCode(request.getCode().trim());
        permission.setName(request.getName().trim());
        permission.setModule(request.getModule().trim());
        permissionMapper.insert(permission);
        return permission;
    }

    public SysPermission update(Long id, PermissionRequest request) {
        SysPermission permission = requirePermission(id);
        validate(request);
        // 编码被 @RequirePermission、路由 meta、v-permission 直接引用，
        // 改了会让已有代码校验和角色授权同时失效，因此只允许改名称和分组
        if (!permission.getCode().equals(request.getCode().trim())) {
            throw new BizException("权限编码创建后不可修改");
        }

        permission.setName(request.getName().trim());
        permission.setModule(request.getModule().trim());
        permissionMapper.updateById(permission);
        return permission;
    }

    @Transactional
    public void delete(Long id) {
        SysPermission permission = requirePermission(id);
        long roleCount = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getPermission, permission.getCode()));
        if (roleCount > 0) {
            throw new BizException("该权限点已被 " + roleCount + " 个角色使用，请先在角色管理中取消授权");
        }
        permissionMapper.deleteById(id);
    }

    private void validate(PermissionRequest request) {
        if (!StringUtils.hasText(request.getCode())) {
            throw new BizException("请输入权限编码");
        }
        if (!request.getCode().trim().matches("^[a-z][a-z0-9]*:[a-z][a-z0-9]*$")) {
            throw new BizException("权限编码格式应为 模块:动作，如 users:view");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("请输入权限名称");
        }
        if (!StringUtils.hasText(request.getModule())) {
            throw new BizException("请输入所属模块");
        }
    }

    private void ensureCodeAvailable(String code) {
        long exists = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, code.trim()));
        if (exists > 0) {
            throw new BizException("权限编码已存在");
        }
    }

    private SysPermission requirePermission(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BizException(404, "权限点不存在");
        }
        return permission;
    }
}
