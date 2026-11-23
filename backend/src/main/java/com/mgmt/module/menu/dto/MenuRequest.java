package com.mgmt.module.menu.dto;

import lombok.Data;

@Data
public class MenuRequest {

    /** 上级菜单ID，0 或空表示顶级 */
    private Long parentId;

    private String title;

    /** 英文名称，可选 */
    private String titleEn;

    private String path;

    private String component;

    private String icon;

    /** 为空表示登录即可见 */
    private String permission;

    private Integer sort;

    /** 1 显示 0 隐藏 */
    private Integer visible;
}
