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

    /** 记住我：为 true 时会话有效期取 remember-days，否则沿用登录超时设置 */
    private Boolean rememberMe;
}
