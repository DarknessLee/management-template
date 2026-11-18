package com.mgmt.module.user.dto;

import lombok.Data;

/**
 * 更新接口允许"部分字段"提交：
 * 状态开关只传 status，编辑弹窗传整表单，密码为空表示不修改。
 */
@Data
public class UserUpdateRequest {

    private String username;

    private String realName;

    private String password;

    private String email;

    private String phone;

    private String role;

    private Integer status;
}
