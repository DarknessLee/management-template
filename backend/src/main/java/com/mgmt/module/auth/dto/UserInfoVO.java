package com.mgmt.module.auth.dto;

import lombok.Data;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String realName;

    /** 角色 code，前端侧边栏/按钮权限都基于它判断 */
    private String role;

    private String avatar;
}
