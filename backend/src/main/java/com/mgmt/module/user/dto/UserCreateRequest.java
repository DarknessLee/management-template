package com.mgmt.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "请输入用户名")
    private String username;

    @NotBlank(message = "请输入姓名")
    private String realName;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, message = "密码长度不能少于 6 位")
    private String password;

    @Email(message = "请输入正确的邮箱")
    private String email;

    private String phone;

    @NotBlank(message = "请选择角色")
    private String role;

    private Long deptId;

    private Integer status;
}
