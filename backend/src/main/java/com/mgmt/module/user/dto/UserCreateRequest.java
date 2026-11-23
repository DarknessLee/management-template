package com.mgmt.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "{validation.usernameRequired}")
    private String username;

    @NotBlank(message = "{validation.realNameRequired}")
    private String realName;

    @NotBlank(message = "{validation.passwordRequired}")
    @Size(min = 6, message = "{validation.passwordMinLength}")
    private String password;

    @Email(message = "{validation.emailInvalid}")
    private String email;

    private String phone;

    @NotBlank(message = "{validation.roleRequired}")
    private String role;

    private Long deptId;

    private Integer status;
}
