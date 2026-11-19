package com.mgmt.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码标识，登录时原样带回 */
    private String key;

    /** data:image/png;base64,... */
    private String image;
}
