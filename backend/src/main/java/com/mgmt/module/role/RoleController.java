package com.mgmt.module.role;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.role.dto.RoleRequest;
import com.mgmt.module.role.dto.RoleVO;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/roles")
@Tag(name = "角色管理", description = "角色的查询、维护与授权")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色数量少（个位数），不做分页，但保持与用户列表一致的 { list, total } 结构，
     * 前端处理列表响应时不需要写两套逻辑。
     */
    @Operation(summary = "查询角色列表")
    @GetMapping
    @RequirePermission("roles:view")
    public Result<PageResult<RoleVO>> list() {
        List<RoleVO> list = roleService.list();
        return Result.ok(PageResult.of(1, list.size(), list, list.size()));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @RequirePermission("roles:add")
    @OperationLog(module = "角色管理", action = "新增角色")
    public Result<RoleVO> create(@RequestBody RoleRequest request) {
        return Result.ok(roleService.create(request));
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    @RequirePermission("roles:edit")
    @OperationLog(module = "角色管理", action = "编辑角色")
    public Result<RoleVO> update(@PathVariable Long id, @RequestBody RoleRequest request) {
        return Result.ok(roleService.update(id, request));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @RequirePermission("roles:delete")
    @OperationLog(module = "角色管理", action = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
