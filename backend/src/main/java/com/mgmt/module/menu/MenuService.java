package com.mgmt.module.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.module.menu.entity.SysMenu;
import com.mgmt.module.menu.mapper.MenuMapper;
import com.mgmt.module.role.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜单查询：按当前用户权限过滤后返回，前端据此注册路由和渲染侧边栏。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final PermissionService permissionService;

    public List<SysMenu> currentUserMenus(Long userId) {
        List<String> permissions = permissionService.permissionsOf(userId);
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getVisible, 1)
                        .orderByAsc(SysMenu::getSort)
                        .orderByAsc(SysMenu::getId))
                .stream()
                .filter(menu -> !StringUtils.hasText(menu.getPermission())
                        || permissions.contains(menu.getPermission()))
                .toList();
    }
}
