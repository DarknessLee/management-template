package com.mgmt.module.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.BizException;
import com.mgmt.module.menu.dto.MenuRequest;
import com.mgmt.module.menu.entity.SysMenu;
import com.mgmt.module.menu.mapper.MenuMapper;
import com.mgmt.module.permission.entity.SysPermission;
import com.mgmt.module.permission.mapper.SysPermissionMapper;
import com.mgmt.module.role.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜单查询与维护：侧边栏和路由都由菜单数据驱动，新增菜单后刷新页面生效。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final PermissionService permissionService;
    private final SysPermissionMapper permissionDefinitionMapper;

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

    public List<SysMenu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
    }

    public SysMenu create(MenuRequest request) {
        validate(request);
        ensurePathAvailable(request.getPath().trim(), null);

        SysMenu menu = new SysMenu();
        apply(menu, request);
        menuMapper.insert(menu);
        return menu;
    }

    public SysMenu update(Long id, MenuRequest request) {
        SysMenu menu = requireMenu(id);
        validate(request);
        ensurePathAvailable(request.getPath().trim(), id);

        apply(menu, request);
        menuMapper.updateById(menu);
        return menu;
    }

    public void delete(Long id) {
        requireMenu(id);
        menuMapper.deleteById(id);
    }

    private void apply(SysMenu menu, MenuRequest request) {
        menu.setTitle(request.getTitle().trim());
        menu.setPath(request.getPath().trim());
        menu.setComponent(request.getComponent().trim());
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setPermission(trimToNull(request.getPermission()));
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setVisible(request.getVisible() == null ? 1 : request.getVisible());
    }

    private void validate(MenuRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BizException("请输入菜单名称");
        }
        if (!StringUtils.hasText(request.getPath())) {
            throw new BizException("请输入路由路径");
        }
        if (!request.getPath().trim().matches("^/[a-zA-Z0-9\\-_/]*$")) {
            throw new BizException("路由路径应以 / 开头，只能包含字母、数字、- 和 _");
        }
        if (!StringUtils.hasText(request.getComponent())) {
            throw new BizException("请输入组件路径");
        }
        if (!request.getComponent().trim().matches("^views/[a-zA-Z0-9\\-_/]+\\.vue$")) {
            throw new BizException("组件路径格式应为 views/模块/index.vue");
        }
        if (StringUtils.hasText(request.getPermission())) {
            long exists = permissionDefinitionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getCode, request.getPermission().trim()));
            if (exists == 0) {
                throw new BizException("权限编码不存在，请先在权限点管理中创建");
            }
        }
    }

    private void ensurePathAvailable(String path, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, path);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new BizException("路由路径已存在");
        }
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(404, "菜单不存在");
        }
        return menu;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
