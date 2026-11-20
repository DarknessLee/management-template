package com.mgmt.module.menu;

import com.mgmt.common.Result;
import com.mgmt.common.UserContext;
import com.mgmt.module.menu.entity.SysMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单接口只要求登录，返回内容已按当前用户权限过滤。
 */
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/current")
    public Result<List<SysMenu>> current() {
        return Result.ok(menuService.currentUserMenus(UserContext.getUserId()));
    }
}
