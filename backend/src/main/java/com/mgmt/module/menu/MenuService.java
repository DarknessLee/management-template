package com.mgmt.module.menu;

import com.mgmt.common.I18n;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单查询与维护：侧边栏和路由都由菜单数据驱动，新增菜单后刷新页面生效。
 * 菜单最多两级：component 为空的记录是目录，只用于分组；有组件路径的是可访问页面。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final PermissionService permissionService;
    private final SysPermissionMapper permissionDefinitionMapper;

    public List<SysMenu> currentUserMenus(Long userId) {
        List<String> permissions = permissionService.permissionsOf(userId);
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
        return toTree(menus, permissions);
    }

    public List<SysMenu> listAll() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
        return toTree(menus, null);
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

        Long parentId = normalizeParentId(request.getParentId());
        if (parentId.equals(id)) {
            throw new BizException(I18n.get("menu.parentSelf"));
        }
        long childCount = childCountOf(id);
        if (childCount > 0 && !parentId.equals(0L)) {
            throw new BizException(I18n.get("menu.hasChildrenParent"));
        }
        if (childCount > 0 && StringUtils.hasText(request.getComponent())) {
            throw new BizException(I18n.get("menu.hasChildrenComponent"));
        }

        apply(menu, request);
        menuMapper.updateById(menu);
        return menu;
    }

    public void delete(Long id) {
        requireMenu(id);
        if (childCountOf(id) > 0) {
            throw new BizException(I18n.get("menu.deleteChildrenFirst"));
        }
        menuMapper.deleteById(id);
    }

    private void apply(SysMenu menu, MenuRequest request) {
        menu.setParentId(normalizeParentId(request.getParentId()));
        menu.setTitle(request.getTitle().trim());
        menu.setTitleEn(trimToNull(request.getTitleEn()));
        menu.setPath(request.getPath().trim());
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setPermission(trimToNull(request.getPermission()));
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setVisible(request.getVisible() == null ? 1 : request.getVisible());
    }

    private void validate(MenuRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BizException(I18n.get("menu.nameRequired"));
        }
        if (!StringUtils.hasText(request.getPath())) {
            throw new BizException(I18n.get("menu.pathRequired"));
        }
        if (!request.getPath().trim().matches("^/[a-zA-Z0-9\\-_/]*$")) {
            throw new BizException(I18n.get("menu.pathFormat"));
        }
        String component = trimToNull(request.getComponent());
        if (component != null && !component.matches("^views/[a-zA-Z0-9\\-_/]+\\.vue$")) {
            throw new BizException(I18n.get("menu.componentFormat"));
        }
        Long parentId = normalizeParentId(request.getParentId());
        if (!parentId.equals(0L)) {
            SysMenu parent = menuMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(I18n.get("menu.parentNotFound"));
            }
            if (StringUtils.hasText(parent.getComponent())) {
                throw new BizException(I18n.get("menu.parentMustBeDirectory"));
            }
            if (!normalizeParentId(parent.getParentId()).equals(0L)) {
                throw new BizException(I18n.get("menu.maxTwoLevels"));
            }
            if (component == null) {
                throw new BizException(I18n.get("menu.childComponentRequired"));
            }
        }
        if (StringUtils.hasText(request.getPermission())) {
            long exists = permissionDefinitionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getCode, request.getPermission().trim()));
            if (exists == 0) {
                throw new BizException(I18n.get("menu.permissionNotFound"));
            }
        }
    }

    /**
     * 平铺记录按 parentId 组装成树。目录的权限过滤交给 prune 统一处理。
     */
    private List<SysMenu> toTree(List<SysMenu> menus, List<String> permissions) {
        Map<Long, SysMenu> byId = new LinkedHashMap<>();
        menus.forEach(menu -> {
            menu.setChildren(new ArrayList<>());
            byId.put(menu.getId(), menu);
        });

        List<SysMenu> roots = new ArrayList<>();
        menus.forEach(menu -> {
            Long parentId = normalizeParentId(menu.getParentId());
            SysMenu parent = parentId.equals(0L) ? null : byId.get(parentId);
            // 上级被隐藏时子菜单仍按顶级展示，避免菜单凭空消失
            if (parent == null) {
                roots.add(menu);
            } else {
                parent.getChildren().add(menu);
            }
        });

        if (permissions != null) {
            prune(roots, permissions);
        }
        return roots;
    }

    /**
     * 递归过滤无权限的菜单；目录下的子菜单全部被过滤后，目录本身也不再展示。
     */
    private void prune(List<SysMenu> nodes, List<String> permissions) {
        nodes.removeIf(menu -> {
            if (StringUtils.hasText(menu.getPermission())
                    && !permissions.contains(menu.getPermission())) {
                return true;
            }
            List<SysMenu> children = menu.getChildren();
            if (children != null && !children.isEmpty()) {
                prune(children, permissions);
            }
            boolean directory = !StringUtils.hasText(menu.getComponent());
            return directory && (children == null || children.isEmpty());
        });
    }

    private void ensurePathAvailable(String path, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, path);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new BizException(I18n.get("menu.pathExists"));
        }
    }

    private long childCountOf(Long id) {
        return menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(404, I18n.get("menu.notFound"));
        }
        return menu;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
