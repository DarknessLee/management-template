package com.mgmt.module.menu.dto;

import lombok.Data;

@Data
public class MenuRequest {

    private String title;

    private String path;

    private String component;

    private String icon;

    /** 为空表示登录即可见 */
    private String permission;

    private Integer sort;

    /** 1 显示 0 隐藏 */
    private Integer visible;
}
