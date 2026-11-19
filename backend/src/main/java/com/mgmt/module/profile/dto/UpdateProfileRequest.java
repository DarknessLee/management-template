package com.mgmt.module.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料可修改字段的白名单。
 * 刻意不包含 username / roleId / status / password：
 * 这些字段一旦允许自改，就等于给了提权和改密的旁路。
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过 50 个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
