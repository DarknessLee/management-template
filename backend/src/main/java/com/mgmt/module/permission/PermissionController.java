package com.mgmt.module.permission;

import com.mgmt.common.Result;
import com.mgmt.module.permission.dto.PermissionRequest;
import com.mgmt.module.permission.entity.SysPermission;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@Tag(name = "权限点管理", description = "权限点的定义与维护")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionDefinitionService permissionDefinitionService;

    /**
     * 权限点数量少，直接返回全量列表。
     * 角色管理页授权时也要用这份数据，所以查看权限沿用 roles:view。
     */
    @GetMapping
    @RequirePermission("roles:view")
    public Result<List<SysPermission>> list() {
        return Result.ok(permissionDefinitionService.list());
    }

    @PostMapping
    @RequirePermission("roles:edit")
    @OperationLog(module = "权限管理", action = "新增权限点")
    public Result<SysPermission> create(@RequestBody PermissionRequest request) {
        return Result.ok(permissionDefinitionService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("roles:edit")
    @OperationLog(module = "权限管理", action = "编辑权限点")
    public Result<SysPermission> update(@PathVariable Long id, @RequestBody PermissionRequest request) {
        return Result.ok(permissionDefinitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("roles:edit")
    @OperationLog(module = "权限管理", action = "删除权限点")
    public Result<Void> delete(@PathVariable Long id) {
        permissionDefinitionService.delete(id);
        return Result.ok();
    }
}
