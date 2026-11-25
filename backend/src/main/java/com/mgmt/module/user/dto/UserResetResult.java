package com.mgmt.module.user.dto;

import lombok.Data;

@Data
public class UserResetResult {

    private final String username;
    private final String realName;
    /** 临时密码，仅本次返回，需要管理员转告用户 */
    private final String password;
}
