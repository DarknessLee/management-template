package com.mgmt.module.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String realName;

    /** 角色 code，前端侧边栏/按钮权限都基于它判断 */
    private String role;

    /** 权限标识集合，前端 v-permission 与菜单渲染使用；后端校验不依赖它 */
    private List<String> permissions;

    private String avatar;

    /** 密码已超过策略有效期，前端需强制弹出修改密码窗口 */
    private Boolean mustChangePassword;
}
