package com.mgmt.module.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "{validation.oldPasswordRequired}")
    private String oldPassword;

    @NotBlank(message = "{validation.newPasswordRequired}")
    @Size(min = 6, max = 32, message = "{validation.newPasswordLength}")
    private String newPassword;
}
