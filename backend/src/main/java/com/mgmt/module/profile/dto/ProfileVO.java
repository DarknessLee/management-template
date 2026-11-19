package com.mgmt.module.profile.dto;

import lombok.Data;

@Data
public class ProfileVO {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private String avatar;
}
