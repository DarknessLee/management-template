package com.mgmt.module.menu;

import com.mgmt.common.Result;
import com.mgmt.common.UserContext;
import com.mgmt.module.menu.dto.MenuRequest;
import com.mgmt.module.menu.entity.SysMenu;
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
@RequestMapping("/menus")
@Tag(name = "菜单管理", description = "菜单与路由配置的维护")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 当前用户菜单只要求登录，返回内容已按当前用户权限过滤。
     */
    @GetMapping("/current")
    public Result<List<SysMenu>> current() {
        return Result.ok(menuService.currentUserMenus(UserContext.getUserId()));
    }

    @GetMapping
    @RequirePermission("menu:view")
    public Result<List<SysMenu>> list() {
        return Result.ok(menuService.listAll());
    }

    @PostMapping
    @RequirePermission("menu:edit")
    @OperationLog(module = "菜单管理", action = "新增菜单")
    public Result<SysMenu> create(@RequestBody MenuRequest request) {
        return Result.ok(menuService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("menu:edit")
    @OperationLog(module = "菜单管理", action = "编辑菜单")
    public Result<SysMenu> update(@PathVariable Long id, @RequestBody MenuRequest request) {
        return Result.ok(menuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("menu:edit")
    @OperationLog(module = "菜单管理", action = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
