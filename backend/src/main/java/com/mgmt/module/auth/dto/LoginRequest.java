package com.mgmt.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{validation.usernameRequired}")
    private String username;

    @NotBlank(message = "{validation.passwordRequired}")
    private String password;

    @NotBlank(message = "{validation.captchaKeyRequired}")
    private String captchaKey;

    @NotBlank(message = "{validation.captchaCodeRequired}")
    private String captchaCode;
}
